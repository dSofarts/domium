package ru.domium.documentservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;
import ru.domium.documentservice.dto.DocumentDtos.DocumentInstanceDto;
import ru.domium.documentservice.model.*;
import ru.domium.documentservice.security.AuthorizationService;
import ru.domium.documentservice.service.DocumentWorkflowService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProviderControllerTest {

  @Mock private DocumentWorkflowService workflow;
  @Mock private AuthorizationService authz;

  private ProviderController controller;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new ProviderController(workflow, authz);
  }

  // -------------------- uploadNewVersion --------------------

  @Test
  void uploadNewVersion_shouldAssertProvider_extractProviderId_callWorkflow_andReturnDto() {
    UUID documentId = UUID.randomUUID();
    UUID providerId = UUID.randomUUID();

    Jwt jwt = jwtWithSubjectAndRoles(providerId, List.of("manager"));

    MultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "x".getBytes());
    String comment = "new version";

    DocumentInstance doc = baseDoc(documentId, UUID.randomUUID());
    doc.setVersion(5);
    doc.setCurrentFileStorageId("fid");
    doc.setStatus(DocumentStatus.SENT_TO_USER);
    doc.setSentAt(Instant.now());

    when(workflow.uploadNewVersion(documentId, providerId, file, comment)).thenReturn(doc);

    DocumentInstanceDto dto = controller.uploadNewVersion(documentId, file, comment, jwt);

    assertNotNull(dto);
    assertEquals(documentId, dto.id());
    assertEquals(5, dto.version());
    assertEquals(DocumentStatus.SENT_TO_USER, dto.status());

    InOrder inOrder = inOrder(authz, workflow);
    inOrder.verify(authz).assertProvider(jwt);
    inOrder.verify(workflow).uploadNewVersion(documentId, providerId, file, comment);
    inOrder.verifyNoMoreInteractions();

    verifyNoMoreInteractions(workflow);
  }

  @Test
  void uploadNewVersion_shouldPassNullComment_whenNotProvided() {
    UUID documentId = UUID.randomUUID();
    UUID providerId = UUID.randomUUID();

    Jwt jwt = jwtWithSubjectAndRoles(providerId, List.of("manager"));

    MultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "x".getBytes());

    DocumentInstance doc = baseDoc(documentId, UUID.randomUUID());
    when(workflow.uploadNewVersion(documentId, providerId, file, null)).thenReturn(doc);

    controller.uploadNewVersion(documentId, file, null, jwt);

    verify(authz).assertProvider(jwt);
    verify(workflow).uploadNewVersion(documentId, providerId, file, null);
    verifyNoMoreInteractions(workflow);
  }

  // -------------------- manualUpload --------------------

  @Test
  void manualUpload_shouldAssertProvider_extractProviderId_callWorkflow_andReturnDto() {
    UUID projectId = UUID.randomUUID();
    UUID providerId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    UUID stage = UUID.randomUUID();
    Jwt jwt = jwtWithSubjectAndRoles(providerId, List.of("manager"));

    MultipartFile file = new MockMultipartFile("file", "manual.pdf", "application/pdf", "data".getBytes());

    String title = "Act 1";

    DocumentInstance created = baseDoc(UUID.randomUUID(), userId);
    created.setProjectId(projectId);
    created.setStageCode(stage);
    created.setGroup(group(groupId, projectId, DocumentGroupType.TECH_DOCS));
    created.setStatus(DocumentStatus.SENT_TO_USER);
    created.setVersion(1);
    created.setCurrentFileStorageId("fid");
    created.setSentAt(Instant.now());

    when(workflow.manualUpload(projectId, providerId, stage, groupId, userId, file, title))
        .thenReturn(created);

    DocumentInstanceDto dto = controller.manualUpload(projectId, file, stage, groupId, userId, title, jwt);

    assertNotNull(dto);
    assertEquals(projectId, dto.projectId());
    assertEquals(userId, dto.userId());
    assertEquals(stage, dto.stageCode());
    assertEquals(DocumentStatus.SENT_TO_USER, dto.status());
    assertEquals(1, dto.version());

    InOrder inOrder = inOrder(authz, workflow);
    inOrder.verify(authz).assertProvider(jwt);
    inOrder.verify(workflow).manualUpload(projectId, providerId, stage, groupId, userId, file, title);
    inOrder.verifyNoMoreInteractions();

    verifyNoMoreInteractions(workflow);
  }

  @Test
  void manualUpload_shouldPassNullGroupId_andNullTitle_whenNotProvided() {
    UUID projectId = UUID.randomUUID();
    UUID providerId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID stage = UUID.randomUUID();
    Jwt jwt = jwtWithSubjectAndRoles(providerId, List.of("manager"));

    MultipartFile file = new MockMultipartFile("file", "manual.pdf", "application/pdf", "data".getBytes());


    DocumentInstance created = baseDoc(UUID.randomUUID(), userId);
    created.setProjectId(projectId);
    created.setStageCode(stage);

    when(workflow.manualUpload(projectId, providerId, stage, null, userId, file, null))
        .thenReturn(created);

    controller.manualUpload(projectId, file, stage, null, userId, null, jwt);

    verify(authz).assertProvider(jwt);
    verify(workflow).manualUpload(projectId, providerId, stage, null, userId, file, null);
    verifyNoMoreInteractions(workflow);
  }

  // -------------------- helpers --------------------

  private static Jwt jwtWithSubjectAndRoles(UUID subject, List<String> roles) {
    return Jwt.withTokenValue("t")
        .header("alg", "none")
        .subject(subject.toString()) // SecurityUtils.getCurrentUserId(jwt) -> subject
        .claim("realm_access", Map.of("roles", roles))
        .build();
  }

  private static DocumentInstance baseDoc(UUID docId, UUID userId) {
    DocumentInstance d = new DocumentInstance();
    d.setId(docId);
    d.setUserId(userId);
    d.setProjectId(UUID.randomUUID());
    d.setStageCode(UUID.randomUUID());
    d.setStatus(DocumentStatus.CREATED);
    d.setVersion(1);
    d.setSentAt(Instant.now());
    return d;
  }

  private static DocumentGroup group(UUID id, UUID projectId, DocumentGroupType type) {
    DocumentGroup g = new DocumentGroup();
    g.setId(id);
    g.setProjectId(projectId);
    g.setType(type);
    return g;
  }
}
