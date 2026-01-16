package ru.domium.building.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.domium.building.api.dto.workflow.StageDto;
import ru.domium.building.model.Stage;

@Mapper(componentModel = "spring")
public interface StageUpdateMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workflowId", ignore = true)
    @Mapping(target = "name", expression = "java(dto.getName() != null ? dto.getName().trim() : null)")
    void updateFromDto(StageDto dto, @MappingTarget Stage stage);
}
