package ru.domium.building.api.dto.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Запрос на создание workflow")
public class CreateWorkflowRequest {
    @Schema(description = "Название workflow", example = "DEFAULT")
    private String name;

    @Schema(description = "Сделать workflow активным сразу", example = "true")
    private boolean active = true;

    @Schema(description = "Список стадий (опционально, если не задан — можно добавить позже)")
    private List<StageDto> stages;
}
