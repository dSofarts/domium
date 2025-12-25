package ru.domium.building.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.domium.building.api.dto.substage.SubStageStatusDto;
import ru.domium.building.model.Task;

@Mapper(componentModel = "spring")
public interface SubStageStatusMapper {
    @Mapping(target = "completed", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "workItems", ignore = true)
    SubStageStatusDto toDto(Task subStageTask);
}
