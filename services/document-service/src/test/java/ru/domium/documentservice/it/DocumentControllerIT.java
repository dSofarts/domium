package ru.domium.documentservice.it;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;
import ru.domium.documentservice.dto.DocumentDtos.DocumentDetailsDto;
import ru.domium.documentservice.dto.DocumentDtos.RejectRequest;
import ru.domium.documentservice.dto.DocumentDtos.SignRequest;
import ru.domium.documentservice.dto.DocumentDtos.SignatureDto;
import ru.domium.documentservice.model.DocumentStatus;
import ru.domium.documentservice.model.SignatureType;

public class DocumentControllerIT extends AbstractDocumentServiceIT {

  @Test
  void details_asOwnerClient_returnsDetailsIncludingAuditAndVersions() {
    UUID projectId = UUID.randomUUID();
    UUID stage = UUID.randomUUID();
    var created = manualUpload(projectId, stage, client.userId(), "details");

    ResponseEntity<DocumentDetailsDto> resp = rest.exchange(
        baseUrl("/" + created.id()),
        HttpMethod.GET,
        new HttpEntity<>(bearerHeaders(client.accessToken())),
        DocumentDetailsDto.class
    );

    Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
    DocumentDetailsDto body = resp.getBody();
    Assertions.assertNotNull(body);
    Assertions.assertEquals(created.id(), body.document().id());

    Assertions.assertTrue(body.versions().size() >= 1);
    Assertions.assertTrue(body.audit().size() >= 2);
  }

  @Test
  void details_asOtherClient_forbidden_403() {
    UUID projectId = UUID.randomUUID();
    UUID stage = UUID.randomUUID();

    var created = manualUpload(projectId, stage, UUID.randomUUID(), "forbidden");

    ResponseEntity<String> resp = rest.exchange(
        baseUrl("/" + created.id()),
        HttpMethod.GET,
        new HttpEntity<>(bearerHeaders(client.accessToken())),
        String.class
    );

    Assertions.assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
  }

  @Test
  void file_markViewed_true_owner_updatesStatusToViewed_andAddsAudit() {
    UUID projectId = UUID.randomUUID();
    UUID stage = UUID.randomUUID();
    var created = manualUpload(projectId, stage, client.userId(), "view");

    String fileUrl = UriComponentsBuilder.fromHttpUrl(baseUrl("/" + created.id() + "/file"))
        .queryParam("markViewed", true)
        .toUriString();

    HttpHeaders headers = bearerHeaders(client.accessToken());
    headers.setAccept(List.of(MediaType.APPLICATION_PDF));

    ResponseEntity<byte[]> resp = rest.exchange(
        fileUrl,
        HttpMethod.GET,
        new HttpEntity<>(headers),
        byte[].class
    );

    Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
    Assertions.assertEquals(MediaType.APPLICATION_PDF, resp.getHeaders().getContentType());
    Assertions.assertTrue(Objects.requireNonNull(resp.getBody()).length > 10);

    String status = jdbc.queryForObject(
        "select status from document.document_instance where id = ?",
        String.class,
        created.id()
    );
    Assertions.assertEquals(DocumentStatus.VIEWED.name(), status);

    Integer viewedAudit = jdbc.queryForObject(
        "select count(*) from document.document_audit_log where document_id = ? and action = 'VIEWED'",
        Integer.class,
        created.id()
    );
    Assertions.assertEquals(1, viewedAudit);
  }

  @Test
  void file_withoutToken_returns401() {
    ResponseEntity<String> resp = rest.getForEntity(
        baseUrl("/" + UUID.randomUUID() + "/file"),
        String.class
    );
    Assertions.assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
  }

