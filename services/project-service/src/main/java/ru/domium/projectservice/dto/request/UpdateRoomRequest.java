package ru.domium.projectservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;
import ru.domium.projectservice.entity.RoomType;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class UpdateRoomRequest {

    private UUID id;

    private RoomType type;

    @DecimalMin(value = "0.0", inclusive = false, message = "Площадь комнаты должна быть больше 0")
    private BigDecimal area;
}
