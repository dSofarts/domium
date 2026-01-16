package ru.domium.documentservice.service;

import java.io.InputStream;
import java.net.URL;

public interface FileStorageService {
  String save(String bucket, InputStream stream, long size, String contentType, String filename);
  InputStream load(String bucket, String fileStorageId);
  FileStorageService.FileObject loadWithMetadata(String bucket, String fileStorageId);
  URL generatePresignedUrl(String bucket, String fileStorageId, int ttlSeconds);

  void delete(String bucket, String objectKey);

  record FileObject(InputStream stream, String contentType) {}
}
