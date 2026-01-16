package ru.domium.documentservice.service;

import io.minio.GetObjectResponse;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class MinioFileStorageServiceTest {

  @Mock private MinioClient minio;

  @InjectMocks private MinioFileStorageService service;

  // UUID(36 chars) + "_" + sanitized filename
  private static final Pattern OBJECT_NAME_PATTERN = Pattern.compile(
      "^[0-9a-fA-F-]{36}_.+$"
  );

  // -------------------- save --------------------

  @Test
  void save_shouldPutObject_andReturnGeneratedObjectName_withSanitizedFilename() throws Exception {
    String bucket = "bucket-docs";
    String filename = "my file(1)?!.pdf";
    String contentType = "application/pdf";
    byte[] data = "hello".getBytes();

    InputStream in = new ByteArrayInputStream(data);

    ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);

    String objectName = service.save(bucket, in, data.length, contentType, filename);

    assertNotNull(objectName);
    assertTrue(OBJECT_NAME_PATTERN.matcher(objectName).matches(), "objectName must contain UUID prefix");
    assertTrue(objectName.endsWith("_my_file_1___.pdf"),
        "objectName must end with sanitized filename (non [a-zA-Z0-9._-] -> _)");

    verify(minio).putObject(captor.capture());

    PutObjectArgs args = captor.getValue();
    // максимально возможно проверяем содержимое args без привязки к версии minio-sdk
    assertEquals(bucket, readStringArg(args, "bucket", "bucketName", "bucket"));
    assertEquals(objectName, readStringArg(args, "object", "objectName", "object"));
    assertEquals(contentType, readStringArg(args, "contentType", "contentType"));
  }

  @Test
  void save_shouldUseDefaultFilename_whenFilenameIsNull() throws Exception {
    String bucket = "bucket-docs";
    byte[] data = "hello".getBytes();
    InputStream in = new ByteArrayInputStream(data);

    String objectName = service.save(bucket, in, data.length, "text/plain", null);

    assertNotNull(objectName);
    assertTrue(objectName.endsWith("_file"), "when filename is null, sanitize() must return 'file'");

    verify(minio).putObject(any(PutObjectArgs.class));
  }

  @Test
  void save_shouldWrapAnyExceptionIntoBadRequest_withOriginalMessage() throws Exception {
    String bucket = "bucket-docs";
    byte[] data = "hello".getBytes();
    InputStream in = new ByteArrayInputStream(data);

    doThrow(new RuntimeException("minio down")).when(minio).putObject(any(PutObjectArgs.class));

    ResponseStatusException ex = assertThrows(
        ResponseStatusException.class,
        () -> service.save(bucket, in, data.length, "application/pdf", "a.pdf")
    );

    assertEquals(BAD_REQUEST, ex.getStatusCode());
    assertNotNull(ex.getReason());
    assertTrue(ex.getReason().startsWith("Failed to save file to storage: "));
    assertTrue(ex.getReason().contains("minio down"));
  }

  // -------------------- load --------------------

  @Test
  void load_shouldReturnStream_whenMinioReturnsObject() throws Exception {
    String bucket = "bucket-docs";
    String key = "abc.pdf";

    GetObjectResponse expected = mock(GetObjectResponse.class);

    when(minio.getObject(any(GetObjectArgs.class))).thenReturn(expected);

    InputStream actual = service.load(bucket, key);

    assertSame(expected, actual);
    verify(minio).getObject(any(GetObjectArgs.class));
  }

  @Test
  void load_shouldWrapAnyExceptionIntoNotFound_andIncludeKey() throws Exception {
    String bucket = "bucket-docs";
    String key = "missing.pdf";

    when(minio.getObject(any(GetObjectArgs.class))).thenThrow(new RuntimeException("404"));

    ResponseStatusException ex = assertThrows(
        ResponseStatusException.class,
        () -> service.load(bucket, key)
    );

    assertEquals(NOT_FOUND, ex.getStatusCode());
    assertEquals("File not found in storage: " + key, ex.getReason());
  }

  // -------------------- delete --------------------

  @Test
  void delete_shouldCallRemoveObject() throws Exception {
    String bucket = "bucket-docs";
    String key = "to-delete.pdf";

    assertDoesNotThrow(() -> service.delete(bucket, key));

    ArgumentCaptor<RemoveObjectArgs> captor = ArgumentCaptor.forClass(RemoveObjectArgs.class);
    verify(minio).removeObject(captor.capture());

    RemoveObjectArgs args = captor.getValue();
    assertEquals(bucket, readStringArg(args, "bucket", "bucketName", "bucket"));
    assertEquals(key, readStringArg(args, "object", "objectName", "object"));
  }

  @Test
  void delete_shouldNotThrow_whenMinioThrowsBecauseDeleteIsCompensating() throws Exception {
    doThrow(new RuntimeException("remove failed")).when(minio).removeObject(any(RemoveObjectArgs.class));

    assertDoesNotThrow(() -> service.delete("bucket", "key"));

    verify(minio).removeObject(any(RemoveObjectArgs.class));
  }

  /**
   * Пытается достать строковое поле/свойство из minio-args.
   * Делает тесты устойчивее к различиям версий SDK (bucketName vs bucket, objectName vs object).
   */
  private static String readStringArg(Object args, String... candidates) {
    // 1) пробуем методы вида bucket(), object(), contentType()
    for (String c : candidates) {
      try {
        var m = args.getClass().getMethod(c);
        Object v = m.invoke(args);
        if (v instanceof String s) return s;
      } catch (Exception ignored) {}
    }
    // 2) пробуем поля с разными именами
    for (String c : candidates) {
      try {
        Field f = findField(args.getClass(), c);
        if (f != null) {
          f.setAccessible(true);
          Object v = f.get(args);
          if (v instanceof String s) return s;
        }
      } catch (Exception ignored) {}
    }
    // 3) fallback: если не нашли — пусть тест явно упадёт
    fail("Unable to read any of candidates " + String.join(",", candidates) + " from " + args.getClass());
    return null;
  }

  private static Field findField(Class<?> type, String name) {
    Class<?> cur = type;
    while (cur != null && cur != Object.class) {
      try {
        return cur.getDeclaredField(name);
      } catch (NoSuchFieldException ignored) {
        cur = cur.getSuperclass();
      }
    }
    return null;
  }
}
