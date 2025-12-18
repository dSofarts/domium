package ru.domium.documentservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import ru.domium.documentservice.exception.ApiExceptions;
import ru.domium.documentservice.model.*;
import ru.domium.documentservice.repository.DocumentTemplateRepository;
import ru.domium.documentservice.security.AuthorizationService;
import ru.domium.documentservice.service.DocumentWorkflowService;
import ru.domium.documentservice.service.FileStorageService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


/**
 * @deprecated
 * Этот контроллер оставлен временно.
 * НЕ ИСПОЛЬЗОВАТЬ.
 *
 * Причина:
 * - Планировал создать шаблоны документов в которые подставляются данные пользователя
 * и автоматически создается документ для подписания
 * - возможно эндпоинты будут удалены
 */
@Deprecated(since = "2025-12", forRemoval = true)
@RestController
@RequestMapping("/provider/templates")
public class TemplateController {

  private final DocumentTemplateRepository repo;
  private final FileStorageService storage;
  private final AuthorizationService authz;

  public TemplateController(DocumentTemplateRepository repo, FileStorageService storage, AuthorizationService authz) {
    this.repo = repo;
    this.storage = storage;
    this.authz = authz;
  }

  @GetMapping
  public List<DocumentTemplate> list(Authentication authentication) {
    authz.assertProvider(authentication);
    return repo.findAll();
  }

  @GetMapping("/{id}")
  public DocumentTemplate get(@PathVariable UUID id, Authentication authentication) {
    authz.assertProvider(authentication);
    return repo.findById(id).orElseThrow(() -> ApiExceptions.notFound("Template not found"));
  }

  /**
   * MVP template creation: upload a file to MinIO bucket 'templates' and create metadata record.
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public DocumentTemplate create(
      @RequestPart("file") MultipartFile file,
      @RequestParam("code") String code,
      @RequestParam("name") String name,
      @RequestParam(name = "description", required = false) String description,
      @RequestParam("stageCode") StageCode stageCode,
      @RequestParam(name = "tagsJson", required = false) JsonNode tagsJson,
      @RequestParam(name = "required", defaultValue = "false") boolean required,
      @RequestParam(name = "projectType", required = false) String projectType,
      @RequestParam(name = "templateEngineType", defaultValue = "TEXT") TemplateEngineType templateEngineType,
      Authentication authentication
  ) {
    authz.assertProvider(authentication);

    if (file == null || file.isEmpty()) {
      throw ApiExceptions.badRequest("file is required");
    }

    String fileId;
    try {
      fileId = storage.save(
          DocumentWorkflowService.BUCKET_TEMPLATES,
          file.getInputStream(),
          file.getSize(),
          file.getContentType(),
          file.getOriginalFilename()
      );
    } catch (Exception e) {
      throw ApiExceptions.badRequest("Failed to upload template file: " + e.getMessage());
    }

    DocumentTemplate t = new DocumentTemplate();
    t.setCode(code);
    t.setName(name);
    t.setDescription(description);
    t.setStageCode(stageCode);
    t.setTagsJson(tagsJson);
    t.setRequired(required);
    t.setProjectType(projectType);
    t.setFileStorageId(fileId);
    t.setTemplateEngineType(templateEngineType);

    return repo.save(t);
  }


  @PutMapping("/{id}")
  public DocumentTemplate update(@PathVariable UUID id,
      @RequestBody DocumentTemplate body,
      Authentication authentication) {
    authz.assertProvider(authentication);
    DocumentTemplate t = repo.findById(id).orElseThrow(() -> ApiExceptions.notFound("Template not found"));
    t.setName(body.getName());
    t.setDescription(body.getDescription());
    t.setStageCode(body.getStageCode());
    t.setTagsJson(body.getTagsJson());
    t.setRequired(body.isRequired());
    t.setProjectType(body.getProjectType());
    t.setTemplateEngineType(body.getTemplateEngineType());
    // fileStorageId update intentionally omitted from JSON update (use dedicated file upload if needed)
    return repo.save(t);
  }
}
