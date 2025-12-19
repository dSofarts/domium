package ru.domium.projectservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "project_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProjectType type;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "material", nullable = false, length = 50)
    private String material;

    @Column(name = "location", nullable = false, length = 100)
    private String location;

    @Column(name = "description")
    private String description;

//    @Column(name = "likes", nullable = false)
//    private Integer likes;

    @Column(name = "publication_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProjectPublicationStatus publicationStatus;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Floor> floors = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProjectImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProjectOrder> orders = new ArrayList<>();
}
