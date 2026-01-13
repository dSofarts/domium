package ru.domium.projectservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.domium.projectservice.dto.request.CreateProjectRequest;
import ru.domium.projectservice.dto.request.UpdateProjectRequest;
import ru.domium.projectservice.dto.response.ProjectResponse;
import ru.domium.projectservice.service.ProjectService;
import ru.domium.security.util.SecurityUtils;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Project", description = "API для управления проектами")
public class ProjectController {

    private final ProjectService projectService;

    @Operation(
            summary = "Получить список проектов",
            description = "Возвращает список проектов. Доступно для аутентифицированных пользователей."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список проектов",
                    content = @Content(schema = @Schema(implementation = ProjectResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @Operation(
            summary = "Получить проект по ID",
            description = "Возвращает информацию о проекте по его ID. Доступно для аутентифицированных пользователей."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Информация о проекте найдена",
                    content = @Content(schema = @Schema(implementation = ProjectResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Проект не найден")
    })
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(
            @Parameter(description = "ID проекта", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID projectId) {
        ProjectResponse project = projectService.getProjectById(projectId);
        return ResponseEntity.ok(project);
    }

    @Operation(
            summary = "Создать проект",
            description = "Создаёт новый проект. Доступно только для менеджеров."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Проект успешно создан",
                    content = @Content(schema = @Schema(implementation = ProjectResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён")
    })
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                                 description = "Данные для создания проекта",
                                                                 required = true)
                                                         @RequestBody @Valid CreateProjectRequest request,
                                                         @Parameter(
                                                                 description = "JWT токен текущего пользователя",
                                                                 hidden = true)
                                                         @AuthenticationPrincipal Jwt jwt
    ) {
        UUID managerId = SecurityUtils.requireSubjectUuid(jwt);
        ProjectResponse created = projectService.createProject(request, managerId);

        var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{projectId}")
                .buildAndExpand(created.getId().toString())
                .toUri();

        return ResponseEntity.created(location).
                body(created);
    }

    @Operation(
            summary = "Обновить проект",
            description = "Обновляет информацию о проекте. Доступно только для менеджеров."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Проект успешно обновлён",
                    content = @Content(schema = @Schema(implementation = ProjectResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Проект не найден"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён")
    })
    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(
            @Parameter(description = "ID проекта", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID projectId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для обновления проекта",
                    required = true
            )
            @RequestBody @Valid UpdateProjectRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID managerId = SecurityUtils.requireSubjectUuid(jwt);
        ProjectResponse projectResponse = projectService.updateProject(projectId, request, managerId);
        return ResponseEntity.ok(projectResponse);
    }

    @Operation(
            summary = "Удалить проект",
            description = "Удаляет проект по ID. Доступно только для менеджеров."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Проект удалён"),
            @ApiResponse(responseCode = "404", description = "Проект не найден"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён")
    })
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @Parameter(description = "ID проекта", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID managerId = SecurityUtils.requireSubjectUuid(jwt);
        projectService.deleteProject(projectId, managerId);
        return ResponseEntity.noContent().build();
    }
}
