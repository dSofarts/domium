package ru.domium.documentservice.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import ru.domium.documentservice.dto.DocumentDtos.DocumentInstanceDto;
import ru.domium.documentservice.service.DocumentWorkflowService;
import ru.domium.documentservice.service.FileStorageService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@TestPropertySource(properties = {
    "spring.cloud.consul.enabled=false",
    "spring.cloud.consul.discovery.enabled=false",
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://keycloak:8080/realms/domium",
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractDocumentServiceIT {

  protected static final String KEYCLOAK_CLIENT_ID = "domium";
  protected static final String KEYCLOAK_CLIENT_SECRET = "FaxzBgk7pkyattBrV8MlVCVg80jjZKo5";

  protected static final String USER_MANAGER = "test-manager";
  protected static final String PASS_MANAGER = "test-manager";

  protected static final String USER_CLIENT = "test-client";
  protected static final String PASS_CLIENT = "test-client";

  @LocalServerPort
  protected int port;

  @Autowired
  protected TestRestTemplate rest;

  @Autowired
  protected JdbcTemplate jdbc;

  @Autowired
  protected FileStorageService storage;

  @Autowired
  protected ObjectMapper om;

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String issuerUri;

  protected KeycloakTokenClient.TokenInfo manager;
  protected KeycloakTokenClient.TokenInfo client;

  protected final List<UUID> createdDocumentIds = new CopyOnWriteArrayList<>();

  @BeforeAll
  void initTokens() {
    KeycloakTokenClient kc = new KeycloakTokenClient(om, issuerUri, KEYCLOAK_CLIENT_ID, KEYCLOAK_CLIENT_SECRET);
    manager = kc.passwordGrant(USER_MANAGER, PASS_MANAGER);
    client = kc.passwordGrant(USER_CLIENT, PASS_CLIENT);

    Assertions.assertTrue(manager.realmRoles().contains("manager"), "test-manager must have realm role 'manager'");
    Assertions.assertTrue(client.realmRoles().contains("client"), "test-client must have realm role 'client'");
  }

  @AfterEach
  void cleanupCreatedData() {
    for (UUID docId : createdDocumentIds) {
      List<String> fileIds = jdbc.queryForList(
          "select file_storage_id from document.document_file_version where document_id = ?",
          String.class,
          docId
      );

      jdbc.update("delete from document.document_instance where id = ?", docId);

      for (String fileId : fileIds) {
        storage.delete(DocumentWorkflowService.BUCKET_DOCUMENTS, fileId);
      }

      assertDocFullyDeleted(docId);
    }

    createdDocumentIds.clear();
  }

  protected String baseUrl(String path) {
    return "http://localhost:" + port + path;
  }

  protected HttpHeaders bearerHeaders(String token) {
    HttpHeaders h = new HttpHeaders();
    h.setBearerAuth(token);
    h.setAccept(List.of(MediaType.APPLICATION_JSON));
    return h;
  }

  protected static byte[] samplePdfBytes(String marker) {
    String s = "%PDF-1.4\n" +
        "% domium-it " + marker + "\n" +
        "1 0 obj\n<< /Type /Catalog >>\nendobj\n" +
        "trailer\n<<>>\n%%EOF\n";
    return s.getBytes(StandardCharsets.UTF_8);
  }

  protected DocumentInstanceDto manualUpload(UUID projectId, UUID stageCode, UUID userId, String title) {
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", byteArrayResource(samplePdfBytes("manual"), "doc.pdf"));
    body.add("stageCode", stageCode.toString());
    body.add("userId", userId.toString());
    if (title != null) body.add("title", title);

    HttpHeaders headers = bearerHeaders(manager.accessToken());
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(body, headers);

    ResponseEntity<DocumentInstanceDto> resp = rest.postForEntity(
        baseUrl("/provider/projects/" + projectId + "/documents/manualUpload"),
        req,
        DocumentInstanceDto.class
    );

    Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode(), "manualUpload must return 200");
    Assertions.assertNotNull(resp.getBody(), "manualUpload must return body");

    UUID docId = Objects.requireNonNull(resp.getBody()).id();
    createdDocumentIds.add(docId);

    String currentFileId = jdbc.queryForObject(
        "select current_file_storage_id from document.document_instance where id = ?",
        String.class,
        docId
    );
    Assertions.assertNotNull(currentFileId);
    try (var is = storage.load(DocumentWorkflowService.BUCKET_DOCUMENTS, currentFileId)) {
      byte[] bytes = is.readAllBytes();
      Assertions.assertTrue(bytes.length > 10, "stored file must not be empty");
    } catch (Exception e) {
      Assertions.fail("Expected file to exist in MinIO, but load failed: " + e.getMessage());
    }

    return resp.getBody();
  }

  protected static org.springframework.core.io.ByteArrayResource byteArrayResource(byte[] bytes, String filename) {
    return new org.springframework.core.io.ByteArrayResource(bytes) {
      @Override
      public String getFilename() {
        return filename;
      }
    };
  }

  protected void assertDocFullyDeleted(UUID docId) {
    Integer docCnt = jdbc.queryForObject(
        "select count(*) from document.document_instance where id = ?",
        Integer.class,
        docId
    );
    Integer verCnt = jdbc.queryForObject(
        "select count(*) from document.document_file_version where document_id = ?",
        Integer.class,
        docId
    );
    Integer comCnt = jdbc.queryForObject(
        "select count(*) from document.document_comment where document_id = ?",
        Integer.class,
        docId
    );
    Integer sigCnt = jdbc.queryForObject(
        "select count(*) from document.document_signature where document_id = ?",
        Integer.class,
        docId
    );
    Integer audCnt = jdbc.queryForObject(
        "select count(*) from document.document_audit_log where document_id = ?",
        Integer.class,
        docId
    );

    Assertions.assertEquals(0, docCnt);
    Assertions.assertEquals(0, verCnt);
    Assertions.assertEquals(0, comCnt);
    Assertions.assertEquals(0, sigCnt);
    Assertions.assertEquals(0, audCnt);
  }

  protected static <T> ResponseEntity<T> exchange(TestRestTemplate rest,
      HttpMethod method,
      String url,
      HttpEntity<?> req,
      Class<T> responseType) {
    return rest.exchange(url, method, req, responseType);
  }

  protected static UriComponentsBuilder uri(String baseUrl) {
    return UriComponentsBuilder.fromHttpUrl(baseUrl);
  }

  protected static Duration timeout10s() {
    return Duration.ofSeconds(10);
  }
}
