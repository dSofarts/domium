package ru.domium.building.model;

import java.util.UUID;

public record StageTransition(
        UUID nextStageId,
        String nextStageName,
        int nextStageIndex,
        int totalStages,
        boolean canTransition
) {
}
