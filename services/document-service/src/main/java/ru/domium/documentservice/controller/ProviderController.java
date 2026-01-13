package ru.domium.documentservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import ru.domium.documentservice.dto.DocumentDtos.AdvanceStageRequest;
import ru.domium.documentservice.dto.DocumentDtos.DocumentInstanceDto;
import ru.domium.documentservice.model.StageCode;
import ru.domium.documentservice.security.AuthorizationService;
import ru.domium.documentservice.service.DocumentMapper;
import ru.domium.documentservice.service.DocumentWorkflowService;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.domium.openapi.config.DomiumOpenApiAutoConfiguration;
import ru.domium.security.util.SecurityUtils;

@Tag(
    name = "Manager / Documents",
    description = "Операции поставщика: загрузка и обновление документов"
)
@SecurityRequirement(name = DomiumOpenApiAutoConfiguration.SECURITY_SCHEME_NAME)
@RestController
@RequestMapping("/provider")
public class ProviderController {

  private final DocumentWorkflowService workflow;
  private final AuthorizationService authz;

  public ProviderController(DocumentWorkflowService workflow, AuthorizationService authz) {
    this.workflow = workflow;
    this.authz = authz;
  }

  @Operation(
      summary = "Загрузить новую версию документа",
      description = """
        Загрузка новой версии файла документа.
        Предыдущая версия сохраняется в истории.
        Статус документа переводится в SENT_TO_USER.
        Доступно только для роли PROVIDER / ADMIN.
        """
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Новая версия документа загружена"),
      @ApiResponse(responseCode = "400", description = "Некорректный файл или состояние документа"),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
      @ApiResponse(responseCode = "404", description = "Документ не найден")
  })
  @Parameter(
      name = "documentId",
      description = "UUID документа",
      required = true
  )
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content = @Content(
          mediaType = MediaType.MULTIPART_FORM_DATA_VALUE
      ),
      description = "Multipart-запрос с PDF файлом и опциональным комментарием"
  )
  @PreAuthorize("hasRole('MANAGER')")
  @PostMapping(value = "/documents/{documentId}/uploadNewVersion",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public DocumentInstanceDto uploadNewVersion(@PathVariable UUID documentId,
      @RequestPart("file") MultipartFile file,
      @RequestPart(name = "comment", required = false) String comment,
      @AuthenticationPrincipal Jwt jwt) {
    authz.assertProvider(jwt);
    UUID providerId = UUID.fromString(SecurityUtils.getCurrentUserId(jwt));
    var doc = workflow.uploadNewVersion(documentId, providerId, file, comment);
    return DocumentMapper.toDto(doc);
  }

  @Operation(
      summary = "Ручная загрузка документа",
      description = """
        Загрузка уникального документа вручную.
        Документ может быть привязан к группе договора.
        Отправляется пользователю сразу после загрузки.
        Доступно только для роли MANAGER / ADMIN.
        """
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Документ успешно загружен"),
      @ApiResponse(responseCode = "400", description = "Некорректные параметры запроса"),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
  })
  @Parameter(
      name = "projectId",
      description = "UUID проекта",
      required = true
  )
  @Parameter(
      name = "stageCode",
      description = "UUID этапа проекта",
      required = true
  )
  @Parameter(
      name = "groupId",
      description = "UUID группы документов (опционально)",
      required = false
  )
  @Parameter(
      name = "userId",
      description = "UUID пользователя, которому адресован документ",
      required = true
  )
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content = @Content(
          mediaType = MediaType.MULTIPART_FORM_DATA_VALUE
      ),
      description = "Multipart-запрос с PDF файлом и опциональным комментарием"
  )
  @PreAuthorize("hasRole('MANAGER')")
  @PostMapping(value = "/projects/{projectId}/documents/manualUpload",
  consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public DocumentInstanceDto manualUpload(@PathVariable UUID projectId,
      @RequestPart("file") MultipartFile file,
      @RequestParam("stageCode") UUID stageCode,
      @RequestParam(name = "groupId", required = false) UUID groupId,
      @RequestParam(name = "userId") UUID userId,
      @RequestParam(name = "title", required = false) String title,
      @AuthenticationPrincipal Jwt jwt) {
    authz.assertProvider(jwt);
    UUID providerId = UUID.fromString(SecurityUtils.getCurrentUserId(jwt));
    var doc = workflow.manualUpload(projectId, providerId, stageCode, groupId, userId, file, title);
    return DocumentMapper.toDto(doc);
  }

  @Operation(
      summary = "Теневое удаление документа",
      description = """
      Помечает документ статусом DELETE.
      Документ перестает попадать в списки и становится недоступен через read-эндпоинты,
      но запись в БД и файл в Minio остаются.
      Доступно только для роли MANAGER / ADMIN.
      """
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Документ помечен как удалён"),
      @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
      @ApiResponse(responseCode = "404", description = "Документ не найден")
  })
  @Parameter(
      name = "documentId",
      description = "UUID документа",
      required = true
  )
  @Parameter(
      name = "comment",
      description = "Комментарий к удалению (опционально)",
      required = false
  )
  @PreAuthorize("hasRole('MANAGER')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/documents/{documentId}")
  public void softDelete(@PathVariable UUID documentId,
      @RequestParam(name = "comment", required = false) String comment,
      @AuthenticationPrincipal Jwt jwt) {
    authz.assertProvider(jwt);
    UUID providerId = UUID.fromString(SecurityUtils.getCurrentUserId(jwt));
    workflow.softDelete(documentId, providerId, comment);
  }
}
