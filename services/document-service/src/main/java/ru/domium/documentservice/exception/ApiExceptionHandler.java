package ru.domium.documentservice.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, Object>> handle(ResponseStatusException ex) {

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", ex.getStatusCode().value());
    body.put("error", ex.getStatusCode().toString());
    body.put("message", ex.getReason());
    body.put("timestamp", Instant.now());

    return ResponseEntity
        .status(ex.getStatusCode())
        .body(body);
  }
}
