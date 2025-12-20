package ru.domium.projectservice.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.domium.projectservice.dto.request.CreateProjectRequest;
import ru.domium.projectservice.dto.response.ProjectImageResponse;
import ru.domium.projectservice.dto.response.ProjectResponse;
import ru.domium.projectservice.entity.Project;
import ru.domium.projectservice.repository.ProjectRepository;
import ru.domium.projectservice.objectstorage.service.ImageS3Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ModelMapper modelMapper;
    private final ProjectRepository projectRepository;
    private final ImageS3Service imageS3Service;

    public ProjectResponse createProject(CreateProjectRequest dto) {
        Project newProject = projectRepository.save(mapToEntity(dto));
        return mapToResponse(newProject);
    }

    @Transactional
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ProjectResponse mapToResponse(Project project) {
        ProjectResponse response = modelMapper.map(project, ProjectResponse.class);
        List<ProjectImageResponse> imageResponses = project.getImages().stream()
                .map(image -> {
                    String objectKey = image.getStorageObjectKey();
                    String url = imageS3Service.getPublicUrlByKey(objectKey);
                    return new ProjectImageResponse(url);
                })
                .toList();
        response.getImageUrls().addAll(imageResponses);
        return response;
    }

    private Project mapToEntity(CreateProjectRequest dto) {
        return modelMapper.map(dto, Project.class);
    }


}

