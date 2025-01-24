package org.example.awesome.pizza.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends BaseHttpException {
  public BadRequestException(final String message) {
    super(HttpStatus.BAD_REQUEST, message);
  }
}
