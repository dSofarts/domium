package ru.domium.building.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "workflow_tasks")
@Data
public class Task {
    @Id
    private UUID id;

    private UUID stageId;
    private UUID parentId;
    private String type;
    private String name;
    private String description;
    private int position;
}
