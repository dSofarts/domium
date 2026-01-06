package ru.domium.documentservice.controller;

import static ru.domium.security.util.SecurityUtils.hasRole;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import ru.domium.documentservice.dto.DocumentDtos.*;
import ru.domium.documentservice.model.*;
import ru.domium.documentservice.security.AuthorizationService;
import ru.domium.documentservice.service.DocumentMapper;
import ru.domium.documentservice.service.DocumentWorkflowService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.jwt.Jwt;
import ru.domium.security.util.SecurityUtils;

@Tag(
    name = "Documents",
    description = "Просмотр, подписание и отклонение документов"
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/documents")
public class DocumentController {

  private final DocumentWorkflowService workflow;
  private final AuthorizationService authz;

  public DocumentController(DocumentWorkflowService workflow, AuthorizationService authz) {
    this.workflow = workflow;
    this.authz = authz;
  }

  @Operation(
      summary = "Получить детали документа",
      description = "Возвращает метаданные документа, версии файлов, комментарии, подписи и аудит"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Документ найден"),
      @ApiResponse(responseCode = "403", description = "Нет доступа к документу"),
      @ApiResponse(responseCode = "404", description = "Документ не найден")
  })
  @Parameter(
      name = "documentId",
      description = "UUID документа",
      required = true
  )
  @GetMapping("/{documentId}")
  public DocumentDetailsDto details(@PathVariable UUID documentId, @AuthenticationPrincipal Jwt jwt) {
    var doc = workflow.getDocument(documentId);
    authz.assertCanReadDocument(jwt, doc);

    var versions = workflow.listVersions(documentId);
    var comments = workflow.listComments(documentId);
    var signatures = workflow.listSignatures(documentId);
    var audits = workflow.listAudits(documentId);
    return DocumentMapper.toDetails(doc, versions, comments, signatures, audits);
  }

  @Operation(
      summary = "Получить файл документа",
      description = "Возвращает PDF-файл документа. Может отметить документ как просмотренный"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "PDF файл",
          content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)
      ),
      @ApiResponse(responseCode = "403", description = "Нет доступа к документу"),
      @ApiResponse(responseCode = "404", description = "Документ или файл не найден")
  })
  @Parameter(
      name = "documentId",
      description = "UUID документа",
      required = true
  )
  @Parameter(
      name = "markViewed",
      description = "Пометить документ как просмотренный",
      example = "true"
  )
  @PreAuthorize("isAuthenticated()")
  @GetMapping(value = "/{documentId}/file", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<Resource> file(@PathVariable UUID documentId,
      @RequestParam(defaultValue = "true") boolean markViewed,
      @AuthenticationPrincipal Jwt jwt,
      HttpServletRequest request) {
    var doc = workflow.getDocument(documentId);
    authz.assertCanReadDocument(jwt, doc);

    ActorType actorType = (hasRole(jwt, "manager") || hasRole(jwt, "admin"))
        ? ActorType.MANAGER : ActorType.CLIENT;
    UUID actorId = UUID.fromString(SecurityUtils.getCurrentUserId(jwt));

    InputStream is = workflow.loadDocumentFile(documentId, markViewed, actorType, actorId);
    var resource = new InputStreamResource(is);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"document-" + documentId + ".pdf\"")
        .contentType(MediaType.APPLICATION_PDF)
        .body(resource);
  }

  @Operation(
      summary = "Подписать документ",
      description = "Подписание документа пользователем (SIMPLE signature)"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Документ подписан"),
      @ApiResponse(responseCode = "400", description = "Некорректный статус документа или код подтверждения"),
      @ApiResponse(responseCode = "403", description = "Нет доступа"),
      @ApiResponse(responseCode = "404", description = "Документ не найден")
  })
  @Parameter(
      name = "documentId",
      description = "UUID документа",
      required = true
  )
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Данные подписи (тип подписи и код подтверждения)",
      required = true
  )
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{documentId}/sign")
  public SignatureDto sign(@PathVariable UUID documentId, @RequestBody SignRequest body,
      @AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
    UUID userId = UUID.fromString(SecurityUtils.getCurrentUserId(jwt));
    var doc = workflow.getDocument(documentId);
    authz.assertCanReadDocument(jwt, doc);

    var sig = workflow.sign(documentId, userId, body.signatureType(), body.confirmationCode(), request.getRemoteAddr(), request.getHeader("User-Agent"));
    return DocumentMapper.toDto(sig);
  }

  @Operation(
      summary = "Отклонить документ",
      description = "Отклонение документа с комментарием пользователя"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Документ отклонён"),
      @ApiResponse(responseCode = "400", description = "Некорректный статус документа"),
      @ApiResponse(responseCode = "403", description = "Нет доступа"),
      @ApiResponse(responseCode = "404", description = "Документ не найден")
  })
  @Parameter(
      name = "documentId",
      description = "UUID документа",
      required = true
  )
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Комментарий пользователя (опционально)",
      required = false
  )
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{documentId}/reject")
  public void reject(@PathVariable UUID documentId,
      @RequestBody RejectRequest body,
      @AuthenticationPrincipal Jwt jwt) {
    UUID userId = UUID.fromString(SecurityUtils.getCurrentUserId(jwt));
    var doc = workflow.getDocument(documentId);
    authz.assertCanReadDocument(jwt, doc);
    workflow.reject(documentId, userId, body != null ? body.comment() : null);
  }
}
