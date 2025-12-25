package ru.domium.building.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Schema(description = "Детальная информация о стройке (write-model view)")
public class BuildingDto {
    @Schema(description = "Идентификатор стройки", example = "8f1d6d18-1bfb-4a8a-95c9-6bfe0c2e8a0b")
    private UUID id;

    @Schema(description = "Идентификатор проекта", example = "b2c7c12b-0d1f-4f29-8ad0-0fa3d8c9f5d1")
    private UUID projectId;

    @Schema(description = "Текущее имя стадии", example = "FOUNDATION")
    private String currentStageName;

    @Schema(description = "Прогресс 0..100", example = "25")
    private int progress;

    @Schema(description = "Статус стройки", example = "ACTIVE")
    private String status;

    @Schema(description = "Дата/время создания", example = "2025-12-24T12:00:00")
    private LocalDateTime createdAt;
}
