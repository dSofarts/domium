package ru.domium.building.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import ru.domium.building.mapper.WorkflowMapper;
import ru.domium.building.mapper.StageUpdateMapper;
import ru.domium.building.model.Workflow;
import ru.domium.building.model.Stage;
import ru.domium.building.model.Task;
import ru.domium.building.api.dto.workflow.CreateWorkflowRequest;
import ru.domium.building.api.dto.workflow.UpsertStagesRequest;
import ru.domium.building.api.dto.workflow.UpsertStageTasksRequest;
import ru.domium.building.api.dto.workflow.WorkflowDto;
import ru.domium.building.api.dto.workflow.StageDto;
import ru.domium.building.api.dto.workflow.SubStageDto;
import ru.domium.building.api.dto.workitem.WorkItemDto;
import ru.domium.building.repository.BuildingRepository;
import ru.domium.building.repository.WorkflowRepository;
import ru.domium.building.repository.StageRepository;
import ru.domium.building.repository.TaskRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.CONFLICT;

@Service
@RequiredArgsConstructor
public class WorkflowService {
    private static final UUID DEFAULT_WORKFLOW_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SYSTEM_MANAGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final WorkflowRepository workflowRepository;
    private final StageRepository stageRepository;
    private final TaskRepository taskRepository;
    private final BuildingRepository buildingRepository;
    private final WorkflowMapper workflowMapper;
    private final StageUpdateMapper stageUpdateMapper;

    @Transactional(readOnly = true)
    public WorkflowDto getActive(UUID managerId) {
        return workflowRepository.findByManagerIdAndActiveTrue(managerId)
                .map(this::toDtoWithStages)
                .orElseGet(() -> {
                    Workflow wf = workflowRepository.findById(DEFAULT_WORKFLOW_ID)
                            .orElseThrow(() -> new NoSuchElementException("Default workflow not found"));
                    return toDtoWithStages(wf);
                });
    }

    @Transactional(readOnly = true)
    public WorkflowDto getDefault() {
        Workflow wf = workflowRepository.findById(DEFAULT_WORKFLOW_ID)
                .orElseThrow(() -> new NoSuchElementException("Default workflow not found"));
        return toDtoWithStages(wf);
    }

