package org.example.awesome.pizza.exception;

import org.springframework.http.HttpStatus;

public class InternalServerErrorException extends BaseHttpException {
  public InternalServerErrorException(final String message) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, message);
  }
}
