package ru.domium.documentservice.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.time.Instant;
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
@Table(name = "document_signature",
    indexes = @Index(name = "idx_signature_doc", columnList = "document_id"))
public class DocumentSignature {

  @Id @GeneratedValue
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "document_id", nullable = false)
  private DocumentInstance document;

  @Column(name = "signer_user_id", nullable = false)
  private UUID signerUserId;

  @Enumerated(EnumType.STRING)
  @Column(name = "signer_type", nullable = false)
  private ActorType signerType;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private SignatureType type;

  @Column(name = "signed_at", nullable = false, updatable = false)
  private Instant signedAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "signature_payload", columnDefinition = "jsonb")
  private JsonNode signaturePayloadJson;

  @Column(name = "file_hash", nullable = false)
  private String fileHash;

  @PrePersist
  void onCreate() { this.signedAt = Instant.now(); }
}
