package org.example.awesome.pizza;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AwesomePizzaApplicationTest {

  @Test
  void mainTest() {
    Assertions.assertDoesNotThrow(() -> AwesomePizzaApplication.main(new String[] {}));
  }
}
