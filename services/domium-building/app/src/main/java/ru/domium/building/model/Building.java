package ru.domium.building.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "buildings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Building {
    @Id
    @GeneratedValue
    private UUID id;

    @Version
    private long version;

    private UUID projectId;
    private UUID clientId;
    private UUID managerId;
    private UUID workflowId;

    private UUID currentStageId;
    private String currentStageName;
    private int currentStageIndex;
    private int progress = 0;
    private String status = "ACTIVE";

    private LocalDateTime createdAt = LocalDateTime.now();

    public void nextStage(UUID userId, StageTransition transition) {
        if (transition == null || !transition.canTransition()) {
            throw new IllegalStateException(
                    "Cannot transition from " + currentStageName);
        }

        currentStageId = transition.nextStageId();
        currentStageName = transition.nextStageName();
        currentStageIndex = transition.nextStageIndex();

        int totalStages = transition.totalStages();
        if (totalStages <= 1) {
            progress = 100;
            status = "COMPLETED";
            return;
        }
        progress = (int) Math.round((currentStageIndex * 100.0) / (totalStages - 1));
        if (currentStageIndex >= totalStages - 1) status = "COMPLETED";
    }

    public static Building from(UUID projectId, UUID clientId, UUID managerId, UUID workflowId) {
        Building building = new Building();
        building.projectId = projectId;
        building.clientId = clientId;
        building.managerId = managerId;
        building.workflowId = workflowId;
        return building;
    }
}
