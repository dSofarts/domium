package ru.domium.building.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import ru.domium.building.api.dto.project.ProjectDto;

@Data
@Schema(description = "Детали стройки: building + витрина для UI (project)")
public class BuildingDetailsDto {
    @Schema(description = "Основные поля стройки")
    private BuildingDto building;

    @Schema(description = "Витрина для UI (project/manager/objectInfo)")
    private ProjectDto project;
}
