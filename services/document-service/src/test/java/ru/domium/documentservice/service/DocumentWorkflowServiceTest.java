package ru.domium.documentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.domium.documentservice.model.*;
import ru.domium.documentservice.repository.*;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;

@ExtendWith(MockitoExtension.class)
class DocumentWorkflowServiceTest {

  @Mock private DocumentTemplateRepository templateRepo;
  @Mock private DocumentGroupRepository groupRepo;
  @Mock private DocumentInstanceRepository docRepo;
  @Mock private DocumentFileVersionRepository versionRepo;
  @Mock private DocumentCommentRepository commentRepo;
  @Mock private DocumentSignatureRepository signatureRepo;
  @Mock private DocumentAuditLogRepository auditRepo;
  @Mock private FileStorageService storage;
  @Mock private PdfGenerator pdfGenerator;

  @Mock private Counter documentsGeneratedTotal;
  @Mock private Counter documentsSignedTotal;
  @Mock private Counter documentsRejectedTotal;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private DocumentWorkflowService service;

  @BeforeEach
  void setUp() {
    service = new DocumentWorkflowService(
        templateRepo,
        groupRepo,
        docRepo,
        versionRepo,
        commentRepo,
        signatureRepo,
        auditRepo,
        storage,
        pdfGenerator,
        documentsGeneratedTotal,
        documentsSignedTotal,
        documentsRejectedTotal,
        objectMapper
    );
  }

  // -------------------- uploadNewVersion --------------------
  @Nested
  class UploadNewVersionTests {

    @Test
    void shouldThrowBadRequest_whenFileIsNull() {
      ResponseStatusException ex = assertThrows(
          ResponseStatusException.class,
          () -> service.uploadNewVersion(UUID.randomUUID(), UUID.randomUUID(), null, "c")
      );

      assertEquals(BAD_REQUEST, ex.getStatusCode());
      assertEquals("file is required", ex.getReason());

      verifyNoInteractions(docRepo, storage, versionRepo, commentRepo, auditRepo);
    }

    @Test
    void shouldThrowBadRequest_whenFileIsEmpty() {
      MultipartFile empty = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[0]);

      ResponseStatusException ex = assertThrows(
          ResponseStatusException.class,
          () -> service.uploadNewVersion(UUID.randomUUID(), UUID.randomUUID(), empty, "c")
      );

      assertEquals(BAD_REQUEST, ex.getStatusCode());
      assertEquals("file is required", ex.getReason());

      verifyNoInteractions(docRepo, storage, versionRepo, commentRepo, auditRepo);
    }

    @Test
    void shouldThrowNotFound_whenDocumentNotFound() {
      UUID docId = UUID.randomUUID();
      MultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "x".getBytes());

      when(docRepo.findByIdForUpdate(docId)).thenReturn(Optional.empty());

      ResponseStatusException ex = assertThrows(
          ResponseStatusException.class,
          () -> service.uploadNewVersion(docId, UUID.randomUUID(), file, null)
      );

      assertEquals(NOT_FOUND, ex.getStatusCode());
      assertEquals("Document not found", ex.getReason());

