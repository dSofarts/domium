package ru.domium.building.api.dto.transition;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Описание нарушения/причины, по которой переход этапа запрещён")
public class TransitionViolationDto {
    @Schema(description = "Код причины", example = "PAYMENT_REQUIRED")
    private String code;

    @Schema(description = "Описание", example = "Не выполнена оплата по договору")
    private String message;
}
