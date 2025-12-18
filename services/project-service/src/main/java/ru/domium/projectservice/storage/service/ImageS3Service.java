package ru.domium.projectservice.storage.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.domium.projectservice.exception.ImageStorageException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class ImageS3Service implements ImageStorageService {

    private final S3Client s3Client;
    private final String bucketName;
    private final String publicBaseUrl;

    public ImageS3Service(S3Client s3Client,
                          @Value("${minio.bucket}") String bucketName,
                          @Value("${minio.endpoint}") String publicBaseUrl) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.publicBaseUrl = publicBaseUrl;
    }

    public String uploadProjectImage(UUID projectId, UUID imageId, MultipartFile image) {
        String objectKey = buildObjectKey(projectId, imageId);
        putImageInStorage(image, objectKey);
        return objectKey;
    }

//    TODO: UNUSED FOR NOW
    public String replaceProjectImage(UUID projectId, String oldObjectKey, MultipartFile image, UUID newImageId) {
        String newObjectKey = buildObjectKey(projectId, newImageId);
        putImageInStorage(image, newObjectKey);

        if (oldObjectKey != null && !oldObjectKey.isBlank()) {
            deleteImageByKey(oldObjectKey);
        }

        return newObjectKey;
    }

    public void deleteImageByKey(String objectKey) {
        try {
            s3Client.deleteObject(builder -> builder.bucket(bucketName).key(objectKey).build());
        } catch (S3Exception | SdkClientException e) {
            throw new ImageStorageException("Couldn't delete image from bucket " + bucketName + ": " + e.getMessage());
        }
    }

    public String getPublicUrlByKey(String objectKey) {
        return publicBaseUrl + "/" + bucketName + "/" + objectKey;
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

        try (InputStream inputStream = image.getInputStream()) {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, image.getSize()));
        } catch (IOException | S3Exception | SdkClientException e) {
            throw new ImageStorageException("Couldn't upload image to bucket " + bucketName + ": " + e.getMessage());
        }
    }

    /**
     * Just in case.
     * Downloads an image from S3 by its key via service.
     */
    public byte[] downloadImageByKey(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        try (ResponseInputStream<GetObjectResponse> inputStream = s3Client.getObject(getObjectRequest)) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new ImageStorageException("Couldn't download image from bucket " + bucketName + ": " + e.getMessage());
        }
    }
}