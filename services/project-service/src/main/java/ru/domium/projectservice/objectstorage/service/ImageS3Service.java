package ru.domium.projectservice.objectstorage.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.domium.projectservice.exception.ImageStorageException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@Slf4j
public class ImageS3Service implements ImageStorageService {

    private final S3Client s3Client;
    private final String bucketName;
    private final String publicBaseUrl;

    public ImageS3Service(S3Client s3Client,
                          @Value("${minio.bucket}") String bucketName,
                          @Value("${minio.url}") String publicBaseUrl) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public String uploadProjectImage(UUID projectId, UUID imageId, MultipartFile image) {
        String objectKey = buildObjectKey(projectId, imageId);
        log.info("Загрузка изображения проекта в S3: projectId={}, imageId={}, objectKey={}, originalFilename={}, size={}, contentType={}",
                projectId,
                imageId,
                objectKey,
                image != null ? image.getOriginalFilename() : null,
                image != null ? image.getSize() : null,
                image != null ? image.getContentType() : null);

        putImageInStorage(image, objectKey);
        log.debug("Загрузка в S3 завершена: objectKey={}", objectKey);
        return objectKey;
    }

    //    TODO: UNUSED FOR NOW
    @Override
    public String replaceProjectImage(UUID projectId, String oldObjectKey, MultipartFile image, UUID newImageId) {
        String newObjectKey = buildObjectKey(projectId, newImageId);
        log.info("Замена изображения проекта в S3: projectId={}, newImageId={}, newObjectKey={}, oldObjectKey={}, originalFilename={}, size={}, contentType={}",
                projectId,
                newImageId,
                newObjectKey,
                oldObjectKey,
                image != null ? image.getOriginalFilename() : null,
                image != null ? image.getSize() : null,
                image != null ? image.getContentType() : null);

        putImageInStorage(image, newObjectKey);

        if (oldObjectKey != null && !oldObjectKey.isBlank()) {
            deleteImageByKey(oldObjectKey);
        } else {
            log.debug("Старый objectKey не задан — удаление пропущено (oldObjectKey пуст)");
        }

        return newObjectKey;
    }

    @Override
    public void deleteImageByKey(String objectKey) {
        log.info("Удаление изображения из S3: bucketName={}, objectKey={}", bucketName, objectKey);

        try {
            s3Client.deleteObject(builder -> builder.bucket(bucketName).key(objectKey).build());
            log.debug("Удаление из S3 завершено: objectKey={}", objectKey);
        } catch (S3Exception | SdkClientException e) {
            log.error("Ошибка удаления изображения из S3: bucketName={}, objectKey={}", bucketName, objectKey, e);
            throw new ImageStorageException("Couldn't delete image from bucket " + bucketName + ": " + e.getMessage());
        }
    }

    public String getPublicUrlByKey(String objectKey) {
        String url = publicBaseUrl + "/" + bucketName + "/" + objectKey;
        log.debug("Публичная ссылка сформирована: objectKey={}, url={}", objectKey, url);
        return url;
    }

    private String buildObjectKey(UUID projectId, UUID imageId) {
        return projectId + "/images/" + imageId;
    }

    private void putImageInStorage(MultipartFile image, String objectKey) {
        String contentType = image.getContentType() != null ? image.getContentType() : "application/octet-stream";

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .contentType(contentType)
                .key(objectKey)
                .build();

        try {
            byte[] bytes = image.getBytes();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));
        } catch (IOException | S3Exception | SdkClientException e) {
            log.error("Ошибка загрузки изображения в S3: bucketName={}, objectKey={}, originalFilename={}, size={}, contentType={}",
                    bucketName,
                    objectKey,
                    image.getOriginalFilename(),
                    image.getSize(),
                    contentType,
                    e);
            throw new ImageStorageException("Couldn't upload image to bucket " + bucketName + ": " + e.getMessage());
        }
    }
}