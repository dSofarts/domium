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
import ru.domium.projectservice.dto.request.CreateProjectRequest;
import ru.domium.projectservice.dto.response.ProjectResponse;
import ru.domium.projectservice.service.ProjectService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Project", description = "API для управления проектами")
public class ProjectController {

    private final ProjectService projectService;

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
        ProjectResponse created = projectService.createProject(request);
        URI location = URI.create("/projects/" + created.getId());
        return ResponseEntity.created(location).
                body(created);
    }

    @Deprecated(forRemoval = false)
    @Operation(
            summary = "Обновить проект",
            description = "(В доработке) Обновляет информацию о проекте. Доступно только для менеджеров.",
            deprecated = true
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
            @RequestBody @Valid CreateProjectRequest request) {
        //TODO: Implementation for updating a project would go here
        return ResponseEntity.notFound().build(); // Placeholder response
    }

    @Deprecated(forRemoval = false)
    @Operation(
            summary = "Удалить проект",
            description = "(В доработке) Удаляет проект по ID. Доступно только для менеджеров.",
            deprecated = true
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
            @PathVariable UUID projectId) {
        //TODO: Deleting a project implementation would go here
        return ResponseEntity.notFound().build(); // Placeholder response
    }

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
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(
            @Parameter(description = "ID проекта", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID projectId) {
        ProjectResponse project = projectService.getProjectById(projectId);
        return ResponseEntity.ok(project);
    }

    @Deprecated(forRemoval = false)
    @Operation(
            summary = "Лайк проекта",
            description = "Метод потенциальный и пока не реализован.",
            deprecated = true
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "Не реализовано (заглушка)")
    })
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping("/{projectId}/like")
    public ResponseEntity<Void> likeProject(@PathVariable String projectId) {
//        TODO: Implementation for liking a project would go here
        return ResponseEntity.notFound().build(); // Placeholder response
    }

    @Deprecated(forRemoval = false)
    @Operation(
            summary = "Убрать лайк у проекта",
            description = "Метод потенциальный и пока не реализован.",
            deprecated = true
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "Не реализовано (заглушка)")
    })
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping("/{projectId}/unlike")
    public ResponseEntity<Void> unlikeProject(@PathVariable String projectId) {
//        TODO: Implementation for unliking a project would go here
        return ResponseEntity.notFound().build(); // Placeholder response
    }
}
