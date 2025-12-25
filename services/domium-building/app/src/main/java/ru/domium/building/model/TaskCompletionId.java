package ru.domium.building.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@Embeddable
public class TaskCompletionId implements Serializable {
    private UUID buildingId;
    private UUID taskId;
}
