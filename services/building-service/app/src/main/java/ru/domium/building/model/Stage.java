package ru.domium.building.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "workflow_stages")
@Data
public class Stage {
    @Id
    private UUID id;

    private UUID workflowId;
    private String name;
    private String description;
    private int plannedDays;
    private int position;
}


