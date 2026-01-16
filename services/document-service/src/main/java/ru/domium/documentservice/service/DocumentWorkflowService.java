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

  public List<DocumentInstance> listProjectDocuments(UUID projectId, DocumentStatus status,
      UUID stage, DocumentGroupType groupType) {
    return docRepo.findForProject(projectId, status, stage, groupType);
  }

  public List<DocumentInstance> listProjectDocumentsForUser(UUID projectId, UUID userId,
      DocumentStatus status, UUID stage, DocumentGroupType groupType) {
    return docRepo.findForProjectAndUser(projectId, userId, status, stage, groupType);
  }

  public List<DocumentInstance> listGroupDocuments(UUID groupId) {
    return docRepo.findAllByGroup_Id(groupId);
  }

  public DocumentInstance getDocument(UUID documentId) {
    return docRepo.findByIdWithRelations(documentId)
        .orElseThrow(() -> ApiExceptions.notFound("Document not found"));
  }
  @Transactional
  public InputStream loadDocumentFile(UUID documentId, boolean markViewed, ActorType actorType,
      UUID actorId) {
    DocumentInstance doc = docRepo.findByIdForUpdate(documentId)
        .orElseThrow(() -> ApiExceptions.notFound("Document not found"));
    boolean isOwnerClient =
        actorType == ActorType.CLIENT
            && actorId != null
            && actorId.equals(doc.getUserId());

    if (markViewed && isOwnerClient && doc.getStatus() == DocumentStatus.SENT_TO_USER) {
      doc.setStatus(DocumentStatus.VIEWED);
      doc.setViewedAt(Instant.now());
      docRepo.save(doc);
      audit(doc, actorType, actorId, AuditAction.VIEWED, json());
    }
    return storage.load(BUCKET_DOCUMENTS, doc.getCurrentFileStorageId());
  }

  @Transactional
  public FileStorageService.FileObject loadDocumentFileWithMetadata(UUID documentId, boolean markViewed,
      ActorType actorType, UUID actorId) {
    DocumentInstance doc = docRepo.findByIdForUpdate(documentId)
        .orElseThrow(() -> ApiExceptions.notFound("Document not found"));
    boolean isOwnerClient =
        actorType == ActorType.CLIENT
            && actorId != null
            && actorId.equals(doc.getUserId());

    if (markViewed && isOwnerClient && doc.getStatus() == DocumentStatus.SENT_TO_USER) {
      doc.setStatus(DocumentStatus.VIEWED);
      doc.setViewedAt(Instant.now());
      docRepo.save(doc);
      audit(doc, actorType, actorId, AuditAction.VIEWED, json());
    }
    return storage.loadWithMetadata(BUCKET_DOCUMENTS, doc.getCurrentFileStorageId());
  }

  @Transactional
  public DocumentInstance softDelete(UUID documentId, UUID providerId, String comment){
    DocumentInstance doc = docRepo.findByIdForUpdate(documentId)
        .orElseThrow(() -> ApiExceptions.notFound("Document not found"));
    if (doc.getStatus() == DocumentStatus.DELETE) {
      return doc;
    }
    doc.setStatus(DocumentStatus.DELETE);
    docRepo.save(doc);
    if (comment != null && !comment.isBlank()) {
      DocumentComment c = new DocumentComment();
      c.setDocument(doc);
      c.setAuthorType(CommentAuthorType.MANAGER);
      c.setAuthorId(providerId);
      c.setText(comment);
      commentRepo.save(c);
      audit(doc, ActorType.MANAGER, providerId, AuditAction.COMMENT_ADDED, json("text", comment));
    }

    audit(doc, ActorType.MANAGER, providerId, AuditAction.DELETED, json());
    return doc;
  }

  @Transactional
  public DocumentSignature sign(UUID documentId, UUID signerUserId, ActorType signerType,
      SignatureType signatureType, String confirmationCode, String ip, String userAgent) {
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

    if (signatureRepo.existsByDocument_IdAndSignerType(documentId, signerType)) {
      throw ApiExceptions.badRequest("Document already signed by " + signerType);
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
    signature.setSignerType(signerType);
    signature.setType(signatureType);
    signature.setFileHash(hash);
    signature.setSignaturePayloadJson(json(
        "ip", ip,
        "userAgent", userAgent,
        "confirmationCode", mask(confirmationCode)
    ));
    signature = signatureRepo.save(signature);

    boolean hasClient = signatureRepo.existsByDocument_IdAndSignerType(documentId, ActorType.CLIENT);
    boolean hasManager = signatureRepo.existsByDocument_IdAndSignerType(documentId, ActorType.MANAGER);
    if (hasClient && hasManager) {
      doc.setStatus(DocumentStatus.SIGNED);
      doc.setSignedAt(Instant.now());
      docRepo.save(doc);
      documentsSigned.increment();
    }

    audit(doc, signerType, signerUserId, AuditAction.SIGNED,
        json("signatureType", signatureType.name(), "fileHash", hash));
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
    fv.setCreatedByType(ActorType.MANAGER);
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
      c.setAuthorType(CommentAuthorType.MANAGER);
      c.setAuthorId(providerId);
      c.setText(comment);
      commentRepo.save(c);
      audit(doc, ActorType.MANAGER, providerId, AuditAction.COMMENT_ADDED, json("text", comment));
    }

    audit(doc, ActorType.MANAGER, providerId, AuditAction.VERSION_UPDATED,
        json("version", nextVersion));
    audit(doc, ActorType.MANAGER, providerId, AuditAction.SENT,
        json("reason", "new-version-uploaded"));
    return doc;
  }

  @Transactional
  public DocumentInstance manualUpload(UUID projectId, UUID providerId, UUID stage,
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
    doc.setTitle(title != null && !title.isBlank() ? title : file.getOriginalFilename());
    doc.setStatus(DocumentStatus.SENT_TO_USER);
    doc.setSentAt(Instant.now());
    doc.setVersion(1);
    doc.setCurrentFileStorageId(fileId);
    doc = docRepo.save(doc);

    DocumentFileVersion fv = new DocumentFileVersion();
    fv.setDocument(doc);
    fv.setVersion(1);
    fv.setFileStorageId(fileId);
    fv.setCreatedByType(ActorType.MANAGER);
    fv.setCreatedById(providerId);
    versionRepo.save(fv);

    audit(doc, ActorType.MANAGER, providerId, AuditAction.MANUAL_UPLOADED, json("title", title));
    audit(doc, ActorType.MANAGER, providerId, AuditAction.SENT, json("reason", "manual-upload"));
    return doc;
  }

  @Transactional
  public DocumentInstance uploadStageDocument(UUID projectId, UUID uploaderId, UUID recipientId,
      UUID stage, MultipartFile file, String title, DocumentGroupType groupType, ActorType uploaderType) {
    if (file == null || file.isEmpty()) {
      throw ApiExceptions.badRequest("file is required");
    }
    if (projectId == null || stage == null) {
      throw ApiExceptions.badRequest("projectId and stage are required");
    }
    if (recipientId == null) {
      throw ApiExceptions.badRequest("recipientId is required");
    }

    DocumentGroup group = resolveGroup(projectId, groupType);

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
              storage.delete(BUCKET_DOCUMENTS, finalFileId);
            }
          }
        }
    );

    DocumentInstance doc = new DocumentInstance();
    doc.setTemplate(null);
    doc.setGroup(group);
    doc.setProjectId(projectId);
    doc.setUserId(recipientId);
    doc.setStageCode(stage);
    doc.setTitle(title != null && !title.isBlank() ? title : file.getOriginalFilename());
    doc.setStatus(DocumentStatus.SENT_TO_USER);
    doc.setSentAt(Instant.now());
    doc.setVersion(1);
    doc.setCurrentFileStorageId(fileId);
    doc = docRepo.save(doc);

    DocumentFileVersion fv = new DocumentFileVersion();
    fv.setDocument(doc);
    fv.setVersion(1);
    fv.setFileStorageId(fileId);
    fv.setCreatedByType(uploaderType);
    fv.setCreatedById(uploaderId);
    versionRepo.save(fv);

    audit(doc, uploaderType, uploaderId, AuditAction.MANUAL_UPLOADED, json("title", title));
    audit(doc, uploaderType, uploaderId, AuditAction.SENT, json("reason", "stage-upload"));
    return doc;
  }

  public String generatePresignedUrl(UUID documentId, int ttlSeconds) {
    DocumentInstance doc = docRepo.findById(documentId)
        .orElseThrow(() -> ApiExceptions.notFound("Document not found"));
    return storage.generatePresignedUrl(BUCKET_DOCUMENTS, doc.getCurrentFileStorageId(), ttlSeconds)
        .toString();
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

  private DocumentGroup resolveGroup(UUID projectId, DocumentGroupType groupType) {
    if (groupType == null) {
      return null;
    }
    return groupRepo.findByProjectIdAndType(projectId, groupType)
        .orElseGet(() -> {
          DocumentGroup group = new DocumentGroup();
          group.setProjectId(projectId);
          group.setType(groupType);
          group.setTitle(groupType.name());
          return groupRepo.save(group);
        });
  }

  private String escape(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
  }
}
