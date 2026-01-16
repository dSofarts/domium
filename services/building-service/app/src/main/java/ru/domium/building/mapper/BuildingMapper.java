package ru.domium.building.mapper;

import org.mapstruct.Mapper;
import ru.domium.building.api.dto.BuildingDto;
import ru.domium.building.model.Building;

@Mapper(componentModel = "spring")
public interface BuildingMapper {
    BuildingDto toDto(Building building);
}
