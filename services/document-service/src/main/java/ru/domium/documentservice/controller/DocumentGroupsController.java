package ru.domium.documentservice.controller;

import ru.domium.documentservice.dto.DocumentDtos.DocumentInstanceDto;
import ru.domium.documentservice.security.AuthorizationService;
import ru.domium.documentservice.service.DocumentMapper;
import ru.domium.documentservice.service.DocumentWorkflowService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


/**
 * @deprecated
 * Этот контроллер оставлен временно.
 * НЕ ИСПОЛЬЗОВАТЬ.
 *
 * Причина:
 * - пока нет необходимости в группировке документов
 * - возможно эндпоинты будут удалены
 */
@Deprecated(since = "2025-12", forRemoval = true)
@RestController
@RequestMapping("/documentGroups")
public class DocumentGroupsController {

  private final DocumentWorkflowService workflow;
  private final AuthorizationService authz;

  public DocumentGroupsController(DocumentWorkflowService workflow, AuthorizationService authz) {
    this.workflow = workflow;
    this.authz = authz;
  }

  @GetMapping("/{groupId}/documents")
  public List<DocumentInstanceDto> listGroupDocuments(@PathVariable UUID groupId, Authentication authentication) {
    // Group listing is effectively a document listing; we enforce per-document access.
    var groupDocs = workflow.listGroupDocuments(groupId);
    for (var d : groupDocs) {
      authz.assertCanReadDocument(authentication, d);
    }
    return groupDocs.stream().map(DocumentMapper::toDto).toList();
  }
}
