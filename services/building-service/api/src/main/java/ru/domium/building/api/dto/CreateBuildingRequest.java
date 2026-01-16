package ru.domium.building.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import ru.domium.building.api.dto.project.ProjectDto;

import java.util.UUID;

@Data
@Schema(description = "Запрос на инициацию стройки")
public class CreateBuildingRequest {
    @Schema(description = "Информация о проекте: идентификатор + менеджер + objectInfo (название/атрибуты/прогресс/стадия)")
    private ProjectDto project;

    @Schema(description = "Опционально: workflowId (если не задан — берётся активный workflow менеджера или создаётся DEFAULT)")
    private UUID workflowId;
}
