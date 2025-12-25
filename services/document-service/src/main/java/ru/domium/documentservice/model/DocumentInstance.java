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
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "document_instance",
    indexes = {
        @Index(name = "idx_doc_project", columnList = "project_id"),
        @Index(name = "idx_doc_user", columnList = "user_id"),
        @Index(name = "idx_doc_group", columnList = "group_id")
    })
public class DocumentInstance extends BaseEntity {

  @Id
  @GeneratedValue
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id")
  private DocumentTemplate template;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id")
  private DocumentGroup group;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "stage_code", nullable = false)
  private StageCode stageCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private DocumentStatus status;

  @Column(name = "version", nullable = false)
  private int version;

  @Column(name = "current_file_storage_id", nullable = false)
  private String currentFileStorageId;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "viewed_at")
  private Instant viewedAt;

  @Column(name = "signed_at")
  private Instant signedAt;

  @Column(name = "rejected_at")
  private Instant rejectedAt;

  @Column(name = "due_date")
  private Instant dueDate;


}
