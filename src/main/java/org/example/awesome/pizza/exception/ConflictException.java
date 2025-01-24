package org.example.awesome.pizza.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BaseHttpException {
  public ConflictException(final String message) {
    super(HttpStatus.CONFLICT, message);
  }
}
