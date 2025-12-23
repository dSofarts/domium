package ru.domium.documentservice.controller;

import static ru.domium.security.util.SecurityUtils.hasRole;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import ru.domium.documentservice.dto.DocumentDtos.*;
import ru.domium.documentservice.model.*;
import ru.domium.documentservice.security.AuthorizationService;
import ru.domium.documentservice.service.DocumentMapper;
import ru.domium.documentservice.service.DocumentWorkflowService;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import ru.domium.security.util.SecurityUtils;

@Tag(
    name = "Project / Documents",
    description = "Список документов проекта с фильтрацией"
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/projects/{projectId}/documents")
public class ProjectDocumentsController {

  private final DocumentWorkflowService workflow;
  private final AuthorizationService authz;

  public ProjectDocumentsController(DocumentWorkflowService workflow, AuthorizationService authz) {
    this.workflow = workflow;
    this.authz = authz;
  }

  @Operation(
      summary = "Получить документы проекта",
      description = """
        Возвращает список документов проекта.

        Поведение зависит от роли:
        - PROVIDER / ADMIN — все документы проекта
        - USER — только документы, доступные пользователю
        """
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Список документов проекта"),
      @ApiResponse(responseCode = "403", description = "Нет доступа к проекту"),
      @ApiResponse(responseCode = "404", description = "Проект не найден")
  })
  @Parameter(
      name = "projectId",
      description = "UUID проекта",
      required = true
  )
  @Parameter(
      name = "status",
      description = "Фильтр по статусу документа",
      required = false
  )
  @Parameter(
      name = "stage",
      description = "Фильтр по этапу проекта (INIT_DOCS, CONSTRUCTION, FINAL_DOCS)",
      required = false
  )
  @Parameter(
      name = "groupType",
      description = "Фильтр по типу группы документов",
      required = false
  )
  @PreAuthorize("isAuthenticated()")
  @GetMapping
  public List<DocumentInstanceDto> list(@PathVariable UUID projectId,
      @RequestParam(required = false) DocumentStatus status,
      @RequestParam(required = false) StageCode stage,
      @RequestParam(required = false) DocumentGroupType groupType,
      @AuthenticationPrincipal Jwt jwt) {
    boolean isProvider = (hasRole(jwt, "builder") || hasRole(jwt, "admin"));
    List<DocumentInstance> docs;
    if (isProvider) {
      docs = workflow.listProjectDocuments(projectId, status, stage, groupType);
    } else {
      UUID userId = UUID.fromString(SecurityUtils.getCurrentUserId(jwt));
      docs = workflow.listProjectDocumentsForUser(projectId, userId, status, stage, groupType);
    }
    return docs.stream().map(DocumentMapper::toDto).toList();
  }

  /**
   * @deprecated
   * Эндпоинт не нужен сейчас, в будущем возможно
   * Под удаление
   */
  @Deprecated(since = "2025-12", forRemoval = true)
  @PostMapping("/generate")
  public List<DocumentInstanceDto> generate(@PathVariable UUID projectId,
      @RequestParam StageCode stage,
      @RequestBody(required = false) GenerateRequest body,
      @AuthenticationPrincipal Jwt jwt) {
    authz.assertProvider(jwt);
    UUID userId = body != null ? body.userId() : null;
    String projectType = body != null ? body.projectType() : null;
    Map<String, Object> data = body != null ? body.data() : Map.of();
    List<DocumentInstance> docs = workflow.generateForStage(projectId, stage, userId, projectType, data);
    return docs.stream().map(DocumentMapper::toDto).toList();
  }
}
