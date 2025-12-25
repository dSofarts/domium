package ru.domium.building.api.dto.substage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Статус подэтапа (substage) для конкретной стройки")
public class SubStageStatusDto {
    @Schema(description = "Идентификатор подэтапа")
    private UUID id;

    @Schema(description = "Название подэтапа")
    private String name;

    @Schema(description = "Описание подэтапа")
    private String description;

    @Schema(description = "Позиция подэтапа для отображения (0..n-1)", example = "0")
    private int position;

    @Schema(description = "Флаг завершения подэтапа")
    private boolean completed;

    @Schema(description = "Время завершения (UTC), если завершён")
    private Instant completedAt;

    @Schema(description = "Виды работ (work items) внутри подэтапа со статусами выполнения")
    private List<WorkItemStatusDto> workItems;
}
