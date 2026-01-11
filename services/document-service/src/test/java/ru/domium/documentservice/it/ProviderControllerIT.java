package ru.domium.documentservice.it;

import java.util.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import ru.domium.documentservice.dto.DocumentDtos.DocumentInstanceDto;
import ru.domium.documentservice.model.DocumentStatus;

public class ProviderControllerIT extends AbstractDocumentServiceIT {

  @Test
  void manualUpload_success_createsDocumentInDbAndMinio() {
    UUID projectId = UUID.randomUUID();
    UUID stage = UUID.randomUUID();

    DocumentInstanceDto dto = manualUpload(projectId, stage, client.userId(), "IT manual upload");

    Assertions.assertEquals(projectId, dto.projectId());
    Assertions.assertEquals(client.userId(), dto.userId());
    Assertions.assertEquals(stage, dto.stageCode());
    Assertions.assertEquals(DocumentStatus.SENT_TO_USER, dto.status());
    Assertions.assertEquals(1, dto.version());

    Integer cnt = jdbc.queryForObject(
        "select count(*) from document.document_instance where id = ?",
        Integer.class,
        dto.id()
    );
    Assertions.assertEquals(1, cnt);
  }

  @Test
  void manualUpload_missingStageCode_returns400() {
    UUID projectId = UUID.randomUUID();

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", byteArrayResource(samplePdfBytes("missing-stage"), "doc.pdf"));
    body.add("userId", client.userId().toString());

    HttpHeaders headers = bearerHeaders(manager.accessToken());
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    ResponseEntity<String> resp = rest.postForEntity(
        baseUrl("/provider/projects/" + projectId + "/documents/manualUpload"),
        new HttpEntity<>(body, headers),
        String.class
    );

    Assertions.assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
  }

  @Test
  void manualUpload_asClient_forbidden_403() {
    UUID projectId = UUID.randomUUID();
    UUID stage = UUID.randomUUID();

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", byteArrayResource(samplePdfBytes("client-tries"), "doc.pdf"));
    body.add("stageCode", stage.toString());
    body.add("userId", client.userId().toString());

    HttpHeaders headers = bearerHeaders(client.accessToken());
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    ResponseEntity<String> resp = rest.postForEntity(
        baseUrl("/provider/projects/" + projectId + "/documents/manualUpload"),
        new HttpEntity<>(body, headers),
        String.class
    );

    Assertions.assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
  }

  @Test
  void uploadNewVersion_success_incrementsVersionAndStoresNewObject() {
    UUID projectId = UUID.randomUUID();
    UUID stage = UUID.randomUUID();
    DocumentInstanceDto created = manualUpload(projectId, stage, client.userId(), "initial");

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", byteArrayResource(samplePdfBytes("v2"), "v2.pdf"));
    body.add("comment", "new version from IT");

    HttpHeaders headers = bearerHeaders(manager.accessToken());
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    ResponseEntity<DocumentInstanceDto> resp = rest.postForEntity(
        baseUrl("/provider/documents/" + created.id() + "/uploadNewVersion"),
        new HttpEntity<>(body, headers),
        DocumentInstanceDto.class
    );

    Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
    Assertions.assertNotNull(resp.getBody());
    Assertions.assertEquals(2, Objects.requireNonNull(resp.getBody()).version());
    Assertions.assertEquals(DocumentStatus.SENT_TO_USER, resp.getBody().status());

    Integer versionInDb = jdbc.queryForObject(
        "select version from document.document_instance where id = ?",
        Integer.class,
        created.id()
    );
    Assertions.assertEquals(2, versionInDb);

    Integer versionsCount = jdbc.queryForObject(
        "select count(*) from document.document_file_version where document_id = ?",
        Integer.class,
        created.id()
    );
    Assertions.assertEquals(2, versionsCount);

    List<String> fileIds = jdbc.queryForList(
        "select file_storage_id from document.document_file_version where document_id = ? order by version asc",
        String.class,
        created.id()
    );
    Assertions.assertEquals(2, fileIds.size());
    for (String fid : fileIds) {
      try (var is = storage.load("documents", fid)) {
        Assertions.assertTrue(is.readAllBytes().length > 10);
      } catch (Exception e) {
        Assertions.fail("Expected file to exist in MinIO: " + fid + ", err=" + e.getMessage());
      }
    }
  }

