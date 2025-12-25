package ru.domium.building.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "building_workflows")
@Data
public class Workflow {
    @Id
    private UUID id;

    private UUID managerId;
    private String name;
    private boolean active = true;
    private Instant createdAt = Instant.now();
}
