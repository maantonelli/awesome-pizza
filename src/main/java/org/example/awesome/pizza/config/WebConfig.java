package org.example.awesome.pizza.config;

import lombok.RequiredArgsConstructor;
import org.example.awesome.pizza.interceptor.LogInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
  private final LogInterceptor logInterceptor;
  @Value("${awesome-pizza.config.http-log:false}")
  private final boolean shouldLog;

  /**
   * Add logging interceptor to HTTP interceptors, if configured
   * @param registry interceptor registry
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    if (shouldLog)
      registry.addInterceptor(logInterceptor);
  }
}
