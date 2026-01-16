package ru.domium.building.api.dto.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import ru.domium.building.api.dto.workitem.WorkItemDto;

import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Подэтап (substage) внутри стадии workflow. Может выполняться в любом порядке.")
public class SubStageDto {
    @Schema(description = "Идентификатор подэтапа")
    private UUID id;

    @Schema(description = "Название подэтапа (уникально в рамках стадии)")
    private String name;

    @Schema(description = "Описание подэтапа")
    private String description;

    @Schema(description = "Позиция подэтапа для отображения (0..n-1)", example = "0")
    private int position;

    @Schema(description = "Список видов работ (work items) внутри подэтапа")
    private List<WorkItemDto> workItems;
}
