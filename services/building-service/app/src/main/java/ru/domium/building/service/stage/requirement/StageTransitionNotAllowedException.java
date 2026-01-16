package ru.domium.building.service.stage.requirement;

import java.util.List;

public class StageTransitionNotAllowedException extends RuntimeException {
    private final List<TransitionViolation> violations;

    public StageTransitionNotAllowedException(List<TransitionViolation> violations) {
        super("Stage transition is not allowed: " + violations);
        this.violations = violations;
    }

    public List<TransitionViolation> getViolations() {
        return violations;
    }
}
