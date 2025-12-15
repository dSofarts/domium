package ru.domium.projectservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "project_images",
       uniqueConstraints = {
               @UniqueConstraint(name = "uq_project_images_project_url", columnNames = {"project_id", "image_url"})
       },
       indexes = {
               @Index(name = "idx_project_images_project_id", columnList = "project_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectImage {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(optional = false)
    private Project project;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "is_main", nullable = false)
    private Boolean isMain;
}
