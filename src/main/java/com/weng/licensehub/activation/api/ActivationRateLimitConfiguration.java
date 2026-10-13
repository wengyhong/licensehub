package com.weng.licensehub.activation.api;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration(proxyBeanMethods = false)
public class ActivationRateLimitConfiguration {

  @Bean
  FilterRegistrationBean<ActivationRateLimitFilter> activationRateLimitFilterRegistration(
      ActivationRateLimiter rateLimiter) {

    var registration = new FilterRegistrationBean<ActivationRateLimitFilter>();

    registration.setFilter(new ActivationRateLimitFilter(rateLimiter));

    registration.addUrlPatterns("/api/activations", "/api/activations/*");

    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);

    return registration;
  }
}
