package ru.domium.building.api.dto.transition;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Ответ при запрете перехода этапа (HTTP 409)")
public class StageTransitionDeniedDto {
    @Schema(description = "Идентификатор стройки", example = "8f1d6d18-1bfb-4a8a-95c9-6bfe0c2e8a0b")
    private UUID buildingId;

    @Schema(description = "Список нарушений (причин запрета)")
    private List<TransitionViolationDto> violations = new ArrayList<>();
}
