package ru.domium.documentservice.it;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;



@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
public class InfrastructureAvailabilityIT {

  private String keycloakBaseUrl;
  private String keycloakIssuer;
  private String minioBaseUrl;
  private String minioAccessKey;
  private String minioSecretKey;
  private String minioBucket;
  private String jdbcUrl;
  private String dbUser;
  private String dbPass;

  private HttpClient httpClient;
  private DriverManager ds;

  @BeforeAll
  void init() {
    keycloakBaseUrl = System.getenv("KC_HOSTNAME_URL");
    System.out.println(keycloakBaseUrl);
    keycloakIssuer = System.getenv("KEYCLOAK_ISSUER_URI");
    System.out.println(keycloakIssuer);
    minioBaseUrl = System.getenv("MINIO_ENDPOINT");
    System.out.println(minioBaseUrl);
    minioAccessKey = System.getenv("MINIO_ACCESS_KEY");
    System.out.println(minioAccessKey);
    minioSecretKey = System.getenv("MINIO_SECRET_KEY");
    System.out.println(minioSecretKey);
    minioBucket = "documents";
    jdbcUrl = "jdbc:postgresql://" + System.getenv("POSTGRES_HOST") +
        ":" + System.getenv("POSTGRES_PORT") + "/" + System.getenv("POSTGRES_DB");
    System.out.println(jdbcUrl);
    dbUser = System.getenv("POSTGRES_USER");
    System.out.println(dbUser);
    dbPass = System.getenv("POSTGRES_PASSWORD");
    System.out.println(dbPass);

    httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build();
    DriverManager.setLoginTimeout(50);
  }

  @Test
  @Timeout(50)
  void minio_is_live() {
    assertDoesNotThrow(() -> waitUntilHttpOk(minioBaseUrl + "/minio/health/live", Duration.ofSeconds(40),
        Duration.ofSeconds(2)), "MinIO should be live");
  }

  @Test
  @Timeout(50)
  void keycloak_is_live() {
    assertDoesNotThrow(() -> waitUntilHttpOk(keycloakIssuer + "/protocol/openid-connect/certs", Duration.ofSeconds(40),
        Duration.ofSeconds(2)), "Keycloak should be live");
  }

  @Test
  @Timeout(50)
  void postgres_is_live() {
    assertDoesNotThrow(()-> waitUntilDbReady(jdbcUrl,dbUser, dbPass,
        Duration.ofSeconds(40), Duration.ofSeconds(3)), "Postgres should be live");
  }

  private int getStatus(String url) throws IOException, InterruptedException {
    HttpRequest req = HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofSeconds(3))
        .GET()
        .build();
    HttpResponse<Void> resp = null;
    resp = httpClient.send(req, HttpResponse.BodyHandlers.discarding());

    return resp.statusCode();
  }

  private int waitUntilHttpOk(String url, Duration timeout, Duration pollInterval) {
    Instant start = Instant.now();
    Instant deadline = start.plus(timeout);
    Integer lastCode = null;
    Exception lastException = null;
    int code;
    while (Instant.now().isBefore(deadline)) {

      try {
        code = getStatus(url);
        if (code == 200)
          return 200;
        else{
          lastCode = code;
          sleep(pollInterval);
        }

      }catch(IOException e ){
        lastException = e;
          sleep(pollInterval);
      } catch (InterruptedException e) {
        lastException = e;
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    }
    throw new RuntimeException(
        String.format("HTTP endpoint %s did not respond with OK (%d) within %s with error %s",
            url, lastCode, timeout,lastException.getMessage()));
  }

  private void waitUntilDbReady(String jdbcUrl, String user, String pass, Duration timeout, Duration pollInterval){
    String lastError = null;
    Instant instant= Instant.now().plus(timeout);
    while (Instant.now().isBefore(instant)) {
      try(Connection conn = DriverManager.getConnection(jdbcUrl, user, pass)){
        try (PreparedStatement st = conn.prepareStatement("SELECT 1")) {
          st.setQueryTimeout(15);
          try(ResultSet rs = st.executeQuery()) {
            if (rs.next()) {
              if (rs.getInt(1) == 1) {
                return;
              }
            }
            sleep(pollInterval);
          }
        }
      } catch (SQLException e) {
        lastError = e.getMessage();
        sleep(pollInterval);
      }
    }
    throw new RuntimeException(
        String.format("DB endpoint %s did not respond with OK within %s with error %s",
            jdbcUrl, timeout, lastError));
  }

  private void sleep(Duration pollInterval){
    try {
      Thread.sleep(pollInterval.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

}
