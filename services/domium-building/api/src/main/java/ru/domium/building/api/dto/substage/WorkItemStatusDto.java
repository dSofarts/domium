package ru.domium.building.api.dto.substage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Schema(description = "Статус вида работ (work item) для конкретной стройки")
public class WorkItemStatusDto {
    @Schema(description = "Идентификатор вида работ")
    private UUID id;

    @Schema(description = "Название вида работ")
    private String name;

    @Schema(description = "Описание вида работ")
    private String description;

    @Schema(description = "Позиция вида работ для отображения (0..n-1)", example = "0")
    private int position;

    @Schema(description = "Флаг выполнения вида работ")
    private boolean completed;

    @Schema(description = "Время выполнения (UTC), если выполнен")
    private Instant completedAt;
}