  @Test
  void sign_success_requiresBothSides_andCreatesTwoSignatures() {
    UUID projectId = UUID.randomUUID();
    UUID stage = UUID.randomUUID();
    var created = manualUpload(projectId, stage, client.userId(), "sign");

    SignRequest reqBody = new SignRequest(SignatureType.SIMPLE, "1234");

    HttpHeaders headers = bearerHeaders(client.accessToken());
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("User-Agent", "domium-it");

    ResponseEntity<SignatureDto> resp = rest.exchange(
        baseUrl("/" + created.id() + "/sign"),
        HttpMethod.POST,
        new HttpEntity<>(reqBody, headers),
        SignatureDto.class
    );

    Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
    Assertions.assertNotNull(resp.getBody());
    Assertions.assertEquals(created.id(), Objects.requireNonNull(resp.getBody()).documentId());
    Assertions.assertEquals("CLIENT", Objects.requireNonNull(resp.getBody()).signerType().name());

    String status = jdbc.queryForObject(
        "select status from document.document_instance where id = ?",
        String.class,
        created.id()
    );
    Assertions.assertEquals(DocumentStatus.SENT_TO_USER.name(), status);

    HttpHeaders managerHeaders = bearerHeaders(manager.accessToken());
    managerHeaders.setContentType(MediaType.APPLICATION_JSON);
    managerHeaders.set("User-Agent", "domium-it");

    ResponseEntity<SignatureDto> managerResp = rest.exchange(
        baseUrl("/" + created.id() + "/sign"),
        HttpMethod.POST,
        new HttpEntity<>(reqBody, managerHeaders),
        SignatureDto.class
    );

    Assertions.assertEquals(HttpStatus.OK, managerResp.getStatusCode());
    Assertions.assertNotNull(managerResp.getBody());
    Assertions.assertEquals("MANAGER", Objects.requireNonNull(managerResp.getBody()).signerType().name());

    String finalStatus = jdbc.queryForObject(
        "select status from document.document_instance where id = ?",
        String.class,
        created.id()
    );
    Assertions.assertEquals(DocumentStatus.SIGNED.name(), finalStatus);

    Integer sigCnt = jdbc.queryForObject(
        "select count(*) from document.document_signature where document_id = ?",
        Integer.class,
        created.id()
    );
    Assertions.assertEquals(2, sigCnt);

    Integer signedAudit = jdbc.queryForObject(
        "select count(*) from document.document_audit_log where document_id = ? and action = 'SIGNED'",
        Integer.class,
        created.id()
    );
    Assertions.assertEquals(2, signedAudit);
  }

  @Test
  void sign_invalidConfirmationCode_returns400() {
    UUID projectId = UUID.randomUUID();
    UUID stage = UUID.randomUUID();
    var created = manualUpload(projectId, stage, client.userId(), "sign-bad");

    SignRequest reqBody = new SignRequest(SignatureType.SIMPLE, "12");

    HttpHeaders headers = bearerHeaders(client.accessToken());
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> resp = rest.exchange(
        baseUrl("/" + created.id() + "/sign"),
        HttpMethod.POST,
        new HttpEntity<>(reqBody, headers),
        String.class
    );

    Assertions.assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
  }

  @Test
  void reject_success_changesStatusRejected_andCreatesCommentAndAudit() {
    UUID projectId = UUID.randomUUID();
    UUID stage = UUID.randomUUID();
    var created = manualUpload(projectId, stage, client.userId(), "reject");

    RejectRequest reqBody = new RejectRequest("не согласен");

    HttpHeaders headers = bearerHeaders(client.accessToken());
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Void> resp = rest.exchange(
        baseUrl("/" + created.id() + "/reject"),
        HttpMethod.POST,
        new HttpEntity<>(reqBody, headers),
        Void.class
    );

    Assertions.assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());

    String status = jdbc.queryForObject(
        "select status from document.document_instance where id = ?",
        String.class,
        created.id()
    );
    Assertions.assertEquals(DocumentStatus.REJECTED.name(), status);

    Integer commentCnt = jdbc.queryForObject(
        "select count(*) from document.document_comment where document_id = ?",
        Integer.class,
        created.id()
    );
    Assertions.assertEquals(1, commentCnt);

    Integer rejectedAudit = jdbc.queryForObject(
        "select count(*) from document.document_audit_log where document_id = ? and action = 'REJECTED'",
        Integer.class,
        created.id()
    );
    Assertions.assertEquals(1, rejectedAudit);
  }

  @Test
  void reject_wrongStatus_returns400() {
    UUID projectId = UUID.randomUUID();
    UUID stage = UUID.randomUUID();
    var created = manualUpload(projectId, stage, client.userId(), "reject-bad");

    SignRequest signBody = new SignRequest(SignatureType.SIMPLE, "1234");
    HttpHeaders headers = bearerHeaders(client.accessToken());
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<SignatureDto> signResp = rest.exchange(
        baseUrl("/" + created.id() + "/sign"),
        HttpMethod.POST,
        new HttpEntity<>(signBody, headers),
        SignatureDto.class
    );
    Assertions.assertEquals(HttpStatus.OK, signResp.getStatusCode());

    HttpHeaders managerHeaders = bearerHeaders(manager.accessToken());
    managerHeaders.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<SignatureDto> signRespManager = rest.exchange(
        baseUrl("/" + created.id() + "/sign"),
        HttpMethod.POST,
        new HttpEntity<>(signBody, managerHeaders),
        SignatureDto.class
    );
    Assertions.assertEquals(HttpStatus.OK, signRespManager.getStatusCode());

    RejectRequest rejectBody = new RejectRequest("late reject");
    ResponseEntity<String> rejectResp = rest.exchange(
        baseUrl("/" + created.id() + "/reject"),
        HttpMethod.POST,
        new HttpEntity<>(rejectBody, headers),
        String.class
    );

    Assertions.assertEquals(HttpStatus.BAD_REQUEST, rejectResp.getStatusCode());
  }
}
