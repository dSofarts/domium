package ru.domium.building.api.dto.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Стадия workflow")
public class StageDto {
    @Schema(description = "Идентификатор стадии")
    private UUID id;

    @Schema(description = "Имя стадии (уникально в рамках workflow)", example = "FOUNDATION")
    private String name;

    @Schema(description = "Описание стадии")
    private String description;

    @Schema(description = "Плановая длительность стадии (дни)", example = "3")
    private int plannedDays;

    @Schema(description = "Позиция стадии в workflow (0..n-1)", example = "0")
    private int position;

    @Schema(description = "Список подэтапов (substage), которые можно выполнять в любом порядке")
    private List<SubStageDto> subStages;
}


