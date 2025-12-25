package ru.domium.building.service.stage;

import org.springframework.stereotype.Service;
import ru.domium.building.model.StageTransition;
import ru.domium.building.model.Stage;
import ru.domium.building.repository.StageRepository;

import java.util.List;
import java.util.UUID;

@Service
public class WorkflowStageTransitionResolver implements StageTransitionResolver {
    private final StageRepository stageRepo;

    public WorkflowStageTransitionResolver(StageRepository stageRepo) {
        this.stageRepo = stageRepo;
    }

    @Override
    public StageTransition resolveNext(UUID workflowId, UUID currentStageId) {
        List<Stage> stages = stageRepo.findByWorkflowIdOrderByPosition(workflowId);
        int totalStages = stages.size();

        int currentIndex = -1;
        for (int i = 0; i < stages.size(); i++) {
            if (stages.get(i).getId().equals(currentStageId)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1 || currentIndex >= stages.size() - 1) {
            return new StageTransition(null, null, -1, totalStages, false);
        }

        Stage nextStage = stages.get(currentIndex + 1);
        return new StageTransition(nextStage.getId(), nextStage.getName(), currentIndex + 1, totalStages, true);
    }
}
