package com.sophia.insightflow.config;

import com.sophia.insightflow.analytics.dto.AnalyticsErrorResponse;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler({
    IllegalArgumentException.class,
    MethodArgumentTypeMismatchException.class,
    MethodArgumentNotValidException.class
  })
  public ResponseEntity<AnalyticsErrorResponse> handleBadRequest(Exception exception) {
    return error(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<AnalyticsErrorResponse> handleUnexpected(Exception exception) {
    LOGGER.error("Unexpected API error", exception);
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected service error");
  }

  private ResponseEntity<AnalyticsErrorResponse> error(HttpStatus status, String message) {
    return ResponseEntity.status(status)
        .body(new AnalyticsErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message));
  }
}
