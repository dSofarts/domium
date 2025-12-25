package ru.domium.building.service.stage.requirement;

import org.springframework.stereotype.Component;
import ru.domium.building.model.Building;
import ru.domium.building.model.StageTransition;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Прогоняет все требования.
 * Если требований нет — переход разрешён по умолчанию.
 */
@Component
public class StageTransitionRequirements {
    private final List<StageTransitionRequirement> requirements;

    public StageTransitionRequirements(List<StageTransitionRequirement> requirements) {
        this.requirements = requirements;
    }

    public void check(Building building, UUID userId, StageTransition transition) {
        List<TransitionViolation> violations = new ArrayList<>();
        for (StageTransitionRequirement req : requirements) {
            List<TransitionViolation> v = req.validate(building, userId, transition);
            if (v != null && !v.isEmpty()) violations.addAll(v);
        }
        if (!violations.isEmpty()) throw new StageTransitionNotAllowedException(violations);
    }
}
