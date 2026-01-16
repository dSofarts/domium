package ru.domium.building.api.dto.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Workflow этапов для менеджера")
public class WorkflowDto {
    @Schema(description = "Идентификатор workflow")
    private UUID id;

    @Schema(description = "Идентификатор менеджера-владельца workflow")
    private UUID managerId;

    @Schema(description = "Название workflow")
    private String name;

    @Schema(description = "Флаг активного workflow")
    private boolean active;

    @Schema(description = "Время создания (UTC)")
    private Instant createdAt;

    @Schema(description = "Список стадий workflow")
    private List<StageDto> stages;
}
