package ru.domium.projectservice.mapper;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import ru.domium.projectservice.dto.request.CreateProjectRequest;
import ru.domium.projectservice.dto.request.UpdateProjectRequest;
import ru.domium.projectservice.dto.response.ProjectImageResponse;
import ru.domium.projectservice.dto.response.ProjectResponse;
import ru.domium.projectservice.entity.Floor;
import ru.domium.projectservice.entity.Project;
import ru.domium.projectservice.entity.ProjectImage;
import ru.domium.projectservice.objectstorage.service.ImageS3Service;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ProjectMapper {

    private final ModelMapper modelMapper;
    private final ImageS3Service imageS3Service;

    public ProjectResponse mapToResponse(Project project) {
        ProjectResponse response = modelMapper.map(project, ProjectResponse.class);
        List<ProjectImageResponse> imageResponses = (project.getImages() == null ?
                List.<ProjectImage>of() : project.getImages())
                .stream()
                .map(image -> {
                    String objectKey = image.getStorageObjectKey();
                    String publicUrl = imageS3Service.getPublicUrlByKey(objectKey);
                    return ProjectImageResponse.builder()
                            .id(image.getId())
                            .imageUrl(publicUrl)
                            .position(image.getPosition())
                            .build();
                })
                .toList();
        response.getImageUrls().addAll(imageResponses);
        return response;
    }

    public Project createDtoToEntity(CreateProjectRequest dto) {
        Project project = modelMapper.map(dto, Project.class);
        if (project.getFloors() != null) {
            for (Floor floor : project.getFloors()) {
                floor.setProject(project);
                floor.getRooms().forEach(room -> room.setFloor(floor));
            }
        }
        return project;
    }

    public void updateEntityFromDto(UpdateProjectRequest dto, Project project) {
        if (dto == null) {
            return;
        }
        Objects.requireNonNull(project, "project must not be null");

        setIfPresentChanged(dto.getName(), project.getName(), project::setName);
        setIfPresentChanged(dto.getType(), project.getType(), project::setType);
        setIfPresentChanged(dto.getCategory(), project.getCategory(), project::setCategory);
        setIfPresentChanged(dto.getPrice(), project.getPrice(), project::setPrice);
        setIfPresentChanged(dto.getMaterial(), project.getMaterial(), project::setMaterial);
        setIfPresentChanged(dto.getLocation(), project.getLocation(), project::setLocation);
        setIfPresentChanged(dto.getDescription(), project.getDescription(), project::setDescription);

        updateFloors(dto, project);
    }

    private void updateFloors(UpdateProjectRequest dto, Project project) {
        if (dto.getFloors() == null) {
            return;
        }

        if (project.getFloors() != null) {
            project.getFloors().clear();
        }

        if (dto.getFloors().isEmpty()) {
            return;
        }

        for (var floorDto : dto.getFloors()) {
            Floor floor = modelMapper.map(floorDto, Floor.class);
            floor.setProject(project);
            if (floor.getRooms() != null) {
                floor.getRooms().forEach(room -> room.setFloor(floor));
            }
            project.getFloors().add(floor);
        }
    }

    private <T> void setIfPresentChanged(T newValue, T oldValue, java.util.function.Consumer<T> setter) {
        if (newValue == null || Objects.equals(newValue, oldValue)) {
            return;
        }
        setter.accept(newValue);
    }
}