    @Transactional(readOnly = true)
    public WorkflowDto get(UUID workflowId, UUID managerId) {
        Workflow wf = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new NoSuchElementException("Workflow not found"));
        if (!wf.getManagerId().equals(managerId) && !SYSTEM_MANAGER_ID.equals(wf.getManagerId())) {
            throw new AccessDeniedException("Forbidden");
        }
        return toDtoWithStages(wf);
    }

    @Transactional
    public WorkflowDto create(UUID managerId, CreateWorkflowRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }

        Workflow wf = new Workflow();
        wf.setId(UUID.randomUUID());
        wf.setManagerId(managerId);
        wf.setName(req.getName().trim());
        wf.setActive(req.isActive());

        if (wf.isActive()) {
            workflowRepository.deactivateActive(managerId);
        }
        workflowRepository.save(wf);

        if (req.getStages() != null && !req.getStages().isEmpty()) {
            upsertStagesInternal(wf.getId(), req.getStages());
        }

        return toDtoWithStages(wf);
    }

    @Transactional
    public WorkflowDto activate(UUID workflowId, UUID managerId) {
        Workflow wf = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new NoSuchElementException("Workflow not found"));
        assertModifiable(wf, managerId);

        workflowRepository.deactivateActive(managerId);
        wf.setActive(true);
        workflowRepository.save(wf);
        return toDtoWithStages(wf);
    }

    @Transactional
    public WorkflowDto upsertStages(UUID workflowId, UUID managerId, UpsertStagesRequest request) {
        Workflow wf = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new NoSuchElementException("Workflow not found"));
        assertModifiable(wf, managerId);
        List<StageDto> stages = request.getStages() == null ? List.of() : request.getStages();
        upsertStagesInternal(workflowId, stages);
        return toDtoWithStages(wf);
    }

    @Transactional
    public WorkflowDto upsertStageTasks(UUID workflowId, UUID stageId, UUID managerId, UpsertStageTasksRequest request) {
        Workflow wf = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new NoSuchElementException("Workflow not found"));
        assertModifiable(wf, managerId);

        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new NoSuchElementException("Stage not found"));
        if (!workflowId.equals(stage.getWorkflowId())) throw new IllegalArgumentException("Stage does not belong to workflow");

        if (buildingRepository.existsByWorkflowIdAndCurrentStageIdIn(workflowId, Set.of(stageId))) {
            throw new ResponseStatusException(CONFLICT, "Cannot change tasks for stage that is used by active buildings");
        }

        List<SubStageDto> desired = request == null || request.getSubStages() == null ? List.of() : request.getSubStages();
        validateTaskTree(desired);

        List<Task> existingSubStages = taskRepository.findByStageIdAndParentIdIsNullOrderByPosition(stageId);
        Map<UUID, Task> existingById = new HashMap<>();
        for (Task t : existingSubStages) existingById.put(t.getId(), t);
        Set<UUID> existingSubStageIds = existingById.keySet();
        List<Task> existingWorkItems = existingSubStageIds.isEmpty() ? List.of()
                : taskRepository.findByParentIdInOrderByParentIdAscPositionAsc(existingSubStageIds);
        for (Task t : existingWorkItems) existingById.put(t.getId(), t);

        Set<UUID> desiredIds = new HashSet<>();
        for (SubStageDto ss : desired) {
            if (ss.getId() != null) desiredIds.add(ss.getId());
            if (ss.getWorkItems() != null) {
                for (WorkItemDto wi : ss.getWorkItems()) {
                    if (wi.getId() != null) desiredIds.add(wi.getId());
                }
            }
        }

        for (Task t : new ArrayList<>(existingById.values())) {
            if (t.getId() != null && !desiredIds.contains(t.getId())) {
                taskRepository.delete(t);
            }
        }

        for (SubStageDto ss : desired) {
            Task subStage;
            UUID subStageId = ss.getId() != null ? ss.getId() : UUID.randomUUID();
            if (ss.getId() != null) {
                subStage = existingById.get(ss.getId());
                if (subStage == null) throw new IllegalArgumentException("SubStage id not found in this stage: " + ss.getId());
            } else {
                subStage = new Task();
                subStage.setId(subStageId);
                subStage.setStageId(stageId);
                subStage.setParentId(null);
                subStage.setType("SUBSTAGE");
            }
            subStage.setName(ss.getName() != null ? ss.getName().trim() : null);
            subStage.setDescription(ss.getDescription());
            subStage.setPosition(ss.getPosition());
            taskRepository.save(subStage);

            List<WorkItemDto> items = ss.getWorkItems() == null ? List.of() : ss.getWorkItems();
            for (WorkItemDto wi : items) {
                Task workItem;
                UUID wid = wi.getId() != null ? wi.getId() : UUID.randomUUID();
                if (wi.getId() != null) {
                    workItem = existingById.get(wi.getId());
                    if (workItem == null) throw new IllegalArgumentException("WorkItem id not found in this stage: " + wi.getId());
                } else {
                    workItem = new Task();
                    workItem.setId(wid);
                    workItem.setStageId(stageId);
                    workItem.setParentId(subStageId);
                    workItem.setType("WORK_ITEM");
                }
                workItem.setName(wi.getName() != null ? wi.getName().trim() : null);
                workItem.setDescription(wi.getDescription());
                workItem.setPosition(wi.getPosition());
                taskRepository.save(workItem);
            }
        }

        return toDtoWithStages(wf);
    }

    /**
     * Запрещаем изменять системный (DEFAULT) workflow и workflow других менеджеров.
     * Для system workflow всегда 403, независимо от managerId.
     */
    private void assertModifiable(Workflow wf, UUID managerId) {
        if (wf == null) throw new NoSuchElementException("Workflow not found");
        if (DEFAULT_WORKFLOW_ID.equals(wf.getId()) || SYSTEM_MANAGER_ID.equals(wf.getManagerId())) {
            throw new AccessDeniedException("Forbidden");
        }
        if (!wf.getManagerId().equals(managerId)) {
            throw new AccessDeniedException("Forbidden");
        }
    }

    private void upsertStagesInternal(UUID workflowId, List<StageDto> desired) {
        validateStages(desired);

        List<Stage> existing = stageRepository.findByWorkflowIdOrderByPosition(workflowId);
        Map<UUID, Stage> existingById = existing.stream()
                .filter(s -> s.getId() != null)
                .collect(Collectors.toMap(Stage::getId, Function.identity()));

        Set<UUID> desiredIds = desired.stream()
                .map(StageDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Stage s : existing) {
            if (s.getId() == null) continue;
            if (!desiredIds.contains(s.getId())) {
                if (buildingRepository.existsByWorkflowIdAndCurrentStageIdIn(workflowId, Set.of(s.getId()))) {
                    throw new IllegalStateException("Cannot delete stage in use by active buildings. stageId=" + s.getId());
                }
                stageRepository.delete(s);
            }
        }

        for (StageDto dto : desired) {
            Stage stage;
            if (dto.getId() != null) {
                stage = existingById.get(dto.getId());
                if (stage == null) {
                    throw new IllegalArgumentException("Stage id not found in this workflow: " + dto.getId());
                }
            } else {
                stage = new Stage();
                stage.setId(UUID.randomUUID());
                stage.setWorkflowId(workflowId);
            }

            stageUpdateMapper.updateFromDto(dto, stage);
            stageRepository.save(stage);
        }
    }

    private void validateStages(List<StageDto> stages) {
        if (stages.isEmpty()) return;

        for (StageDto s : stages) {
            if (s.getName() == null || s.getName().isBlank()) {
                throw new IllegalArgumentException("Stage name is required");
            }

            if (s.getSubStages() != null && !s.getSubStages().isEmpty()) {
                throw new IllegalArgumentException("Stage.subStages must be empty in UpsertStagesRequest");
            }
        }

        Set<Integer> positions = stages.stream().map(StageDto::getPosition).collect(Collectors.toSet());
        if (positions.size() != stages.size()) {
            throw new IllegalArgumentException("Stage positions must be unique");
        }
        int n = stages.size();
        for (int i = 0; i < n; i++) {
            if (!positions.contains(i)) {
                throw new IllegalArgumentException("Stage positions must be contiguous starting from 0");
            }
        }

        Set<String> names = new HashSet<>();
        for (StageDto s : stages) {
            String key = s.getName().trim().toLowerCase(Locale.ROOT);
            if (!names.add(key)) throw new IllegalArgumentException("Stage names must be unique");
        }
    }

    private void validateTaskTree(List<SubStageDto> subStages) {
        if (subStages == null) return;
        if (!subStages.isEmpty()) {
            Set<Integer> positions = subStages.stream().map(SubStageDto::getPosition).collect(Collectors.toSet());
            if (positions.size() != subStages.size()) throw new IllegalArgumentException("SubStage positions must be unique");
            for (int i = 0; i < subStages.size(); i++) if (!positions.contains(i)) {
                throw new IllegalArgumentException("SubStage positions must be contiguous starting from 0");
            }
            Set<String> names = new HashSet<>();
            for (SubStageDto ss : subStages) {
                if (ss.getName() == null || ss.getName().isBlank()) throw new IllegalArgumentException("SubStage name is required");
                String key = ss.getName().trim().toLowerCase(Locale.ROOT);
                if (!names.add(key)) throw new IllegalArgumentException("SubStage names must be unique");
            }
        }

        for (SubStageDto ss : subStages) {
            List<WorkItemDto> items = ss.getWorkItems() == null ? List.of() : ss.getWorkItems();
            if (items.isEmpty()) continue;
            Set<Integer> positions = items.stream().map(WorkItemDto::getPosition).collect(Collectors.toSet());
            if (positions.size() != items.size()) throw new IllegalArgumentException("WorkItem positions must be unique within SubStage");
            for (int i = 0; i < items.size(); i++) if (!positions.contains(i)) {
                throw new IllegalArgumentException("WorkItem positions must be contiguous starting from 0 within SubStage");
            }
            Set<String> names = new HashSet<>();
            for (WorkItemDto wi : items) {
                if (wi.getName() == null || wi.getName().isBlank()) throw new IllegalArgumentException("WorkItem name is required");
                String key = wi.getName().trim().toLowerCase(Locale.ROOT);
                if (!names.add(key)) throw new IllegalArgumentException("WorkItem names must be unique within SubStage");
            }
        }
    }

    private WorkflowDto toDtoWithStages(Workflow wf) {
        WorkflowDto dto = workflowMapper.toDto(wf);
        List<StageDto> stageDtos = workflowMapper.toStageDtos(stageRepository.findByWorkflowIdOrderByPosition(wf.getId()));
        for (StageDto s : stageDtos) {
            if (s.getId() == null) continue;
            List<Task> subStages = taskRepository.findByStageIdAndParentIdIsNullOrderByPosition(s.getId());
            var subStageDtos = workflowMapper.toSubStageDtos(subStages);
            Set<UUID> subStageIds = subStages.stream().map(Task::getId).collect(Collectors.toSet());
            List<Task> workItems = subStageIds.isEmpty()
                    ? List.of()
                    : taskRepository.findByParentIdInOrderByParentIdAscPositionAsc(subStageIds);
            var workItemsByParent = workItems.stream().collect(Collectors.groupingBy(Task::getParentId));
            for (var ss : subStageDtos) {
                if (ss.getId() == null) continue;
                ss.setWorkItems(workflowMapper.toWorkItemDtos(workItemsByParent.getOrDefault(ss.getId(), List.of())));
            }
            s.setSubStages(subStageDtos);
        }
        dto.setStages(stageDtos);
        return dto;
    }
}
