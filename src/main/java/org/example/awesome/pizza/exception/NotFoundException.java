package org.example.awesome.pizza.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BaseHttpException {
  public NotFoundException(final String message) {
    super(HttpStatus.NOT_FOUND, message);
  }
}
