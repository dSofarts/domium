package ru.domium.building.api.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Пользователь")
public class UserDto {
    @Schema(description = "Идентификатор")
    private UUID id;

    @Schema(description = "Имя")
    private String name;
}
