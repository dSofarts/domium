package ru.domium.projectservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "project_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    private Project project;

    @Column(name = "client_user_id", nullable = false)
    private UUID clientUserId;

    @Column(name = "status", length = 50)
    @Enumerated(EnumType.STRING)
    private ProjectOrderStatus status;

    @Column(name = "created_at")
    @CreatedDate
    private Instant createdAt;
}
