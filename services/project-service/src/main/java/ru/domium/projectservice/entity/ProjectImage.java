package ru.domium.projectservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "project_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "project")
@Builder
public class ProjectImage {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    private Project project;

    @Column(name = "storage_object_key", nullable = false)
    private String storageObjectKey;

    @Column(name = "position", nullable = false)
    private Integer position;
}
