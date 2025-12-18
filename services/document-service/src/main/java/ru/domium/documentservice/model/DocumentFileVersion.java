package ru.domium.documentservice.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "document_file_version",
    uniqueConstraints = @UniqueConstraint(name = "uq_doc_version", columnNames = {"document_id", "version"}),
    indexes = @Index(name = "idx_doc_version_doc", columnList = "document_id"))
public class DocumentFileVersion {

  @Id @GeneratedValue
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "document_id", nullable = false)
  private DocumentInstance document;

  @Column(name = "version", nullable = false)
  private int version;

  @Column(name = "file_storage_id", nullable = false)
  private String fileStorageId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "created_by_type", nullable = false)
  private ActorType createdByType;

  @Column(name = "created_by_id")
  private UUID createdById;

  @PrePersist
  void onCreate() {
    this.createdAt = Instant.now();
  }
}
