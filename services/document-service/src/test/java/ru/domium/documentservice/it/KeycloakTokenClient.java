package ru.domium.documentservice.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


public final class KeycloakTokenClient {

  public record TokenInfo(String accessToken, UUID userId, Set<String> realmRoles) {}

  private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration READY_WAIT_MAX = Duration.ofSeconds(60);
  private static final long RETRY_SLEEP_MS = 500;

  private final HttpClient http;
  private final ObjectMapper om;
  private final URI tokenEndpoint;
  private final URI openIdConfigEndpoint;
  private final String clientId;
  private final String clientSecret;

  public KeycloakTokenClient(ObjectMapper om, String issuerUri, String clientId, String clientSecret) {
    this.http = HttpClient.newBuilder()
        .connectTimeout(HTTP_TIMEOUT)
        .version(HttpClient.Version.HTTP_1_1)
        .build();
    this.om = om;

    String base = stripTrailingSlash(issuerUri);
    this.tokenEndpoint = URI.create(base + "/protocol/openid-connect/token");
    this.openIdConfigEndpoint = URI.create(base + "/.well-known/openid-configuration");

    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }

  public TokenInfo passwordGrant(String username, String password) {
    waitUntilReady();

    String form = "grant_type=password"
        + "&client_id=" + enc(clientId)
        + "&client_secret=" + enc(clientSecret)
        + "&username=" + enc(username)
        + "&password=" + enc(password);

    HttpRequest req = HttpRequest.newBuilder(tokenEndpoint)
        .timeout(HTTP_TIMEOUT)
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(form))
        .build();

    HttpResponse<String> resp = sendWithRetry(req);

    try {
      if (resp.statusCode() != 200) {
        throw new IllegalStateException("Keycloak token request failed: HTTP " + resp.statusCode()
            + ", endpoint=" + tokenEndpoint + ", body=" + resp.body());
      }
      JsonNode json = om.readTree(resp.body());
      String accessToken = json.get("access_token").asText();
      Decoded decoded = decodeJwt(accessToken);
      return new TokenInfo(accessToken, decoded.userId, decoded.roles);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to parse Keycloak response from " + tokenEndpoint + ": " + resp.body(), e);
    }
  }

  private HttpResponse<String> sendWithRetry(HttpRequest req) {
    long deadline = System.nanoTime() + READY_WAIT_MAX.toNanos();
    int attempt = 0;
    while (true) {
      attempt++;
      try {
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        int sc = resp.statusCode();

        if (sc == 400) {
          String b = resp.body() == null ? "" : resp.body();
          if (b.contains("invalid_grant") || b.contains("unauthorized_client") || b.contains("invalid_client")) {
            return resp;
          }
        }

        if (sc == 404 || sc == 502 || sc == 503 || sc == 504) {
          if (System.nanoTime() > deadline) return resp;
          sleepQuietly(RETRY_SLEEP_MS);
          continue;
        }

        return resp;
      } catch (IOException e) {
        if (System.nanoTime() > deadline) {
          throw new IllegalStateException("Keycloak request failed after " + attempt
              + " attempts, last error: " + e.getMessage() + ", endpoint=" + tokenEndpoint, e);
        }
        sleepQuietly(RETRY_SLEEP_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted while requesting Keycloak token", e);
      }
    }
  }

  private void waitUntilReady() {
    long deadline = System.nanoTime() + READY_WAIT_MAX.toNanos();
    while (System.nanoTime() < deadline) {
      try {
        HttpRequest req = HttpRequest.newBuilder(openIdConfigEndpoint)
            .timeout(HTTP_TIMEOUT)
            .GET()
            .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) {
          JsonNode json = om.readTree(resp.body());
          if (json.hasNonNull("token_endpoint")) return;
        }
      } catch (Exception ignored) {
      }
      sleepQuietly(RETRY_SLEEP_MS);
    }
    throw new IllegalStateException("Keycloak is not ready: " + openIdConfigEndpoint);
  }

  private record Decoded(UUID userId, Set<String> roles) {}

  private Decoded decodeJwt(String jwt) {
    int p1 = jwt.indexOf('.');
    if (p1 < 0) throw new IllegalArgumentException("Not a JWT: " + jwt);
    int p2 = jwt.indexOf('.', p1 + 1);
    if (p2 < 0) throw new IllegalArgumentException("Not a JWT: " + jwt);

    String payloadPart = jwt.substring(p1 + 1, p2);
    String payloadJson = new String(Base64.getUrlDecoder().decode(payloadPart), StandardCharsets.UTF_8);

    try {
      JsonNode payload = om.readTree(payloadJson);
      UUID sub = UUID.fromString(payload.get("sub").asText());

      Set<String> roles = new HashSet<>();
      JsonNode realmAccess = payload.get("realm_access");
      if (realmAccess != null && realmAccess.has("roles") && realmAccess.get("roles").isArray()) {
        for (JsonNode r : realmAccess.get("roles")) roles.add(r.asText());
      }
      return new Decoded(sub, roles);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to decode JWT payload: " + payloadJson, e);
    }
  }

  private static void sleepQuietly(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static String enc(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }

  private static String stripTrailingSlash(String s) {
    if (s == null) return "";
    return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
  }
}
