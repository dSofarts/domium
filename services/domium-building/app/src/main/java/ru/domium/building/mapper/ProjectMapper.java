package ru.domium.building.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.domium.building.api.dto.project.ProjectDto;
import ru.domium.building.model.BuildingProjection;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(target = "buildingId", source = "id")
    @Mapping(target = "id", source = "projectId")
    @Mapping(target = "manager.id", source = "managerId")
    @Mapping(target = "manager.name", source = "managerName")
    @Mapping(target = "objectInfo.projectName", source = "projectName")
    @Mapping(target = "objectInfo.attributes", source = "metadata")
    @Mapping(target = "objectInfo.stage", source = "stage")
    @Mapping(target = "objectInfo.progress", source = "progress")
    ProjectDto toProjectDto(BuildingProjection projection);
}
