package ru.domium.documentservice.service;

import ru.domium.documentservice.dto.DocumentDtos.*;
import ru.domium.documentservice.model.*;
import java.util.List;

public final class DocumentMapper {
  private DocumentMapper() {}

  public static DocumentInstanceDto toDto(DocumentInstance d) {
    GroupInfo group = null;
    if (d.getGroup() != null) {
      DocumentGroup g = d.getGroup();
      group = new GroupInfo(g.getId(), g.getType(), g.getTitle(), g.getRootDocumentId());
    }

    TemplateInfo template = null;
    if (d.getTemplate() != null) {
      DocumentTemplate t = d.getTemplate();
      template = new TemplateInfo(t.getId(), t.getCode(), t.getName(), t.isRequired(), t.getStageCode());
    }

    return new DocumentInstanceDto(
        d.getId(),
        d.getProjectId(),
        d.getUserId(),
        d.getStageCode(),
        d.getTitle(),
        d.getStatus(),
        d.getVersion(),
        d.getCreatedAt(),
        d.getSentAt(),
        d.getViewedAt(),
        d.getSignedAt(),
        d.getRejectedAt(),
        d.getDueDate(),
        group,
        template
    );
  }

  public static FileVersionDto toDto(DocumentFileVersion v) {
    return new FileVersionDto(v.getId(), v.getVersion(), v.getFileStorageId(), v.getCreatedAt(), v.getCreatedByType(), v.getCreatedById());
  }

  public static CommentDto toDto(DocumentComment c) {
    return new CommentDto(c.getId(), c.getDocument().getId(), c.getAuthorType(), c.getAuthorId(), c.getText(), c.getCreatedAt());
  }

  public static SignatureDto toDto(DocumentSignature s) {
    return new SignatureDto(
        s.getId(),
        s.getDocument().getId(),
        s.getSignerUserId(),
        s.getSignerType(),
        s.getType(),
        s.getSignedAt(),
        s.getSignaturePayloadJson(),
        s.getFileHash()
    );
  }

  public static AuditDto toDto(DocumentAuditLog a) {
    return new AuditDto(a.getId(), a.getDocument().getId(), a.getActorType(), a.getActorId(), a.getAction(), a.getPayloadJson(), a.getCreatedAt());
  }

  public static DocumentDetailsDto toDetails(DocumentInstance doc, List<DocumentFileVersion> versions, List<DocumentComment> comments,
                                             List<DocumentSignature> signatures, List<DocumentAuditLog> audits) {
    return new DocumentDetailsDto(
        toDto(doc),
        versions.stream().map(DocumentMapper::toDto).toList(),
        comments.stream().map(DocumentMapper::toDto).toList(),
        signatures.stream().map(DocumentMapper::toDto).toList(),
        audits.stream().map(DocumentMapper::toDto).toList()
    );
  }
}
