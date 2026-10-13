package com.weng.licensehub.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "licensehub.rate-limit.activation.requests-per-minute=2")
@AutoConfigureMockMvc
@Testcontainers
public class ActivationRateLimitIntegrationTest {
  @Container @ServiceConnection
  static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private MockMvc mockMvc;

  @Test
  void returns429WhenClientExceedsActivationLimit() throws Exception {

    performActivationRequest("192.0.2.10").andExpect(status().isBadRequest());

    performActivationRequest("192.0.2.10").andExpect(status().isBadRequest());

    performActivationRequest("192.0.2.10")
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "60"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(429))
        .andExpect(jsonPath("$.title").value("Too many requests"));
  }

  private ResultActions performActivationRequest(String clientAddress) throws Exception {

    return mockMvc.perform(
        post("/api/activations")
            .with(
                request -> {
                  request.setRemoteAddr(clientAddress);
                  return request;
                })
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"));
  }
}
