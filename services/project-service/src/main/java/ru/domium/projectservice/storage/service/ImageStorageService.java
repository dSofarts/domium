package ru.domium.projectservice.storage.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ImageStorageService {

    String uploadProjectImage(UUID projectId, UUID uuid, MultipartFile image);

    void deleteImageByKey(String objectKey);

    String replaceProjectImage(UUID projectId, String oldObjectKey, MultipartFile image, UUID newImageId);

    String getPublicUrlByKey(String objectKey);
}