  @Test
  void uploadNewVersion_asClient_forbidden_403() {
    UUID projectId = UUID.randomUUID();
    UUID stage = UUID.randomUUID();
    DocumentInstanceDto created = manualUpload(projectId, stage, client.userId(), "initial");

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", byteArrayResource(samplePdfBytes("v2"), "v2.pdf"));

    HttpHeaders headers = bearerHeaders(client.accessToken());
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    ResponseEntity<String> resp = rest.postForEntity(
        baseUrl("/provider/documents/" + created.id() + "/uploadNewVersion"),
        new HttpEntity<>(body, headers),
        String.class
    );

    Assertions.assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
  }

  @Test
  void uploadNewVersion_missingFile_returns400() {
    UUID projectId = UUID.randomUUID();
    UUID stage = UUID.randomUUID();
    DocumentInstanceDto created = manualUpload(projectId, stage, client.userId(), "initial");

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

    HttpHeaders headers = bearerHeaders(manager.accessToken());
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    ResponseEntity<String> resp = rest.postForEntity(
        baseUrl("/provider/documents/" + created.id() + "/uploadNewVersion"),
        new HttpEntity<>(body, headers),
        String.class
    );

    Assertions.assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
  }

  @Test
  void softDelete_success_setsStatusDelete_andNotInListByDefault() {
    UUID projectId = UUID.randomUUID();
    UUID stage = UUID.randomUUID();
    DocumentInstanceDto created = manualUpload(projectId, stage, client.userId(), "to delete");

    String deleteUrl = UriComponentsBuilder
        .fromHttpUrl(baseUrl("/provider/documents/" + created.id()))
        .queryParam("comment", "IT soft delete")
        .toUriString();

    ResponseEntity<Void> delResp = rest.exchange(
        deleteUrl,
        HttpMethod.DELETE,
        new HttpEntity<>(bearerHeaders(manager.accessToken())),
        Void.class
    );

    Assertions.assertEquals(HttpStatus.NO_CONTENT, delResp.getStatusCode());

    String status = jdbc.queryForObject(
        "select status from document.document_instance where id = ?",
        String.class,
        created.id()
    );

    Assertions.assertEquals("DELETE", status);

    // В списках по умолчанию DELETE не должен возвращаться
    ResponseEntity<List> listResp = rest.exchange(
        baseUrl("/projects/" + projectId + "/documents"),
        HttpMethod.GET,
        new HttpEntity<>(bearerHeaders(manager.accessToken())),
        List.class
    );

    Assertions.assertEquals(HttpStatus.OK, listResp.getStatusCode());
    List<?> list = listResp.getBody();
    Assertions.assertNotNull(list);

    String bodyStr = String.valueOf(list);
    Assertions.assertFalse(bodyStr.contains(created.id().toString()), "Deleted doc must not appear in list by default");
  }

  @Test
  void softDelete_asClient_forbidden_403() {
    UUID projectId = UUID.randomUUID();
    UUID stage = UUID.randomUUID();
    DocumentInstanceDto created = manualUpload(projectId, stage, client.userId(), "to delete");

    ResponseEntity<String> delResp = rest.exchange(
        baseUrl("/provider/documents/" + created.id()),
        HttpMethod.DELETE,
        new HttpEntity<>(bearerHeaders(client.accessToken())),
        String.class
    );

    Assertions.assertEquals(HttpStatus.FORBIDDEN, delResp.getStatusCode());
  }
}
