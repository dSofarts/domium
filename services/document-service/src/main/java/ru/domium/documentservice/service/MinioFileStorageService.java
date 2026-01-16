package ru.domium.documentservice.service;

import lombok.extern.slf4j.Slf4j;
import ru.domium.documentservice.exception.ApiExceptions;
import io.minio.*;
import io.minio.http.Method;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MinioFileStorageService implements FileStorageService {

  private final MinioClient minio;
  private final String publicUrl;

  public MinioFileStorageService(MinioClient minio,
                                 @Value("${minio.public-url:}") String publicUrl) {
    this.minio = minio;
    this.publicUrl = publicUrl == null ? "" : publicUrl.trim();
  }

  @Override
  public String save(String bucket, InputStream stream, long size, String contentType, String filename) {
    try {
      String objectName = UUID.randomUUID() + "_" + sanitize(filename);
      PutObjectArgs args = PutObjectArgs.builder()
          .bucket(bucket)
          .object(objectName)
          .stream(stream, size, -1)
          .contentType(contentType)
          .build();
      minio.putObject(args);
      return objectName;
    } catch (Exception e) {
      throw ApiExceptions.badRequest("Failed to save file to storage: " + e.getMessage());
    }
  }

  @Override
  public InputStream load(String bucket, String fileStorageId) {
    try {
      return minio.getObject(GetObjectArgs.builder().bucket(bucket).object(fileStorageId).build());
    } catch (Exception e) {
      throw ApiExceptions.notFound("File not found in storage: " + fileStorageId);
    }
  }

  @Override
  public FileObject loadWithMetadata(String bucket, String fileStorageId) {
    try {
      StatObjectResponse stat = minio.statObject(
          StatObjectArgs.builder().bucket(bucket).object(fileStorageId).build()
      );
      String contentType = stat.contentType();
      InputStream stream = minio.getObject(
          GetObjectArgs.builder().bucket(bucket).object(fileStorageId).build()
      );
      return new FileObject(stream, contentType);
    } catch (Exception e) {
      throw ApiExceptions.notFound("File not found in storage: " + fileStorageId);
    }
  }

  @Override
  public URL generatePresignedUrl(String bucket, String fileStorageId, int ttlSeconds) {
    try {
      String url = minio.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .bucket(bucket)
              .object(fileStorageId)
              .method(Method.GET)
              .expiry(ttlSeconds)
              .build());
      URL signedUrl = new URL(url);
      if (publicUrl.isBlank()) {
        return signedUrl;
      }
      URI base = new URI(publicUrl);
      URI rewritten = new URI(
          base.getScheme(),
          base.getUserInfo(),
          base.getHost(),
          base.getPort(),
          signedUrl.getPath(),
          signedUrl.getQuery(),
          null
      );
      return rewritten.toURL();
    } catch (Exception e) {
      throw ApiExceptions.badRequest("Failed to generate presigned url: " + e.getMessage());
    }
  }

  @Override
  public void delete(String bucket, String objectKey) {
    try {
      minio.removeObject(
          RemoveObjectArgs.builder()
              .bucket(bucket)
              .object(objectKey)
              .build()
      );
    } catch (Exception e) {
      log.error(
          "Failed to delete file from MinIO. bucket={}, objectKey={}",
          bucket, objectKey, e
      );
    }
  }

  private String sanitize(String filename) {
    if (filename == null) return "file";
    return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
  }
}
