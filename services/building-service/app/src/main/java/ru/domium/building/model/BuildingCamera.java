package ru.domium.building.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "building_cameras")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildingCamera {
    @Id
    @GeneratedValue
    private UUID id;

    private UUID buildingId;
    private String name;
    private String rtspUrl;
    private boolean enabled = true;
    private boolean transcode = false;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
}


