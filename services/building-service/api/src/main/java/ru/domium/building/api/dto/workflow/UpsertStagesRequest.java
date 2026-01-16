package ru.domium.building.api.dto.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Запрос на upsert стадий workflow")
public class UpsertStagesRequest {
    @Schema(description = "Полный список стадий (удалит отсутствующие, если они не используются стройками)")
    private List<StageDto> stages;
}
