package ru.domium.documentservice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import ru.domium.documentservice.model.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DocumentDtos {
  private DocumentDtos() {}

  public record GroupInfo(UUID id, DocumentGroupType type, String title, UUID rootDocumentId) {}

  public record TemplateInfo(UUID id, String code, String name, boolean required, UUID stageCode) {}

  public record DocumentInstanceDto(
      UUID id,
      UUID projectId,
      UUID userId,
      UUID stageCode,
      DocumentStatus status,
      int version,
      Instant createdAt,
      Instant sentAt,
      Instant viewedAt,
      Instant signedAt,
      Instant rejectedAt,
      Instant dueDate,
      GroupInfo group,
      TemplateInfo template
  ) {}

  public record FileVersionDto(UUID id, int version, String fileStorageId, Instant createdAt, ActorType createdByType, UUID createdById) {}

  public record CommentDto(UUID id, UUID documentId, CommentAuthorType authorType, UUID authorId, String text, Instant createdAt) {}

  public record SignatureDto(UUID id, UUID documentId, UUID signerUserId, SignatureType type, Instant signedAt, JsonNode signaturePayloadJson, String fileHash) {}

  public record AuditDto(UUID id, UUID documentId, ActorType actorType, UUID actorId, AuditAction action, JsonNode payloadJson, Instant createdAt) {}

  public record DocumentDetailsDto(DocumentInstanceDto document, List<FileVersionDto> versions, List<CommentDto> comments, List<SignatureDto> signatures, List<AuditDto> audit) {}

  public record GenerateRequest(UUID userId, String projectType, java.util.Map<String, Object> data) {}

  public record SignRequest(SignatureType signatureType, String confirmationCode) {}

  public record RejectRequest(String comment) {}

  public record AdvanceStageRequest(UUID nextStage) {}
}
