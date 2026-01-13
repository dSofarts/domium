package ru.domium.documentservice.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "document_template",
    indexes = {
        @Index(name = "idx_template_stage_project_type", columnList = "stage_code, project_type"),
        @Index(name = "idx_template_code", columnList = "code", unique = true)
    })
public class DocumentTemplate extends BaseEntity {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "code", nullable = false, unique = true)
  private String code;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "stage_code", nullable = false)
  private UUID stageCode;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "tags", columnDefinition = "jsonb")
  private JsonNode tagsJson;

  @Column(name = "is_required", nullable = false)
  private boolean required;

  @Column(name = "project_type")
  private String projectType;

  @Column(name = "file_storage_id", nullable = false)
  private String fileStorageId;

  @Enumerated(EnumType.STRING)
  @Column(name = "template_engine_type", nullable = false)
  private TemplateEngineType templateEngineType;
}
