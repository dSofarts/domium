package ru.domium.projectservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import ru.domium.projectservice.entity.RoomType;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateRoomRequest {

    @NotNull(message = "Тип комнаты обязателен")
    private RoomType type;

    @DecimalMin(value = "0.0", inclusive = false, message = "Площадь комнаты должна быть больше 0")
    @NotNull(message = "Площадь комнаты обязательна")
    private BigDecimal area;
}
