package ru.domium.building.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.domium.building.model.BuildingProjection;
import ru.domium.building.model.Task;
import ru.domium.building.model.TaskCompletion;
import ru.domium.building.model.TaskCompletionId;
import ru.domium.building.repository.TaskCompletionRepository;
import ru.domium.building.repository.TaskRepository;
import ru.domium.building.service.BuildingService;
import ru.domium.building.service.stage.requirement.StageTransitionNotAllowedException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

class StageNextRequiresTasksIT extends AbstractPostgresIT {

    private static final UUID CLIENT_ID = UUID.fromString("c1d2e3f4-a5b6-7890-cdef-123456789012");
    private static final UUID MANAGER_ID = UUID.fromString("d2e3f4a5-b6c7-8901-def2-234567890123");
    private static final UUID PROJECT_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private static final UUID DEFAULT_WORKFLOW_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PREPARATION_STAGE_ID = UUID.fromString("22222222-2222-2222-2222-222222222221");

    @Autowired BuildingService buildingService;
    @Autowired TaskRepository taskRepository;
    @Autowired TaskCompletionRepository taskCompletionRepository;

    @Test
    @Transactional
    void nextStage_returns409_whenNotAllWorkItemsCompleted() {
        BuildingProjection created = buildingService.createBuilding(
                PROJECT_ID,
                "Проект",
                CLIENT_ID,
                MANAGER_ID,
                "Менеджер",
                java.util.Map.of(),
                DEFAULT_WORKFLOW_ID
        );

        assertThatThrownBy(() -> buildingService.nextStage(created.getId(), CLIENT_ID, null))
                .isInstanceOf(StageTransitionNotAllowedException.class)
                .satisfies(ex -> {
                    StageTransitionNotAllowedException e = (StageTransitionNotAllowedException) ex;
                    assertThat(e.getViolations()).isNotEmpty();
                    assertThat(e.getViolations().getFirst().code()).isEqualTo("SUBSTAGES_INCOMPLETE");
                });
    }

    @Test
    @Transactional
    void nextStage_succeeds_afterAllWorkItemsCompleted() {
        BuildingProjection created = buildingService.createBuilding(
                PROJECT_ID,
                "Проект",
                CLIENT_ID,
                MANAGER_ID,
                "Менеджер",
                java.util.Map.of(),
                DEFAULT_WORKFLOW_ID
        );

        List<Task> subStages = taskRepository.findByStageIdAndParentIdIsNullOrderByPosition(PREPARATION_STAGE_ID);
        Set<UUID> subStageIds = subStages.stream().map(Task::getId).collect(Collectors.toSet());
        List<Task> workItems = taskRepository.findByParentIdInOrderByParentIdAscPositionAsc(subStageIds).stream()
                .filter(t -> "WORK_ITEM".equalsIgnoreCase(t.getType()))
                .toList();
        assertThat(workItems).isNotEmpty();

        for (Task wi : workItems) {
            TaskCompletionId id = new TaskCompletionId();
            id.setBuildingId(created.getId());
            id.setTaskId(wi.getId());
            TaskCompletion c = new TaskCompletion();
            c.setId(id);
            c.setCompletedBy(CLIENT_ID);
            c.setCompletedAt(Instant.now());
            taskCompletionRepository.save(c);
        }

        BuildingProjection after = buildingService.nextStage(created.getId(), CLIENT_ID, null);
        assertThat(after.getStage()).isNotNull();
        assertThat(after.getProgress()).isGreaterThanOrEqualTo(0);
    }
}


