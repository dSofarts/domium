package ru.domium.building.api.dto.workitem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Вид работ (work item) внутри подэтапа (substage)")
public class WorkItemDto {
    @Schema(description = "Идентификатор вида работ")
    private UUID id;

    @Schema(description = "Название вида работ")
    private String name;

    @Schema(description = "Описание вида работ")
    private String description;

    @Schema(description = "Позиция вида работ для отображения (0..n-1)", example = "0")
    private int position;
}
