package ru.domium.projectservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
//import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.domium.projectservice.dto.response.ProjectImageResponse;
import ru.domium.projectservice.service.ProjectImageService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects/{projectId}/images")
@RequiredArgsConstructor
@Tag(name = "Project Images", description = "API для управления изображениями проекта")
public class ProjectImageController {

    private final ProjectImageService imageService;

    @Operation(
            summary = "Добавить изображения к проекту",
            description = "Загружает одно или несколько изображений для проекта. Доступно только для роли MANAGER."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Изображения успешно добавлены",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ProjectImageResponse.class))
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос или файлы"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Проект не найден")
    })
//    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<ProjectImageResponse> addImages(
            @Parameter(description = "ID проекта", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID projectId,
            @RequestBody(
                    description = "Список изображений для загрузки",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            array = @ArraySchema(schema = @Schema(type = "string", format = "binary"))
                    )
            )
            @RequestPart("images") List<MultipartFile> images) {
        return imageService.addImagesToProject(projectId, images);
    }
}

