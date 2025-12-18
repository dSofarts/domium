package ru.domium.documentservice.model;

import jakarta.persistence.*;
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
@Table(name = "document_group",
    indexes = {
        @Index(name = "idx_group_project_type", columnList = "project_id, type")
    })
public class DocumentGroup extends BaseEntity {
  @Id @GeneratedValue
  private UUID id;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private DocumentGroupType type;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "root_document_id")
  private UUID rootDocumentId;

}
