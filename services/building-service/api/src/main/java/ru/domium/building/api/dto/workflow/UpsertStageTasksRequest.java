package ru.domium.building.api.dto.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Запрос на upsert дерева задач (subStages + workItems) для стадии workflow")
public class UpsertStageTasksRequest {
    @Schema(description = "Полный список подэтапов стадии (subStages) с вложенными видами работ (workItems)")
    private List<SubStageDto> subStages;
}
