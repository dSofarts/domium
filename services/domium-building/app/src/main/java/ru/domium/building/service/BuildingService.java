package ru.domium.building.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.domium.building.mapper.BuildingMapper;
import ru.domium.building.model.Building;
import ru.domium.building.model.Workflow;
import ru.domium.building.model.Stage;
import ru.domium.building.mapper.WorkflowMapper;
import ru.domium.building.service.stage.StageTransitionResolver;
import ru.domium.building.api.dto.BuildingDto;
import ru.domium.building.api.dto.workflow.StageDto;
import ru.domium.building.api.dto.substage.SubStageStatusDto;
import ru.domium.building.service.stage.requirement.StageTransitionRequirements;
import ru.domium.building.service.stage.requirement.StageTransitionNotAllowedException;
import ru.domium.building.service.stage.requirement.TransitionViolation;
import ru.domium.building.repository.BuildingProjectionRepository;
import ru.domium.building.repository.BuildingRepository;
import ru.domium.building.repository.WorkflowRepository;
import ru.domium.building.repository.StageRepository;
import ru.domium.building.repository.TaskRepository;
import ru.domium.building.repository.TaskCompletionRepository;
import ru.domium.building.model.BuildingProjection;
import ru.domium.building.model.Task;
import ru.domium.building.model.TaskCompletion;
import ru.domium.building.model.TaskCompletionId;
import ru.domium.building.mapper.SubStageStatusMapper;
import ru.domium.building.api.dto.substage.WorkItemStatusDto;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuildingService {
    private static final UUID DEFAULT_WORKFLOW_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SYSTEM_MANAGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final BuildingRepository buildingRepository;
    private final BuildingProjectionRepository buildingProjectionRepository;
    private final StageTransitionResolver stageTransitionResolver;
    private final StageTransitionRequirements stageTransitionRequirements;
    private final StageRepository stageRepository;
    private final WorkflowRepository workflowRepository;
    private final BuildingMapper buildingMapper;
    private final WorkflowMapper workflowMapper;
    private final TaskRepository taskRepository;
    private final TaskCompletionRepository taskCompletionRepository;
    private final SubStageStatusMapper subStageStatusMapper;

    @Transactional
    public BuildingProjection nextStage(UUID buildingId, UUID userId) {
        return nextStage(buildingId, userId, null);
    }

    @Transactional
    public BuildingProjection nextStage(UUID buildingId, UUID userId, String managerNameOverride) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Building not found"));

        requireAccess(building, userId);

        var transition = stageTransitionResolver.resolveNext(building.getWorkflowId(), building.getCurrentStageId());
        if (!transition.canTransition()) {
            String message;
            String code;
            if (building.getCurrentStageId() == null) {
                code = "CURRENT_STAGE_NOT_SET";
                message = "Текущая стадия не задана для стройки";
            } else if (building.getCurrentStageIndex() >= 0 && transition.totalStages() > 0
                    && building.getCurrentStageIndex() >= transition.totalStages() - 1) {
                code = "NO_NEXT_STAGE";
                message = "Стройка уже на последнем этапе";
            } else {
                code = "STAGE_NOT_FOUND_IN_WORKFLOW";
                message = "Текущая стадия не найдена в workflow (данные неконсистентны)";
            }
            throw new StageTransitionNotAllowedException(List.of(new TransitionViolation(code, message)));
        }

        stageTransitionRequirements.check(building, userId, transition);
        building.nextStage(userId, transition);

        buildingRepository.save(building);
        BuildingProjection projection = upsertProjectionFromBuilding(building, null, managerNameOverride, null);

        log.info("Building {} advanced to stage: {}", buildingId, building.getCurrentStageName());
        return projection;
    }

    @Transactional(readOnly = true)
    public BuildingDto getBuilding(UUID buildingId, UUID userId) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Building not found: " + buildingId));
        requireAccess(building, userId);
        return buildingMapper.toDto(building);
    }

    @Transactional(readOnly = true)
    public List<StageDto> getStages(UUID buildingId, UUID userId) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Building not found"));
        requireAccess(building, userId);
        if (building.getWorkflowId() == null) return List.of();
        List<Stage> stages = stageRepository.findByWorkflowIdOrderByPosition(building.getWorkflowId());
        List<StageDto> dtos = workflowMapper.toStageDtos(stages);

        for (StageDto dto : dtos) {
            if (dto.getId() == null) continue;
            var subStageDtos = workflowMapper.toSubStageDtos(taskRepository.findByStageIdAndParentIdIsNullOrderByPosition(dto.getId()));
            for (var ss : subStageDtos) {
                if (ss.getId() == null) continue;
                ss.setWorkItems(workflowMapper.toWorkItemDtos(taskRepository.findByParentIdOrderByPosition(ss.getId())));
            }
            dto.setSubStages(subStageDtos);
        }
        return dtos;
    }

    @Transactional(readOnly = true)
    public List<SubStageStatusDto> getCurrentSubStages(UUID buildingId, UUID userId) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Building not found"));
        requireAccess(building, userId);
        if (building.getCurrentStageId() == null) return List.of();

        return getStageSubStagesInternal(buildingId, building.getCurrentStageId());
    }

    @Transactional(readOnly = true)
    public List<SubStageStatusDto> getStageSubStages(UUID buildingId, UUID stageId, UUID userId) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Building not found"));
        requireAccess(building, userId);
        if (stageId == null) return List.of();

        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Stage not found: " + stageId));
        if (building.getWorkflowId() == null || !building.getWorkflowId().equals(stage.getWorkflowId())) {
            throw new ResponseStatusException(NOT_FOUND, "Stage does not belong to building workflow");
        }

        return getStageSubStagesInternal(buildingId, stageId);
    }

    private List<SubStageStatusDto> getStageSubStagesInternal(UUID buildingId, UUID stageId) {
        List<Task> subStages = taskRepository.findByStageIdAndParentIdIsNullOrderByPosition(stageId);
        if (subStages.isEmpty()) return List.of();

        Set<UUID> subStageIds = subStages.stream().map(Task::getId).collect(java.util.stream.Collectors.toSet());
        List<Task> workItems = taskRepository.findByParentIdInOrderByParentIdAscPositionAsc(subStageIds);
        Set<UUID> workItemIds = workItems.stream().map(Task::getId).collect(java.util.stream.Collectors.toSet());
        Set<UUID> completedWorkItemIds = workItemIds.isEmpty()
                ? Set.of()
                : taskCompletionRepository.findCompletedTaskIds(buildingId, workItemIds);

        var workItemsBySubStage = workItems.stream().collect(java.util.stream.Collectors.groupingBy(Task::getParentId));

        return subStages.stream().map(s -> {
            SubStageStatusDto dto = subStageStatusMapper.toDto(s);
            List<Task> items = workItemsBySubStage.getOrDefault(s.getId(), List.of());
            List<WorkItemStatusDto> itemDtos = items.stream().map(wi -> {
                WorkItemStatusDto wid = new WorkItemStatusDto();
                wid.setId(wi.getId());
                wid.setName(wi.getName());
                wid.setDescription(wi.getDescription());
                wid.setPosition(wi.getPosition());
                wid.setCompleted(completedWorkItemIds.contains(wi.getId()));
                return wid;
            }).toList();
            dto.setWorkItems(itemDtos);
            boolean completed = !items.isEmpty() && itemDtos.stream().allMatch(WorkItemStatusDto::isCompleted);
            dto.setCompleted(completed);
            return dto;
        }).toList();
    }

    @Transactional
    public void completeWorkItem(UUID buildingId, UUID userId, UUID workItemId) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Building not found"));
        requireAccess(building, userId);

        Task workItem = taskRepository.findById(workItemId)
                .orElseThrow(() -> new IllegalArgumentException("Task (вид работ) not found"));
        if (!"WORK_ITEM".equalsIgnoreCase(workItem.getType())) {
            throw new IllegalArgumentException("Task is not a WORK_ITEM (вид работ)");
        }
        if (workItem.getParentId() == null) {
            throw new IllegalArgumentException("WORK_ITEM has no parent SUBSTAGE (подэтап)");
        }
        Task subStage = taskRepository.findById(workItem.getParentId())
                .orElseThrow(() -> new IllegalArgumentException("Parent SUBSTAGE (подэтап) not found"));

        if (building.getCurrentStageId() == null || !building.getCurrentStageId().equals(subStage.getStageId())) {
            throw new IllegalArgumentException("Work item (вид работ) does not belong to current stage");
        }

        TaskCompletionId id = new TaskCompletionId();
        id.setBuildingId(buildingId);
        id.setTaskId(workItemId);
        if (taskCompletionRepository.existsById(id)) return; // idempotent

        TaskCompletion completion = new TaskCompletion();
        completion.setId(id);
        completion.setCompletedBy(userId);
        completion.setCompletedAt(Instant.now());
        taskCompletionRepository.save(completion);
    }

    @Transactional
    public BuildingProjection createBuilding(UUID projectId,
                                             String projectName,
                                             UUID clientId,
                                             UUID managerId,
                                             String managerName,
                                             Map<String, Object> attributes,
                                             UUID workflowId) {
        if (projectId == null) throw new IllegalArgumentException("projectId is required");
        if (clientId == null) throw new IllegalArgumentException("clientId is required");
        if (managerId == null) throw new IllegalArgumentException("managerId is required");

        UUID resolvedWorkflowId = resolveWorkflowForManager(managerId, workflowId);

        Building building = Building.from(projectId, clientId, managerId, resolvedWorkflowId);

        List<Stage> stages = stageRepository.findByWorkflowIdOrderByPosition(resolvedWorkflowId);
        if (stages.isEmpty()) throw new IllegalStateException("Workflow has no stages: " + resolvedWorkflowId);

        int totalStages = stages.size();
        building.setCurrentStageName(stages.getFirst().getName());
        building.setCurrentStageId(stages.getFirst().getId());
        building.setCurrentStageIndex(0);
        building.setProgress(totalStages <= 1 ? 100 : 0);
        building.setStatus(totalStages <= 1 ? "COMPLETED" : "ACTIVE");

        buildingRepository.save(building);
        BuildingProjection projection = upsertProjectionFromBuilding(building, projectName, managerName, attributes);

        log.info("Building created: {} for project {}", building.getId(), projectId);
        return projection;
    }

    private UUID resolveWorkflowForManager(UUID managerId, UUID requestedWorkflowId) {
        if (requestedWorkflowId != null) {
            Workflow wf = workflowRepository.findById(requestedWorkflowId)
                    .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + requestedWorkflowId));
            if (!managerId.equals(wf.getManagerId()) && !SYSTEM_MANAGER_ID.equals(wf.getManagerId())) {
                throw new AccessDeniedException("Forbidden");
            }
            return wf.getId();
        }

        return workflowRepository.findByManagerIdAndActiveTrue(managerId)
                .map(Workflow::getId)
                .orElse(DEFAULT_WORKFLOW_ID);
    }

    private BuildingProjection upsertProjectionFromBuilding(Building building,
                                                           String projectName,
                                                           String managerName,
                                                           Map<String, Object> attributes) {
        BuildingProjection projection = buildingProjectionRepository.findById(building.getId())
                .orElseGet(BuildingProjection::new);
        projection.setId(building.getId());
        projection.setProjectId(building.getProjectId());
        if (projectName != null && (projection.getProjectName() == null || projection.getProjectName().isBlank())) {
            projection.setProjectName(projectName);
        }
        projection.setClientId(building.getClientId());
        projection.setManagerId(building.getManagerId());
        if (managerName != null && (projection.getManagerName() == null || projection.getManagerName().isBlank())) {
            projection.setManagerName(managerName);
        }
        projection.setStage(building.getCurrentStageName());
        projection.setProgress(building.getProgress());
        projection.setUpdatedAt(LocalDateTime.now());
        if (attributes != null && (projection.getMetadata() == null || projection.getMetadata().isEmpty())) {
            projection.setMetadata(attributes);
        }
        buildingProjectionRepository.save(projection);
        return projection;
    }

    private void requireAccess(Building building, UUID userId) {
        if (userId == null) throw new AccessDeniedException("Unauthorized");
        if (userId.equals(building.getClientId())) return;
        if (userId.equals(building.getManagerId())) return;
        throw new AccessDeniedException("Forbidden");
    }

}