      verify(docRepo).findByIdForUpdate(docId);
      verifyNoInteractions(storage, versionRepo, commentRepo, auditRepo);
      verify(docRepo, never()).save(any());
    }

    @Test
    void shouldWrapStorageFailureAsBadRequest_withOriginalMessage() throws Exception {
      UUID docId = UUID.randomUUID();
      UUID providerId = UUID.randomUUID();

      DocumentInstance doc = new DocumentInstance();
      doc.setId(docId);
      doc.setVersion(7);
      doc.setStatus(DocumentStatus.VIEWED);
      doc.setCurrentFileStorageId("old");

      MultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "x".getBytes());

      when(docRepo.findByIdForUpdate(docId)).thenReturn(Optional.of(doc));
      when(storage.save(eq(DocumentWorkflowService.BUCKET_DOCUMENTS), any(), anyLong(), any(), any()))
          .thenThrow(new RuntimeException("boom"));

      ResponseStatusException ex = assertThrows(
          ResponseStatusException.class,
          () -> service.uploadNewVersion(docId, providerId, file, null)
      );

      assertEquals(BAD_REQUEST, ex.getStatusCode());
      assertNotNull(ex.getReason());
      assertTrue(ex.getReason().startsWith("Failed to upload file: "));
      assertTrue(ex.getReason().contains("boom"));

      verify(versionRepo, never()).save(any());
      verify(commentRepo, never()).save(any());
      verify(auditRepo, never()).save(any());
      verify(docRepo, never()).save(any());
    }

    @Test
    void shouldUpdateVersionAndDoc_andWriteAudits_whenNoComment() throws Exception {
      UUID docId = UUID.randomUUID();
      UUID providerId = UUID.randomUUID();

      DocumentInstance doc = new DocumentInstance();
      doc.setId(docId);
      doc.setVersion(1);
      doc.setStatus(DocumentStatus.VIEWED);
      doc.setCurrentFileStorageId("old-file");

      MultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "content".getBytes());

      when(docRepo.findByIdForUpdate(docId)).thenReturn(Optional.of(doc));
      when(storage.save(eq(DocumentWorkflowService.BUCKET_DOCUMENTS),
          any(), eq((long) file.getSize()), eq(file.getContentType()), eq(file.getOriginalFilename())))
          .thenReturn("new-file-id");

      ArgumentCaptor<DocumentFileVersion> fvCap = ArgumentCaptor.forClass(DocumentFileVersion.class);
      ArgumentCaptor<DocumentInstance> docSaveCap = ArgumentCaptor.forClass(DocumentInstance.class);
      ArgumentCaptor<DocumentAuditLog> auditCap = ArgumentCaptor.forClass(DocumentAuditLog.class);

      Instant before = Instant.now();
      DocumentInstance result = service.uploadNewVersion(docId, providerId, file, null);
      Instant after = Instant.now();

      // returned doc is the same mutated instance
      assertSame(doc, result);

      verify(versionRepo).save(fvCap.capture());
      DocumentFileVersion fv = fvCap.getValue();
      assertSame(doc, fv.getDocument());
      assertEquals(2, fv.getVersion());
      assertEquals("new-file-id", fv.getFileStorageId());
      assertEquals(ActorType.BUILDER, fv.getCreatedByType());
      assertEquals(providerId, fv.getCreatedById());

      verify(docRepo).save(docSaveCap.capture());
      DocumentInstance saved = docSaveCap.getValue();
      assertSame(doc, saved);
      assertEquals(2, saved.getVersion());
      assertEquals("new-file-id", saved.getCurrentFileStorageId());
      assertEquals(DocumentStatus.SENT_TO_USER, saved.getStatus());
      assertNotNull(saved.getSentAt());
      assertFalse(saved.getSentAt().isBefore(before));
      assertFalse(saved.getSentAt().isAfter(after));

      verify(commentRepo, never()).save(any());

      verify(auditRepo, times(2)).save(auditCap.capture());
      List<DocumentAuditLog> audits = auditCap.getAllValues();

      Set<AuditAction> actions = audits.stream().map(DocumentAuditLog::getAction).collect(Collectors.toSet());
      assertEquals(Set.of(AuditAction.VERSION_UPDATED, AuditAction.SENT), actions);

      DocumentAuditLog versionUpdated = audits.stream()
          .filter(a -> a.getAction() == AuditAction.VERSION_UPDATED).findFirst().orElseThrow();
      assertEquals(ActorType.BUILDER, versionUpdated.getActorType());
      assertEquals(providerId, versionUpdated.getActorId());
      assertEquals(2, versionUpdated.getPayloadJson().get("version").asInt());

      DocumentAuditLog sent = audits.stream()
          .filter(a -> a.getAction() == AuditAction.SENT).findFirst().orElseThrow();
      assertEquals("new-version-uploaded", sent.getPayloadJson().get("reason").asText());
    }

    @Test
    void shouldNotCreateComment_whenCommentIsBlank() throws Exception {
      UUID docId = UUID.randomUUID();
      UUID providerId = UUID.randomUUID();

      DocumentInstance doc = new DocumentInstance();
      doc.setId(docId);
      doc.setVersion(3);
      doc.setCurrentFileStorageId("old");

      MultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "x".getBytes());

      when(docRepo.findByIdForUpdate(docId)).thenReturn(Optional.of(doc));
      when(storage.save(anyString(), any(), anyLong(), any(), any())).thenReturn("fid");

      service.uploadNewVersion(docId, providerId, file, "   ");

      verify(commentRepo, never()).save(any());
      verify(auditRepo, times(2)).save(any()); // только VERSION_UPDATED + SENT
    }

    @Test
    void shouldCreateComment_andWriteCommentAudit_whenCommentProvided() throws Exception {
      UUID docId = UUID.randomUUID();
      UUID providerId = UUID.randomUUID();

      DocumentInstance doc = new DocumentInstance();
      doc.setId(docId);
      doc.setVersion(10);
      doc.setCurrentFileStorageId("old");

      MultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "x".getBytes());

      when(docRepo.findByIdForUpdate(docId)).thenReturn(Optional.of(doc));
      when(storage.save(anyString(), any(), anyLong(), any(), any())).thenReturn("fid");

      ArgumentCaptor<DocumentComment> commentCap = ArgumentCaptor.forClass(DocumentComment.class);
      ArgumentCaptor<DocumentAuditLog> auditCap = ArgumentCaptor.forClass(DocumentAuditLog.class);

      service.uploadNewVersion(docId, providerId, file, "hello");

      verify(commentRepo).save(commentCap.capture());
      DocumentComment c = commentCap.getValue();
      assertSame(doc, c.getDocument());
      assertEquals(CommentAuthorType.PROVIDER, c.getAuthorType());
      assertEquals(providerId, c.getAuthorId());
      assertEquals("hello", c.getText());

      verify(auditRepo, times(3)).save(auditCap.capture());
      List<DocumentAuditLog> audits = auditCap.getAllValues();
      Set<AuditAction> actions = audits.stream().map(DocumentAuditLog::getAction).collect(Collectors.toSet());
      assertEquals(Set.of(AuditAction.COMMENT_ADDED, AuditAction.VERSION_UPDATED, AuditAction.SENT), actions);

      DocumentAuditLog commentAdded = audits.stream()
          .filter(a -> a.getAction() == AuditAction.COMMENT_ADDED).findFirst().orElseThrow();
      assertEquals("hello", commentAdded.getPayloadJson().get("text").asText());
    }
  }

  // -------------------- manualUpload --------------------
  @Nested
  class ManualUploadTests {

    @Test
    void shouldThrowBadRequest_whenFileIsNull() {
      ResponseStatusException ex = assertThrows(
          ResponseStatusException.class,
          () -> service.manualUpload(UUID.randomUUID(), UUID.randomUUID(), StageCode.INIT_DOCS,
              null, UUID.randomUUID(), null, "t")
      );

      assertEquals(BAD_REQUEST, ex.getStatusCode());
      assertEquals("file is required", ex.getReason());
      verifyNoInteractions(groupRepo, storage, docRepo, versionRepo, auditRepo);
    }

    @Test
    void shouldThrowBadRequest_whenFileIsEmpty() {
      MultipartFile empty = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[0]);

      ResponseStatusException ex = assertThrows(
          ResponseStatusException.class,
          () -> service.manualUpload(UUID.randomUUID(), UUID.randomUUID(), StageCode.INIT_DOCS,
              null, UUID.randomUUID(), empty, "t")
      );

      assertEquals(BAD_REQUEST, ex.getStatusCode());
      assertEquals("file is required", ex.getReason());
      verifyNoInteractions(groupRepo, storage, docRepo, versionRepo, auditRepo);
    }

    @Test
    void shouldThrowNotFound_whenGroupNotFound() {
      UUID projectId = UUID.randomUUID();
      UUID groupId = UUID.randomUUID();
      MultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "x".getBytes());

      when(groupRepo.findById(groupId)).thenReturn(Optional.empty());

      ResponseStatusException ex = assertThrows(
          ResponseStatusException.class,
          () -> service.manualUpload(projectId, UUID.randomUUID(), StageCode.INIT_DOCS,
              groupId, UUID.randomUUID(), file, "t")
      );

      assertEquals(NOT_FOUND, ex.getStatusCode());
      assertEquals("DocumentGroup not found", ex.getReason());

      verify(groupRepo).findById(groupId);
      verifyNoInteractions(storage, docRepo, versionRepo, auditRepo);
    }

    @Test
    void shouldThrowForbidden_whenGroupDoesNotBelongToProject() {
      UUID projectId = UUID.randomUUID();
      UUID otherProjectId = UUID.randomUUID();
      UUID groupId = UUID.randomUUID();

      DocumentGroup group = new DocumentGroup();
      group.setId(groupId);
      group.setProjectId(otherProjectId);

      MultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "x".getBytes());

      when(groupRepo.findById(groupId)).thenReturn(Optional.of(group));

      ResponseStatusException ex = assertThrows(
          ResponseStatusException.class,
          () -> service.manualUpload(projectId, UUID.randomUUID(), StageCode.INIT_DOCS,
              groupId, UUID.randomUUID(), file, "t")
      );

      assertEquals(FORBIDDEN, ex.getStatusCode());
      assertEquals("groupId does not belong to project", ex.getReason());

      verifyNoInteractions(storage, docRepo, versionRepo, auditRepo);
    }

    @Test
    void shouldWrapIOExceptionAsBadRequest_whenUploadedFileCannotBeRead() throws Exception {
      UUID projectId = UUID.randomUUID();
      UUID providerId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();

      MultipartFile file = mock(MultipartFile.class);
      when(file.isEmpty()).thenReturn(false);
      when(file.getInputStream()).thenThrow(new IOException("read failed"));

      ResponseStatusException ex = assertThrows(
          ResponseStatusException.class,
          () -> service.manualUpload(projectId, providerId, StageCode.INIT_DOCS,
              null, userId, file, "title")
      );

      assertEquals(BAD_REQUEST, ex.getStatusCode());
      assertEquals("Failed to read uploaded file", ex.getReason());

      verify(storage, never()).save(any(), any(), anyLong(), any(), any());
      verifyNoInteractions(docRepo, versionRepo, auditRepo);
    }

    @Test
    void shouldCreateDocAndVersion_andWriteAudits_whenGroupIsNull_andNoRollbackDeletionOnCommit() throws Exception {
      UUID projectId = UUID.randomUUID();
      UUID providerId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();

      MultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "content".getBytes());

      when(storage.save(anyString(), any(), anyLong(), any(), any())).thenReturn("fid");

      when(docRepo.save(any(DocumentInstance.class))).thenAnswer(inv -> {
        DocumentInstance d = inv.getArgument(0);
        d.setId(UUID.randomUUID());
        return d;
      });

      ArgumentCaptor<DocumentInstance> docCap = ArgumentCaptor.forClass(DocumentInstance.class);
      ArgumentCaptor<DocumentFileVersion> fvCap = ArgumentCaptor.forClass(DocumentFileVersion.class);
      ArgumentCaptor<DocumentAuditLog> auditCap = ArgumentCaptor.forClass(DocumentAuditLog.class);

      TransactionSynchronizationManager.initSynchronization();
      try {
        Instant before = Instant.now();
        DocumentInstance created = service.manualUpload(
            projectId, providerId, StageCode.INIT_DOCS, null, userId, file, "My Title"
        );
        Instant after = Instant.now();

        verify(docRepo).save(docCap.capture());
        DocumentInstance saved = docCap.getValue();
        assertNull(saved.getTemplate());
        assertNull(saved.getGroup());
        assertEquals(projectId, saved.getProjectId());
        assertEquals(userId, saved.getUserId());
        assertEquals(StageCode.INIT_DOCS, saved.getStageCode());
        assertEquals(DocumentStatus.SENT_TO_USER, saved.getStatus());
        assertEquals(1, saved.getVersion());
        assertEquals("fid", saved.getCurrentFileStorageId());
        assertNotNull(saved.getSentAt());
        assertFalse(saved.getSentAt().isBefore(before));
        assertFalse(saved.getSentAt().isAfter(after));

        verify(versionRepo).save(fvCap.capture());
        DocumentFileVersion fv = fvCap.getValue();
        assertSame(created, fv.getDocument());
        assertEquals(1, fv.getVersion());
        assertEquals("fid", fv.getFileStorageId());
        assertEquals(ActorType.BUILDER, fv.getCreatedByType());
        assertEquals(providerId, fv.getCreatedById());

        verify(auditRepo, times(2)).save(auditCap.capture());
        List<DocumentAuditLog> audits = auditCap.getAllValues();
        Set<AuditAction> actions = audits.stream().map(DocumentAuditLog::getAction).collect(Collectors.toSet());
        assertEquals(Set.of(AuditAction.MANUAL_UPLOADED, AuditAction.SENT), actions);

        DocumentAuditLog manual = audits.stream()
            .filter(a -> a.getAction() == AuditAction.MANUAL_UPLOADED).findFirst().orElseThrow();
        assertEquals("My Title", manual.getPayloadJson().get("title").asText());

        // Проверка registered synchronization + на COMMIT delete не вызывается
        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, syncs.size());
        syncs.get(0).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

        verify(storage, never()).delete(anyString(), anyString());

      } finally {
        TransactionSynchronizationManager.clearSynchronization();
      }
    }

    @Test
    void shouldDeleteUploadedFileOnRollback_viaRegisteredSynchronization() throws Exception {
      UUID projectId = UUID.randomUUID();
      UUID providerId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      UUID groupId = UUID.randomUUID();

      DocumentGroup group = new DocumentGroup();
      group.setId(groupId);
      group.setProjectId(projectId);

      MultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "content".getBytes());

      when(groupRepo.findById(groupId)).thenReturn(Optional.of(group));
      when(storage.save(anyString(), any(), anyLong(), any(), any())).thenReturn("fid");

      when(docRepo.save(any(DocumentInstance.class))).thenAnswer(inv -> {
        DocumentInstance d = inv.getArgument(0);
        d.setId(UUID.randomUUID());
        return d;
      });

      TransactionSynchronizationManager.initSynchronization();
      try {
        service.manualUpload(projectId, providerId, StageCode.CONSTRUCTION, groupId, userId, file, "t");

        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, syncs.size());

        syncs.get(0).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(storage).delete(DocumentWorkflowService.BUCKET_DOCUMENTS, "fid");
      } finally {
        TransactionSynchronizationManager.clearSynchronization();
      }
    }
  }

  // Вспомогательная проверка payload (если захотите более строгую валидацию JSON)
  @SuppressWarnings("unused")
  private static void assertObjectNodeHas(ObjectNode node, String key, String expected) {
    assertNotNull(node.get(key));
    assertEquals(expected, node.get(key).asText());
  }
}
