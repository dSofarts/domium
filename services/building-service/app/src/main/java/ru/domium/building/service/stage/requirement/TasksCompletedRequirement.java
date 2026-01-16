package ru.domium.building.service.stage.requirement;

import org.springframework.stereotype.Component;
import ru.domium.building.model.Building;
import ru.domium.building.model.StageTransition;
import ru.domium.building.model.Task;
import ru.domium.building.repository.TaskCompletionRepository;
import ru.domium.building.repository.TaskRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Перед переходом на следующий этап все подэтапы текущей стадии должны быть завершены.
 * Подэтап считается завершённым, если завершены все его дочерние виды работ (ITEM).
 * Порядок выполнения не важен.
 */
@Component
public class TasksCompletedRequirement implements StageTransitionRequirement {
    private final TaskRepository taskRepository;
    private final TaskCompletionRepository completionRepository;

    public TasksCompletedRequirement(TaskRepository taskRepository,
                                     TaskCompletionRepository completionRepository) {
        this.taskRepository = taskRepository;
        this.completionRepository = completionRepository;
    }

    @Override
    public List<TransitionViolation> validate(Building building, UUID userId, StageTransition transition) {
        if (transition == null || !transition.canTransition()) return List.of();
        if (building.getCurrentStageId() == null) return List.of();

        List<Task> subStages = taskRepository.findByStageIdAndParentIdIsNullOrderByPosition(building.getCurrentStageId());
        if (subStages.isEmpty()) return List.of();

        Set<UUID> subStageIds = subStages.stream().map(Task::getId).collect(Collectors.toSet());
        List<Task> workItems = taskRepository.findByParentIdInOrderByParentIdAscPositionAsc(subStageIds);
        if (workItems.isEmpty()) return List.of();

        Set<UUID> workItemIds = workItems.stream().map(Task::getId).collect(Collectors.toSet());
        Set<UUID> completed = completionRepository.findCompletedTaskIds(building.getId(), workItemIds);
        if (completed.size() == workItemIds.size()) return List.of();

        var workItemsBySubStage = workItems.stream().collect(Collectors.groupingBy(Task::getParentId));
        String missing = subStages.stream()
                .filter(ss -> {
                    List<Task> items = workItemsBySubStage.getOrDefault(ss.getId(), List.of());
                    return !items.isEmpty() && items.stream().anyMatch(wi -> !completed.contains(wi.getId()));
                })
                .map(Task::getName)
                .collect(Collectors.joining(", "));

        return List.of(new TransitionViolation(
                "SUBSTAGES_INCOMPLETE",
                "Нельзя перейти на следующий этап: не завершены подэтапы текущей стадии: " + missing
        ));
    }
}
