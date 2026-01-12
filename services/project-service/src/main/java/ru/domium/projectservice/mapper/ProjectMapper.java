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
        if (!dto.getName().equals(project.getName())) {
            project.setName(dto.getName());
        }
        if (!dto.getType().equals(project.getType())) {
            project.setType(dto.getType());
        }
        if (!dto.getCategory().equals(project.getCategory())) {
            project.setCategory(dto.getCategory());
        }
        if (!dto.getPrice().equals(project.getPrice())) {
            project.setPrice(dto.getPrice());
        }
        if (!dto.getMaterial().equals(project.getMaterial())) {
            project.setMaterial(dto.getMaterial());
        }
        if (!dto.getLocation().equals(project.getLocation())) {
            project.setLocation(dto.getLocation());
        }
        if (!dto.getDescription().equals(project.getDescription())) {
            project.setDescription(dto.getDescription());
        }
        if (dto.getFloors() == null || dto.getFloors().isEmpty()) {
            project.getFloors().clear();
        } else {
            project.getFloors().clear();
            for (var floorDto : dto.getFloors()) {
                Floor floor = modelMapper.map(floorDto, Floor.class);
                floor.setProject(project);
                floor.getRooms().forEach(room -> room.setFloor(floor));
                project.getFloors().add(floor);
            }
        }
    }
}
