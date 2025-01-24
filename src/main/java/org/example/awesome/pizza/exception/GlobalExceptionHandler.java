package org.example.awesome.pizza.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(exception = BaseHttpException.class)
  public <E extends BaseHttpException> ResponseEntity<String> httpExceptionHandler(final E exception) {
    log.error("Application managed exception: {} ", exception.getHttpStatus(), exception);
    return ResponseEntity.status(exception.getHttpStatus()).body(exception.getMessage());
  }

  @ExceptionHandler(exception = MissingRequestHeaderException.class)
  public ResponseEntity<String> missingHeaderExceptionHandler(final MissingRequestHeaderException exception) {
    log.error("Missing header [{}] ", exception.getHeaderName(), exception);
    return ResponseEntity.badRequest().body(exception.getMessage());
  }

  @ExceptionHandler(exception = HttpMessageNotReadableException.class)
  public ResponseEntity<String> msgNotReadableException(final HttpMessageNotReadableException exception) {
    log.error("Could not read request body exception", exception);
    return ResponseEntity.badRequest().body(exception.getMessage());
  }

  @ExceptionHandler(exception = Exception.class)
  public ResponseEntity<String> genericExceptionHandler(final Exception exception) {
    final HttpStatus status = Optional.of(exception)
        .filter(ErrorResponse.class::isInstance)
        .map(ErrorResponse.class::cast)
        .map(ErrorResponse::getStatusCode)
        .map(code -> HttpStatus.resolve(code.value()))
        .orElse(HttpStatus.INTERNAL_SERVER_ERROR);

    log.error("GENERIC exception: {} ", status, exception);
    return ResponseEntity.status(status).body(exception.getMessage());
  }

}
