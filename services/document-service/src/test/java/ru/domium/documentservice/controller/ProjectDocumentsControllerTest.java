package ru.domium.documentservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.oauth2.jwt.Jwt;
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

class ProjectDocumentsControllerTest {

  @Mock private DocumentWorkflowService workflow;
  @Mock private AuthorizationService authz;

  private ProjectDocumentsController controller;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new ProjectDocumentsController(workflow, authz);
  }

  @Test
  void list_shouldCallProviderMethod_whenRoleIsBuilder() {
    UUID projectId = UUID.randomUUID();
    Jwt jwt = jwtWithSubjectAndRoles(UUID.randomUUID(), List.of("builder"));

    DocumentStatus status = DocumentStatus.SENT_TO_USER;
    StageCode stage = StageCode.INIT_DOCS;
    DocumentGroupType groupType = DocumentGroupType.ADDITIONAL_AGREEMENTS;

    DocumentInstance d1 = doc(projectId, UUID.randomUUID(), status, stage, groupType, 1);
    DocumentInstance d2 = doc(projectId, UUID.randomUUID(), status, stage, groupType, 2);

    when(workflow.listProjectDocuments(projectId, status, stage, groupType)).thenReturn(List.of(d1, d2));

    List<DocumentInstanceDto> result = controller.list(projectId, status, stage, groupType, jwt);

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(d1.getId(), result.get(0).id());
    assertEquals(d2.getId(), result.get(1).id());

    verify(workflow).listProjectDocuments(projectId, status, stage, groupType);
    verify(workflow, never()).listProjectDocumentsForUser(any(), any(), any(), any(), any());
    verifyNoInteractions(authz);
  }

  @Test
  void list_shouldCallProviderMethod_whenRoleIsAdmin() {
    UUID projectId = UUID.randomUUID();
    Jwt jwt = jwtWithSubjectAndRoles(UUID.randomUUID(), List.of("admin"));

    DocumentInstance d = doc(projectId, UUID.randomUUID(),
        DocumentStatus.SENT_TO_USER, StageCode.CONSTRUCTION, DocumentGroupType.ADDITIONAL_AGREEMENTS, 1);

    when(workflow.listProjectDocuments(eq(projectId), isNull(), isNull(), isNull()))
        .thenReturn(List.of(d));

    List<DocumentInstanceDto> result = controller.list(projectId, null, null, null, jwt);

    assertEquals(1, result.size());
    assertEquals(d.getId(), result.get(0).id());

    verify(workflow).listProjectDocuments(projectId, null, null, null);
    verify(workflow, never()).listProjectDocumentsForUser(any(), any(), any(), any(), any());
    verifyNoInteractions(authz);
  }

  @Test
  void list_shouldCallUserMethod_whenNoBuilderOrAdminRole_andPassUserIdFromJwtSubject() {
    UUID projectId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    Jwt jwt = jwtWithSubjectAndRoles(userId, List.of("client")); // не builder, не admin

    DocumentStatus status = DocumentStatus.SENT_TO_USER;
    StageCode stage = StageCode.FINAL_DOCS;
    DocumentGroupType groupType = DocumentGroupType.FINAL_ACTS;

    DocumentInstance d = doc(projectId, userId, status, stage, groupType, 3);

    when(workflow.listProjectDocumentsForUser(projectId, userId, status, stage, groupType))
        .thenReturn(List.of(d));

    List<DocumentInstanceDto> result = controller.list(projectId, status, stage, groupType, jwt);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(d.getId(), result.get(0).id());
    assertEquals(d.getStatus(), result.get(0).status());
    assertEquals(d.getStageCode(), result.get(0).stageCode());

    verify(workflow).listProjectDocumentsForUser(projectId, userId, status, stage, groupType);
    verify(workflow, never()).listProjectDocuments(any(), any(), any(), any());
    verifyNoInteractions(authz);
  }

  @Test
  void list_shouldTreatMissingRolesAsUser_andCallUserMethod() {
    UUID projectId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    // roles отсутствуют
    Jwt jwt = Jwt.withTokenValue("t")
        .header("alg", "none")
        .subject(userId.toString())
        .build();

    DocumentInstance d = doc(projectId, userId,
        DocumentStatus.SENT_TO_USER, StageCode.INIT_DOCS, DocumentGroupType.ADDITIONAL_AGREEMENTS, 1);

    when(workflow.listProjectDocumentsForUser(eq(projectId), eq(userId), isNull(), isNull(), isNull()))
        .thenReturn(List.of(d));

    List<DocumentInstanceDto> result = controller.list(projectId, null, null, null, jwt);

    assertEquals(1, result.size());
    verify(workflow).listProjectDocumentsForUser(projectId, userId, null, null, null);
    verify(workflow, never()).listProjectDocuments(any(), any(), any(), any());
    verifyNoInteractions(authz);
  }

  // -------------------- helpers --------------------

  private static Jwt jwtWithSubjectAndRoles(UUID subject, List<String> roles) {
    return Jwt.withTokenValue("t")
        .header("alg", "none")
        .subject(subject.toString())
        .claim("realm_access", Map.of("roles", roles))
        .build();
  }

  private static DocumentInstance doc(UUID projectId, UUID userId,
      DocumentStatus status, StageCode stage,
      DocumentGroupType groupType, int version) {
    DocumentInstance d = new DocumentInstance();
    d.setId(UUID.randomUUID());
    d.setProjectId(projectId);
    d.setUserId(userId);
    d.setStatus(status);
    d.setStageCode(stage);
    d.setVersion(version);
    d.setSentAt(Instant.now());
    DocumentGroup g = new DocumentGroup();
    g.setId(UUID.randomUUID());
    g.setProjectId(projectId);
    g.setType(groupType);
    d.setGroup(g);
    return d;
  }
}
