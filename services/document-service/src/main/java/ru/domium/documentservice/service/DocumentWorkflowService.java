package ru.domium.documentservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.domium.documentservice.exception.ApiExceptions;
import ru.domium.documentservice.model.*;
import ru.domium.documentservice.repository.*;
import io.micrometer.core.instrument.Counter;
import jakarta.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class DocumentWorkflowService {

  public static final String BUCKET_TEMPLATES = "templates";
  public static final String BUCKET_DOCUMENTS = "documents";

  private final DocumentTemplateRepository templateRepo;
  private final DocumentGroupRepository groupRepo;
  private final DocumentInstanceRepository docRepo;
  private final DocumentFileVersionRepository versionRepo;
  private final DocumentCommentRepository commentRepo;
  private final DocumentSignatureRepository signatureRepo;
  private final DocumentAuditLogRepository auditRepo;
  private final FileStorageService storage;
  private final PdfGenerator pdfGenerator;
  private final Counter documentsGenerated;
  private final Counter documentsSigned;
  private final Counter documentsRejected;
  private final ObjectMapper objectMapper;

  public DocumentWorkflowService(
      DocumentTemplateRepository templateRepo,
      DocumentGroupRepository groupRepo,
      DocumentInstanceRepository docRepo,
      DocumentFileVersionRepository versionRepo,
      DocumentCommentRepository commentRepo,
      DocumentSignatureRepository signatureRepo,
      DocumentAuditLogRepository auditRepo,
      FileStorageService storage,
      PdfGenerator pdfGenerator,
      Counter documentsGeneratedTotal,
      Counter documentsSignedTotal,
      Counter documentsRejectedTotal,
      ObjectMapper objectMapper) {
    this.templateRepo = templateRepo;
    this.groupRepo = groupRepo;
    this.docRepo = docRepo;
    this.versionRepo = versionRepo;
    this.commentRepo = commentRepo;
    this.signatureRepo = signatureRepo;
    this.auditRepo = auditRepo;
    this.storage = storage;
    this.pdfGenerator = pdfGenerator;
    this.documentsGenerated = documentsGeneratedTotal;
    this.documentsSigned = documentsSignedTotal;
    this.documentsRejected = documentsRejectedTotal;
    this.objectMapper = objectMapper;
  }


  @Transactional
  public List<DocumentInstance> generateForStage(UUID projectId, StageCode stage, UUID userId,
      String projectType, Map<String, Object> data) {
    List<DocumentTemplate> templates = templateRepo.findForStageAndProjectType(stage, projectType);
    if (templates.isEmpty()) {
      return List.of();
    }

    DocumentGroup mainContractGroup = null;
    if (stage == StageCode.INIT_DOCS) {
      mainContractGroup = groupRepo.findByProjectIdAndType(projectId,
              DocumentGroupType.MAIN_CONTRACT)
          .orElseGet(() -> {
            DocumentGroup g = new DocumentGroup();
            g.setProjectId(projectId);
            g.setType(DocumentGroupType.MAIN_CONTRACT);
            g.setTitle("Основной договор");
            return groupRepo.save(g);
          });
    }

    List<DocumentInstance> created = new ArrayList<>();
    for (DocumentTemplate t : templates) {
      DocumentInstance doc = new DocumentInstance();
      doc.setTemplate(t);
      doc.setProjectId(projectId);
      doc.setUserId(userId);
      doc.setStageCode(stage);
      doc.setStatus(DocumentStatus.CREATED);
      doc.setVersion(1);

      // If INIT_DOCS - attach to MAIN_CONTRACT by default (key requirement)
      if (mainContractGroup != null) {
        doc.setGroup(mainContractGroup);
      }

      // generate file
      Map<String, Object> merged = new LinkedHashMap<>();
      if (data != null) {
        merged.putAll(data);
      }
      merged.putIfAbsent("projectId", projectId);
      if (userId != null) {
        merged.putIfAbsent("userId", userId);
      }
      merged.putIfAbsent("stage", stage.name());

      PdfGenerator.GeneratedPdf pdf = pdfGenerator.generate(t, merged);
      String fileId = storage.save(BUCKET_DOCUMENTS, new ByteArrayInputStream(pdf.bytes()),
          pdf.bytes().length, pdf.contentType(), pdf.filename());
      doc.setCurrentFileStorageId(fileId);

      doc = docRepo.save(doc);

      DocumentFileVersion fv = new DocumentFileVersion();
      fv.setDocument(doc);
      fv.setVersion(1);
      fv.setFileStorageId(fileId);
      fv.setCreatedByType(ActorType.SYSTEM);
      versionRepo.save(fv);

      doc.setStatus(DocumentStatus.SENT_TO_USER);
      doc.setSentAt(Instant.now());
      docRepo.save(doc);

      if (mainContractGroup != null && mainContractGroup.getRootDocumentId() == null
          && "CONTRACT_MAIN".equalsIgnoreCase(t.getCode())) {
        mainContractGroup.setRootDocumentId(doc.getId());
        groupRepo.save(mainContractGroup);
      }

      audit(doc, ActorType.SYSTEM, null, AuditAction.GENERATED,
          json("templateCode", t.getCode(), "stage", stage.name()));
      audit(doc, ActorType.SYSTEM, null, AuditAction.SENT,
          json("reason", "auto-send-after-generation"));
      documentsGenerated.increment();
      created.add(doc);
    }
    return created;
  }

  public List<DocumentInstance> listProjectDocuments(UUID projectId, DocumentStatus status,
      StageCode stage, DocumentGroupType groupType) {
    return docRepo.findForProject(projectId, status, stage, groupType);
  }

  public List<DocumentInstance> listProjectDocumentsForUser(UUID projectId, UUID userId,
      DocumentStatus status, StageCode stage, DocumentGroupType groupType) {
    return docRepo.findForProjectAndUser(projectId, userId, status, stage, groupType);
  }

  public List<DocumentInstance> listGroupDocuments(UUID groupId) {
    return docRepo.findAllByGroup_Id(groupId);
  }

  public DocumentInstance getDocument(UUID documentId) {
    return docRepo.findById(documentId)
        .orElseThrow(() -> ApiExceptions.notFound("Document not found"));
  }
  @Transactional
  public InputStream loadDocumentFile(UUID documentId, boolean markViewed, ActorType actorType,
      UUID actorId) {
    DocumentInstance doc = docRepo.findByIdForUpdate(documentId)
        .orElseThrow(() -> ApiExceptions.notFound("Document not found"));
    if (markViewed && doc.getStatus() == DocumentStatus.SENT_TO_USER) {
      doc.setStatus(DocumentStatus.VIEWED);
      doc.setViewedAt(Instant.now());
      docRepo.save(doc);
      audit(doc, actorType, actorId, AuditAction.VIEWED, json());
    }
    return storage.load(BUCKET_DOCUMENTS, doc.getCurrentFileStorageId());
  }

  @Transactional
  public DocumentSignature sign(UUID documentId, UUID signerUserId, SignatureType signatureType,
      String confirmationCode, String ip, String userAgent) {
    if (signatureType != SignatureType.SIMPLE) {
      throw ApiExceptions.badRequest("Only SIMPLE signature is supported in MVP");
    }

    if (confirmationCode == null || confirmationCode.length() < 4) {
      throw ApiExceptions.badRequest("Invalid confirmationCode");
    }

    DocumentInstance doc = docRepo.findByIdForUpdate(documentId)
        .orElseThrow(() -> ApiExceptions.notFound("Document not found"));
    if (!(doc.getStatus() == DocumentStatus.SENT_TO_USER
        || doc.getStatus() == DocumentStatus.VIEWED)) {
      throw ApiExceptions.badRequest("Document cannot be signed in status: " + doc.getStatus());
    }

    // hash current file
    byte[] bytes;
    try (InputStream is = storage.load(BUCKET_DOCUMENTS, doc.getCurrentFileStorageId())) {
      bytes = is.readAllBytes();
    } catch (Exception e) {
      throw ApiExceptions.badRequest("Unable to read file for hashing");
    }
    String hash = sha256Hex(bytes);

    DocumentSignature signature = new DocumentSignature();
    signature.setDocument(doc);
    signature.setSignerUserId(signerUserId);
    signature.setType(signatureType);
    signature.setFileHash(hash);
    signature.setSignaturePayloadJson(json(
        "ip", ip,
        "userAgent", userAgent,
        "confirmationCode", mask(confirmationCode)
    ));
    signature = signatureRepo.save(signature);

    doc.setStatus(DocumentStatus.SIGNED);
    doc.setSignedAt(Instant.now());
    docRepo.save(doc);

    audit(doc, ActorType.CLIENT, signerUserId, AuditAction.SIGNED,
        json("signatureType", signatureType.name(), "fileHash", hash));
    documentsSigned.increment();
    return signature;
  }

  @Transactional
  public void reject(UUID documentId, UUID userId, String comment) {
    DocumentInstance doc = docRepo.findByIdForUpdate(documentId)
        .orElseThrow(() -> ApiExceptions.notFound("Document not found"));
    if (!(doc.getStatus() == DocumentStatus.SENT_TO_USER
        || doc.getStatus() == DocumentStatus.VIEWED)) {
      throw ApiExceptions.badRequest("Document cannot be rejected in status: " + doc.getStatus());
    }
    doc.setStatus(DocumentStatus.REJECTED);
    doc.setRejectedAt(Instant.now());
    docRepo.save(doc);

    if (comment != null && !comment.isBlank()) {
      DocumentComment c = new DocumentComment();
      c.setDocument(doc);
      c.setAuthorType(CommentAuthorType.USER);
      c.setAuthorId(userId);
      c.setText(comment);
      commentRepo.save(c);
      audit(doc, ActorType.CLIENT, userId, AuditAction.COMMENT_ADDED, json("text", comment));
    }
    audit(doc, ActorType.CLIENT, userId, AuditAction.REJECTED, json());
    documentsRejected.increment();
  }

  @Transactional
  public DocumentInstance uploadNewVersion(UUID documentId, UUID providerId, MultipartFile file,
      String comment) {
    if (file == null || file.isEmpty()) {
      throw ApiExceptions.badRequest("file is required");
    }

    DocumentInstance doc = docRepo.findByIdForUpdate(documentId)
        .orElseThrow(() -> ApiExceptions.notFound("Document not found"));
    int nextVersion = doc.getVersion() + 1;

    String fileId;
    try {
      fileId = storage.save(BUCKET_DOCUMENTS, file.getInputStream(), file.getSize(),
          file.getContentType(), file.getOriginalFilename());
    } catch (Exception e) {
      throw ApiExceptions.badRequest("Failed to upload file: " + e.getMessage());
    }

    DocumentFileVersion fv = new DocumentFileVersion();
    fv.setDocument(doc);
    fv.setVersion(nextVersion);
    fv.setFileStorageId(fileId);
    fv.setCreatedByType(ActorType.BUILDER);
    fv.setCreatedById(providerId);
    versionRepo.save(fv);

    doc.setVersion(nextVersion);
    doc.setCurrentFileStorageId(fileId);
    doc.setStatus(DocumentStatus.SENT_TO_USER);
    doc.setSentAt(Instant.now());
    docRepo.save(doc);

    if (comment != null && !comment.isBlank()) {
      DocumentComment c = new DocumentComment();
      c.setDocument(doc);
      c.setAuthorType(CommentAuthorType.PROVIDER);
      c.setAuthorId(providerId);
      c.setText(comment);
      commentRepo.save(c);
      audit(doc, ActorType.BUILDER, providerId, AuditAction.COMMENT_ADDED, json("text", comment));
    }

    audit(doc, ActorType.BUILDER, providerId, AuditAction.VERSION_UPDATED,
        json("version", nextVersion));
    audit(doc, ActorType.BUILDER, providerId, AuditAction.SENT,
        json("reason", "new-version-uploaded"));
    return doc;
  }

  @Transactional
  public DocumentInstance manualUpload(UUID projectId, UUID providerId, StageCode stage,
      UUID groupId, UUID userId, MultipartFile file, String title) {
    if (file == null || file.isEmpty()) {
      throw ApiExceptions.badRequest("file is required");
    }

    DocumentGroup group = null;
    if (groupId != null) {
      group = groupRepo.findById(groupId)
          .orElseThrow(() -> ApiExceptions.notFound("DocumentGroup not found"));
      if (!group.getProjectId().equals(projectId)) {
        throw ApiExceptions.forbidden("groupId does not belong to project");
      }
    }

    String fileId;
    try {
      fileId = storage.save(BUCKET_DOCUMENTS, file.getInputStream(), file.getSize(),
          file.getContentType(), file.getOriginalFilename());
    } catch (IOException e) {
      throw ApiExceptions.badRequest("Failed to read uploaded file");
    }

    String finalFileId = fileId;
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status == STATUS_ROLLED_BACK) {
              log.info("Rolled_back");
              storage.delete(BUCKET_DOCUMENTS, finalFileId);
            }
          }
        }
    );

    DocumentInstance doc = new DocumentInstance();
    doc.setTemplate(null);
    doc.setGroup(group);
    doc.setProjectId(projectId);
    doc.setUserId(userId);
    doc.setStageCode(stage);
    doc.setStatus(DocumentStatus.SENT_TO_USER);
    doc.setSentAt(Instant.now());
    doc.setVersion(1);
    doc.setCurrentFileStorageId(fileId);
    doc = docRepo.save(doc);

    DocumentFileVersion fv = new DocumentFileVersion();
    fv.setDocument(doc);
    fv.setVersion(1);
    fv.setFileStorageId(fileId);
    fv.setCreatedByType(ActorType.BUILDER);
    fv.setCreatedById(providerId);
    versionRepo.save(fv);

    audit(doc, ActorType.BUILDER, providerId, AuditAction.MANUAL_UPLOADED, json("title", title));
    audit(doc, ActorType.BUILDER, providerId, AuditAction.SENT, json("reason", "manual-upload"));
    return doc;
  }

  @Transactional
  public void advanceStage(UUID projectId, StageCode nextStage, UUID providerId) {
    StageCode currentStage = switch (nextStage) {
      case CONSTRUCTION -> StageCode.INIT_DOCS;
      case FINAL_DOCS -> StageCode.CONSTRUCTION;
      default -> null;
    };
    if (currentStage != null) {
      long unsigned = docRepo.countUnsignedRequiredForStage(projectId, currentStage);
      if (unsigned > 0) {
        throw ApiExceptions.badRequest(
            "Cannot advance stage. Unsigned required documents: " + unsigned);
      }
    }
    generateForStage(projectId, nextStage, null, null, Map.of("initiatedBy", "advanceStage"));
  }

  public List<DocumentFileVersion> listVersions(UUID documentId) {
    return versionRepo.findAllByDocument_IdOrderByVersionDesc(documentId);
  }

  public List<DocumentComment> listComments(UUID documentId) {
    return commentRepo.findAllByDocument_IdOrderByCreatedAtAsc(documentId);
  }

  public List<DocumentAuditLog> listAudits(UUID documentId) {
    return auditRepo.findAllByDocument_IdOrderByCreatedAtAsc(documentId);
  }

  public List<DocumentSignature> listSignatures(UUID documentId) {
    return signatureRepo.findAllByDocument_IdOrderBySignedAtAsc(documentId);
  }

  private void audit(DocumentInstance doc, ActorType actorType, UUID actorId, AuditAction action,
      JsonNode payloadJson) {
    DocumentAuditLog a = new DocumentAuditLog();
    a.setDocument(doc);
    a.setActorType(actorType);
    a.setActorId(actorId);
    a.setAction(action);
    a.setPayloadJson(payloadJson);
    auditRepo.save(a);
  }

  private String sha256Hex(byte[] bytes) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(bytes);
      StringBuilder sb = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private String mask(String code) {
    if (code == null) {
      return null;
    }
    if (code.length() <= 2) {
      return "**";
    }
    return "*".repeat(code.length() - 2) + code.substring(code.length() - 2);
  }

  private JsonNode json(Object... kv) {
    ObjectNode node = objectMapper.createObjectNode();

    if (kv == null || kv.length == 0) {
      return node;
    }
    if (kv.length % 2 != 0) {
      throw new IllegalArgumentException("kv must be even");
    }

    for (int i = 0; i < kv.length; i += 2) {
      String key = String.valueOf(kv[i]);
      Object value = kv[i + 1];

      if (value == null) {
        node.putNull(key);
      } else if (value instanceof Boolean b) {
        node.put(key, b);
      } else if (value instanceof Integer n) {
        node.put(key, n);
      } else if (value instanceof Long n) {
        node.put(key, n);
      } else if (value instanceof Double n) {
        node.put(key, n);
      } else if (value instanceof JsonNode jn) {
        node.set(key, jn);
      } else {
        node.put(key, value.toString());
      }
    }

    return node;
  }

  private String escape(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
  }
}
