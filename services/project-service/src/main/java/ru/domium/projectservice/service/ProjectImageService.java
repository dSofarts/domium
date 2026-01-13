package ru.domium.projectservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.domium.projectservice.dto.response.ProjectImageResponse;
import ru.domium.projectservice.entity.Project;
import ru.domium.projectservice.entity.ProjectImage;
import ru.domium.projectservice.entity.ProjectPublicationStatus;
import ru.domium.projectservice.event.ProjectDeletedEvent;
import ru.domium.projectservice.exception.NotAccessException;
import ru.domium.projectservice.exception.NotFoundException;
import ru.domium.projectservice.objectstorage.service.ImageS3Service;
import ru.domium.projectservice.repository.ProjectImageRepository;
import ru.domium.projectservice.repository.ProjectRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectImageService {

    private final ProjectImageRepository projectImageRepository;
    private final ProjectRepository projectRepository;
    private final ImageS3Service imageS3Service;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public List<ProjectImageResponse> addImagesToProject(UUID projectId, List<MultipartFile> images, UUID managerId) {
        int count = images == null ? 0 : images.size();
        log.info("Добавление изображений в проект: projectId={}, managerId={}, imagesCount={}", projectId, managerId, count);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> NotFoundException.projectNotFound(projectId));

        if (!project.getManagerUserId().equals(managerId)) {
            throw new NotAccessException(managerId, projectId);
        }

        List<ProjectImage> savedImages = new ArrayList<>();

        try {
            for (int i = 0; i < count; i++) {
                MultipartFile file = images.get(i);
                log.debug("Загрузка изображения: projectId={}, managerId={}, position={}, originalFilename={}, size={}",
                        projectId, managerId, i, file != null ? file.getOriginalFilename() : null, file != null ? file.getSize() : null);

                ProjectImage projectImage = addImage(project, file, i);
                savedImages.add(projectImage);
            }

            project.setPublicationStatus(ProjectPublicationStatus.PUBLISHED);
            projectRepository.save(project);
            log.info("Проект обновлен после добавления изображений: projectId={}, imagesCount={}", projectId, savedImages.size());

            return savedImages.stream()
                    .map(image -> new ProjectImageResponse(
                            image.getId(),
                            imageS3Service.getPublicUrlByKey(image.getStorageObjectKey()),
                            image.getPosition()
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Ошибка при добавлении изображений в проект, выставляется статус PUBLISH_FAILED: projectId={}, managerId={}, savedCount={}",
                    projectId, managerId, savedImages.size(), e);

            project.setPublicationStatus(ProjectPublicationStatus.PUBLISH_FAILED);
            projectRepository.save(project);
            throw e;
        }
    }

    private ProjectImage addImage(Project project, MultipartFile image, int position) {
        UUID imageId = UUID.randomUUID();

        String objectKey = imageS3Service.uploadProjectImage(project.getId(), imageId, image);

        ProjectImage projectImage = ProjectImage.builder()
                .id(imageId)
                .storageObjectKey(objectKey)
                .project(project)
                .position(position)
                .build();

        saveToDb(projectImage);

        log.info("Изображение добавлено: projectId={}, imageId={}, position={}, objectKey={}",
                project.getId(), projectImage.getId(), projectImage.getPosition(), projectImage.getStorageObjectKey());
        return projectImage;
    }

    private void saveToDb(ProjectImage image) {
        try {
            projectImageRepository.save(image);
        } catch (Exception e) {
            log.error("Ошибка сохранения ProjectImage в БД, начинается событие очистки в ObjectStorage: projectId={}, imageId={}, objectKey={}",
                    image.getProject() != null ? image.getProject().getId() : null,
                    image.getId(),
                    image.getStorageObjectKey(),
                    e
            );

            eventPublisher.publishEvent(
                    new ProjectDeletedEvent(image.getProject().getId(),
                            List.of(image.getStorageObjectKey()))
            );
            throw e;
        }
    }

    @Transactional
    public void deleteProjectImage(UUID projectId, UUID imageId, UUID managerId) {
        log.info("Удаление изображения проекта: projectId={}, imageId={}, managerId={}", projectId, imageId, managerId);

        ProjectImage image = projectImageRepository.findByIdAndProject_Id(imageId, projectId)
                .orElseThrow(() -> NotFoundException.projectImageNotFound(imageId, projectId));

        if (!image.getProject().getManagerUserId().equals(managerId)) {
            log.warn("Отказ в доступе при удалении изображения: projectId={}, imageId={}, managerId={}, projectManagerId={}",
                    projectId, imageId, managerId, image.getProject().getManagerUserId());
            throw new NotAccessException(managerId, projectId);
        }

        projectImageRepository.delete(image);
        log.info("Изображение удалено из БД: projectId={}, imageId={}, objectKey={}", projectId, imageId, image.getStorageObjectKey());

        eventPublisher.publishEvent(new ProjectDeletedEvent(projectId,
                List.of(image.getStorageObjectKey())));
        log.debug("Опубликовано событие очистки изображений: projectId={}, keysCount=1", projectId);
    }

    @Transactional
    public void deleteProjectImages(UUID projectId, List<UUID> imageIds, UUID managerId) {
        int count = imageIds == null ? 0 : imageIds.size();
        log.info("Массовое удаление изображений проекта: projectId={}, managerId={}, imageIdsCount={}", projectId, managerId, count);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> NotFoundException.projectNotFound(projectId));

        if (!project.getManagerUserId().equals(managerId)) {
            log.warn("Отказ в доступе при массовом удалении изображений: projectId={}, managerId={}, projectManagerId={}",
                    projectId, managerId, project.getManagerUserId());
            throw new NotAccessException(managerId, projectId);
        }

        List<ProjectImage> images = projectImageRepository.findAllByIdInAndProject_Id(imageIds, projectId);

        List<String> imageKeys = images.stream()
                .map(ProjectImage::getStorageObjectKey)
                .toList();

        projectImageRepository.deleteAll(images);
        log.info("Изображения удалены из БД: projectId={}, deletedCount={}", projectId, images.size());

        eventPublisher.publishEvent(new ProjectDeletedEvent(projectId, imageKeys));
        log.debug("Опубликовано событие очистки изображений: projectId={}, keysCount={}", projectId, imageKeys.size());
    }
}
