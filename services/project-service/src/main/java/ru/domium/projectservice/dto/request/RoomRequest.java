package ru.domium.projectservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import ru.domium.projectservice.entity.RoomType;

import java.math.BigDecimal;

@Getter
@Setter
public class RoomRequest {

    @NotNull
    private RoomType roomType;
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal area;
}
