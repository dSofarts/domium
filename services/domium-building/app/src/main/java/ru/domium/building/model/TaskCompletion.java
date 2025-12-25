package ru.domium.building.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "building_task_completions")
@Data
public class TaskCompletion {
    @EmbeddedId
    private TaskCompletionId id;

    private UUID completedBy;
    private Instant completedAt;
}
