package ru.domium.building.api.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Объект проекта, используемый на фронте")
public class ProjectDto {
    @Schema(description = "Идентификатор стройки (buildingId)")
    private UUID buildingId;

    @Schema(description = "Идентификатор проекта")
    private UUID id;

    @Schema(description = "Менеджер проекта (id + name)")
    private UserDto manager;

    @Schema(description = "Информация об объекте/проекте: имя + атрибуты + прогресс + стадия")
    private ObjectInfoDto objectInfo;
}
