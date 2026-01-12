package ru.domium.projectservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.domium.projectservice.dto.request.CreateProjectRequest;
import ru.domium.projectservice.dto.request.UpdateProjectRequest;
import ru.domium.projectservice.dto.response.ProjectResponse;
import ru.domium.projectservice.entity.Project;
import ru.domium.projectservice.entity.ProjectImage;
import ru.domium.projectservice.entity.ProjectPublicationStatus;
import ru.domium.projectservice.exception.NotAccessException;
import ru.domium.projectservice.exception.NotFoundException;
import ru.domium.projectservice.event.ProjectDeletedEvent;
import ru.domium.projectservice.mapper.ProjectMapper;
import ru.domium.projectservice.objectstorage.service.ImageS3Service;
import ru.domium.projectservice.repository.ProjectRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectRepository projectRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ImageS3Service imageS3Service;

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest dto, UUID projectCreatorId) {
        Project project = projectMapper.createDtoToEntity(dto);
        project.setManagerUserId(projectCreatorId);
        project.setPublicationStatus(ProjectPublicationStatus.DRAFT);
        Project saved = projectRepository.save(project);
        return projectMapper.mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(projectMapper::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(UUID projectId) {
        return projectRepository.findById(projectId)
                .map(projectMapper::mapToResponse)
                .orElseThrow(() -> NotFoundException.projectNotFound(projectId));
    }

    @Transactional
    public void updateProject(UUID projectId, UpdateProjectRequest dto, UUID managerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> NotFoundException.projectNotFound(projectId));

        if (!project.getManagerUserId().equals(managerId)) {
            throw new NotAccessException(managerId, projectId);
        }
        projectMapper.updateEntityFromDto(dto, project);
        projectRepository.save(project);
    }

    @Transactional
    public void deleteProject(UUID projectId, UUID managerId) {
        Project project = projectRepository.findByIdWithImages(projectId)
                .orElseThrow(() -> NotFoundException.projectNotFound(projectId));

        if (!project.getManagerUserId().equals(managerId)) {
            throw new NotAccessException(managerId, projectId);
        }

        List<String> imageKeys = project.getImages().stream()
                .map(ProjectImage::getStorageObjectKey)
                .toList();

        projectRepository.delete(project);
        eventPublisher.publishEvent(new ProjectDeletedEvent(projectId, imageKeys));
    }
}

