package ru.domium.documentservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import ru.domium.documentservice.dto.DocumentDtos.*;
import ru.domium.documentservice.model.*;
import ru.domium.documentservice.security.AuthorizationService;
import ru.domium.documentservice.service.DocumentWorkflowService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentControllerTest {

  @Mock private DocumentWorkflowService workflow;
  @Mock private AuthorizationService authz;

  private DocumentController controller;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new DocumentController(workflow, authz);
  }

  // -------------------- details --------------------

  @Test
  void details_shouldCallWorkflowAndAuthz_andReturnMappedDto() {
    UUID docId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID stageCode = UUID.randomUUID();

    Jwt jwt = jwtWithSubjectAndRoles(userId, List.of("client"));

    DocumentInstance doc = new DocumentInstance();
    doc.setId(docId);
    doc.setProjectId(UUID.randomUUID());
    doc.setUserId(userId);
    doc.setStageCode(stageCode);
    doc.setStatus(DocumentStatus.SENT_TO_USER);
    doc.setVersion(2);
    doc.setCurrentFileStorageId("fid");
    doc.setSentAt(Instant.now());

    DocumentFileVersion v1 = new DocumentFileVersion();
    v1.setId(UUID.randomUUID());
    v1.setDocument(doc);
    v1.setVersion(1);
    v1.setFileStorageId("f1");
    v1.setCreatedByType(ActorType.MANAGER);
    v1.setCreatedById(UUID.randomUUID());
    v1.setCreatedAt(Instant.now());

    DocumentComment c1 = new DocumentComment();
    c1.setId(UUID.randomUUID());
    c1.setDocument(doc);
    c1.setAuthorType(CommentAuthorType.USER);
    c1.setAuthorId(userId);
    c1.setText("ok");
    c1.setCreatedAt(Instant.now());

    DocumentSignature s1 = new DocumentSignature();
    s1.setId(UUID.randomUUID());
    s1.setDocument(doc);
    s1.setSignerUserId(userId);
    s1.setSignerType(ActorType.CLIENT);
    s1.setType(SignatureType.SIMPLE);
    s1.setFileHash("hash");
    s1.setSignedAt(Instant.now());

    DocumentAuditLog a1 = new DocumentAuditLog();
    a1.setId(UUID.randomUUID());
    a1.setDocument(doc);
    a1.setActorType(ActorType.CLIENT);
    a1.setActorId(userId);
    a1.setAction(AuditAction.SENT);
    a1.setCreatedAt(Instant.now());

    when(workflow.getDocument(docId)).thenReturn(doc);
    when(workflow.listVersions(docId)).thenReturn(List.of(v1));
    when(workflow.listComments(docId)).thenReturn(List.of(c1));
    when(workflow.listSignatures(docId)).thenReturn(List.of(s1));
    when(workflow.listAudits(docId)).thenReturn(List.of(a1));

    DocumentDetailsDto dto = controller.details(docId, jwt);

    assertNotNull(dto);
    assertNotNull(dto.document());
    assertEquals(docId, dto.document().id());
    assertEquals(2, dto.document().version());
    assertEquals(DocumentStatus.SENT_TO_USER, dto.document().status());

    assertEquals(1, dto.versions().size());
    assertEquals(1, dto.comments().size());
    assertEquals(1, dto.signatures().size());
    assertEquals(1, dto.audit().size());

    verify(workflow).getDocument(docId);
    verify(authz).assertCanReadDocument(jwt, doc);
    verify(workflow).listVersions(docId);
    verify(workflow).listComments(docId);
    verify(workflow).listSignatures(docId);
    verify(workflow).listAudits(docId);

    verifyNoMoreInteractions(authz);
  }

  // -------------------- file --------------------

  @Test
  void file_shouldUseBuilderActorType_whenJwtHasBuilderRole() {
    UUID docId = UUID.randomUUID();
    UUID providerIdAsSubject = UUID.randomUUID();
    UUID stageCode = UUID.randomUUID();
    Jwt jwt = jwtWithSubjectAndRoles(providerIdAsSubject, List.of("manager"));

    DocumentInstance doc = new DocumentInstance();
    doc.setId(docId);
    doc.setUserId(UUID.randomUUID());
    doc.setProjectId(UUID.randomUUID());
    doc.setStageCode(stageCode);
    doc.setStatus(DocumentStatus.SENT_TO_USER);
    doc.setVersion(1);
    doc.setCurrentFileStorageId("fid");

    when(workflow.getDocument(docId)).thenReturn(doc);

    InputStream stream = new ByteArrayInputStream("pdf".getBytes());
    when(workflow.loadDocumentFile(eq(docId), eq(true), eq(ActorType.MANAGER), eq(providerIdAsSubject)))
        .thenReturn(stream);

    HttpServletRequest req = mock(HttpServletRequest.class);

    ResponseEntity<Resource> resp = controller.file(docId, true, false, jwt, req);

    assertEquals(200, resp.getStatusCode().value());
    assertEquals(MediaType.APPLICATION_PDF, resp.getHeaders().getContentType());

    String cd = resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
    assertNotNull(cd);
    assertTrue(cd.contains("inline"));
    assertTrue(cd.contains("document-" + docId + ".pdf"));

    assertNotNull(resp.getBody());
    assertTrue(resp.getBody() instanceof InputStreamResource);

    verify(authz).assertCanReadDocument(jwt, doc);
    verify(workflow).loadDocumentFile(docId, true, ActorType.MANAGER, providerIdAsSubject);
  }

  @Test
  void file_shouldUseClientActorType_whenNoBuilderOrAdminRole() {
    UUID docId = UUID.randomUUID();
    UUID userIdAsSubject = UUID.randomUUID();

    Jwt jwt = jwtWithSubjectAndRoles(userIdAsSubject, List.of("client"));

    DocumentInstance doc = new DocumentInstance();
    doc.setId(docId);
    doc.setUserId(userIdAsSubject);

    when(workflow.getDocument(docId)).thenReturn(doc);

    InputStream stream = new ByteArrayInputStream("pdf".getBytes());
    when(workflow.loadDocumentFile(eq(docId), eq(false), eq(ActorType.CLIENT), eq(userIdAsSubject)))
        .thenReturn(stream);

    HttpServletRequest req = mock(HttpServletRequest.class);

    ResponseEntity<Resource> resp = controller.file(docId, false, false, jwt, req);

    assertEquals(200, resp.getStatusCode().value());
    verify(workflow).loadDocumentFile(docId, false, ActorType.CLIENT, userIdAsSubject);
  }

  // -------------------- sign --------------------

  @Test
  void sign_shouldCallWorkflowSign_withRemoteAddrAndUserAgent_andReturnDto() {
    UUID docId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    Jwt jwt = jwtWithSubjectAndRoles(userId, List.of("client"));

    DocumentInstance doc = new DocumentInstance();
    doc.setId(docId);
    doc.setUserId(userId);

    when(workflow.getDocument(docId)).thenReturn(doc);

    HttpServletRequest req = mock(HttpServletRequest.class);
    when(req.getRemoteAddr()).thenReturn("10.0.0.1");
    when(req.getHeader("User-Agent")).thenReturn("JUnit");

    DocumentSignature sig = new DocumentSignature();
    sig.setId(UUID.randomUUID());
    sig.setDocument(doc);
    sig.setSignerUserId(userId);
    sig.setSignerType(ActorType.CLIENT);
    sig.setType(SignatureType.SIMPLE);
    sig.setSignedAt(Instant.now());
    sig.setFileHash("hash");

    when(workflow.sign(
        eq(docId),
        eq(userId),
        eq(ActorType.CLIENT),
        eq(SignatureType.SIMPLE),
        eq("1234"),
        eq("10.0.0.1"),
        eq("JUnit")
    )).thenReturn(sig);

    SignatureDto dto = controller.sign(docId, new SignRequest(SignatureType.SIMPLE, "1234"), jwt, req);

    assertNotNull(dto);
    assertEquals(sig.getId(), dto.id());
    assertEquals(docId, dto.documentId());
    assertEquals(userId, dto.signerUserId());
    assertEquals(ActorType.CLIENT, dto.signerType());
    assertEquals(SignatureType.SIMPLE, dto.type());
    assertEquals("hash", dto.fileHash());

    verify(authz).assertCanReadDocument(jwt, doc);
    verify(workflow).sign(docId, userId, ActorType.CLIENT, SignatureType.SIMPLE, "1234", "10.0.0.1", "JUnit");
  }

  // -------------------- reject --------------------

  @Test
  void reject_shouldCallWorkflowReject_withComment_whenBodyProvided() {
    UUID docId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    Jwt jwt = jwtWithSubjectAndRoles(userId, List.of("client"));

    DocumentInstance doc = new DocumentInstance();
    doc.setId(docId);
    doc.setUserId(userId);

    when(workflow.getDocument(docId)).thenReturn(doc);

    controller.reject(docId, new RejectRequest("nope"), jwt);

    verify(authz).assertCanReadDocument(jwt, doc);
    verify(workflow).reject(docId, userId, "nope");
  }

  @Test
  void reject_shouldCallWorkflowReject_withNullComment_whenBodyIsNull() {
    UUID docId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    Jwt jwt = jwtWithSubjectAndRoles(userId, List.of("client"));

    DocumentInstance doc = new DocumentInstance();
    doc.setId(docId);
    doc.setUserId(userId);

    when(workflow.getDocument(docId)).thenReturn(doc);

    controller.reject(docId, null, jwt);

    verify(authz).assertCanReadDocument(jwt, doc);
    verify(workflow).reject(docId, userId, null);
  }

  // -------------------- helpers --------------------

  private static Jwt jwtWithSubjectAndRoles(UUID subject, List<String> roles) {
    return Jwt.withTokenValue("t")
        .header("alg", "none")
        .subject(subject.toString()) // SecurityUtils.getCurrentUserId(jwt) использует subject
        .claim("realm_access", Map.of("roles", roles)) // SecurityUtils.hasRole читает realm_access.roles
        .build();
  }
}
