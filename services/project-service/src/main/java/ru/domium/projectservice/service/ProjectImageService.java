package ru.domium.projectservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.domium.projectservice.dto.response.ProjectImageResponse;
import ru.domium.projectservice.entity.Project;
import ru.domium.projectservice.entity.ProjectImage;
import ru.domium.projectservice.entity.ProjectPublicationStatus;
import ru.domium.projectservice.exception.NotFoundException;
import ru.domium.projectservice.repository.ProjectImageRepository;
import ru.domium.projectservice.repository.ProjectRepository;
import ru.domium.projectservice.objectstorage.service.ImageS3Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectImageService {

    private final ProjectImageRepository projectImageRepository;
    private final ProjectRepository projectRepository;
    private final ImageS3Service imageS3Service;

    @Transactional
    public List<ProjectImageResponse> addImagesToProject(UUID projectId, List<MultipartFile> images) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> NotFoundException.projectNotFound(projectId));

        List<String> imageUrls = new ArrayList<>();

        try {
            for (int i = 0; i < images.size(); i++) {
                String objectKey = addImage(project, images.get(i), i);
                String url = imageS3Service.getPublicUrlByKey(objectKey);
                imageUrls.add(url);
            }

            project.setPublicationStatus(ProjectPublicationStatus.PUBLISHED);
            projectRepository.save(project);

            return imageUrls.stream()
                    .map(ProjectImageResponse::new)
                    .toList();
        } catch (Exception e) {
            project.setPublicationStatus(ProjectPublicationStatus.PUBLISH_FAILED);
            projectRepository.save(project);
            throw e;
        }
    }

    private String addImage(Project project, MultipartFile image, int position) {
        UUID imageId = UUID.randomUUID();
        String objectKey = imageS3Service.uploadProjectImage(project.getId(), imageId, image);

        ProjectImage projectImage = ProjectImage.builder()
                .id(imageId)
                .storageObjectKey(objectKey)
                .project(project)
                .position(position)
                .build();

        saveToDb(projectImage);
        return objectKey;
    }

    private void saveToDb(ProjectImage image) {
        try {
            projectImageRepository.save(image);
        } catch (Exception e) {
            imageS3Service.deleteImageByKey(image.getStorageObjectKey());
            throw e;
        }
    }
}

