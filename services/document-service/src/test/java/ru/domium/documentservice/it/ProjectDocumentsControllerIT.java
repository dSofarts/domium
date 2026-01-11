package ru.domium.documentservice.it;

import java.util.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;
import ru.domium.documentservice.dto.DocumentDtos.DocumentInstanceDto;
import ru.domium.documentservice.dto.DocumentDtos.RejectRequest;
import ru.domium.documentservice.model.DocumentStatus;

public class ProjectDocumentsControllerIT extends AbstractDocumentServiceIT {

  private static final ParameterizedTypeReference<List<DocumentInstanceDto>> LIST_DOC_DTO =
      new ParameterizedTypeReference<>() {};

  @Test
  void list_asManager_returnsAllDocsInProject() {
    UUID projectId = UUID.randomUUID();
    UUID stage = UUID.randomUUID();

    var d1 = manualUpload(projectId, stage, client.userId(), "d1");
    var d2 = manualUpload(projectId, stage, UUID.randomUUID(), "d2-other-user");

    ResponseEntity<List<DocumentInstanceDto>> resp = rest.exchange(
        baseUrl("/projects/" + projectId + "/documents"),
        HttpMethod.GET,
        new HttpEntity<>(bearerHeaders(manager.accessToken())),
        LIST_DOC_DTO
    );

    Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
    List<DocumentInstanceDto> list = resp.getBody();
    Assertions.assertNotNull(list);
    Assertions.assertEquals(2, list.size());
  }

  @Test
  void list_asClient_returnsOnlyOwnDocs() {
    UUID projectId = UUID.randomUUID();
    UUID stage = UUID.randomUUID();

    var mine = manualUpload(projectId, stage, client.userId(), "mine");
    var other = manualUpload(projectId, stage, UUID.randomUUID(), "other");

    ResponseEntity<List<DocumentInstanceDto>> resp = rest.exchange(
        baseUrl("/projects/" + projectId + "/documents"),
        HttpMethod.GET,
        new HttpEntity<>(bearerHeaders(client.accessToken())),
        LIST_DOC_DTO
    );

    Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
    List<DocumentInstanceDto> list = resp.getBody();
    Assertions.assertNotNull(list);
    Assertions.assertEquals(1, list.size());
    Assertions.assertEquals(mine.id(), list.get(0).id());
  }

  @Test
  void list_statusFilter_returnsOnlyMatching() {
    UUID projectId = UUID.randomUUID();
    UUID stage = UUID.randomUUID();

    var d1 = manualUpload(projectId, stage, client.userId(), "will reject");
    var d2 = manualUpload(projectId, stage, client.userId(), "stay sent");

    HttpHeaders headers = bearerHeaders(client.accessToken());
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Void> rej = rest.exchange(
        baseUrl("/" + d1.id() + "/reject"),
        HttpMethod.POST,
        new HttpEntity<>(new RejectRequest("no"), headers),
        Void.class
    );
    Assertions.assertEquals(HttpStatus.NO_CONTENT, rej.getStatusCode());

    String url = UriComponentsBuilder.fromHttpUrl(baseUrl("/projects/" + projectId + "/documents"))
        .queryParam("status", DocumentStatus.REJECTED)
        .toUriString();

    ResponseEntity<List<DocumentInstanceDto>> resp = rest.exchange(
        url,
        HttpMethod.GET,
        new HttpEntity<>(bearerHeaders(manager.accessToken())),
        LIST_DOC_DTO
    );

    Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
    List<DocumentInstanceDto> list = resp.getBody();
    Assertions.assertNotNull(list);
    Assertions.assertEquals(1, list.size());
    Assertions.assertEquals(d1.id(), list.get(0).id());
  }

  @Test
  void list_invalidStatusParam_returns400() {
    UUID projectId = UUID.randomUUID();

    String url = UriComponentsBuilder.fromHttpUrl(baseUrl("/projects/" + projectId + "/documents"))
        .queryParam("status", "NOT_A_STATUS")
        .toUriString();

    ResponseEntity<String> resp = rest.exchange(
        url,
        HttpMethod.GET,
        new HttpEntity<>(bearerHeaders(manager.accessToken())),
        String.class
    );

    Assertions.assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
  }
}
