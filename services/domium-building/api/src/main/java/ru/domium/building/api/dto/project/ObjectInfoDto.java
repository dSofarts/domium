package ru.domium.building.api.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "Информация для UI по объекту/проекту: имя + атрибуты + прогресс + текущая стадия")
public class ObjectInfoDto {
    @Schema(description = "Название проекта/объекта")
    private String projectName;

    @Schema(description = "Произвольные атрибуты объекта (пока без строгой схемы)")
    private Map<String, Object> attributes;

    @Schema(description = "Текущая стадия/этап (строка)")
    private String stage;

    @Schema(description = "Прогресс 0..100")
    private int progress;
}
