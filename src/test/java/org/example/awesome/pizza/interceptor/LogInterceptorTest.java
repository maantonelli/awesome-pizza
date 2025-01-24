package org.example.awesome.pizza.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({SpringExtension.class})
public class LogInterceptorTest {
  @InjectMocks
  private LogInterceptor underTest;

  @Mock
  private HttpServletRequest request;
  @Mock
  private HttpServletResponse response;

  @Test
  void preHandleTest() throws Exception {
    Assertions.assertThat(underTest.preHandle(request, response, new Object()))
        .isTrue();
  }

  @Test
  void afterCompletionTest() {
    Assertions.assertThatNoException()
        .isThrownBy(() -> underTest.afterCompletion(request, response, new Object(), null));
  }
}
