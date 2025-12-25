package ru.domium.building.service.stage;

import ru.domium.building.model.StageTransition;

import java.util.UUID;

public interface StageTransitionResolver {
    StageTransition resolveNext(UUID workflowId, UUID currentStageId);
}
