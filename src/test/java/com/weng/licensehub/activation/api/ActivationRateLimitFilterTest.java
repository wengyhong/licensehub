package com.weng.licensehub.activation.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivationRateLimitFilterTest {

    private ActivationRateLimiter rateLimiter;
    private ActivationRateLimitFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimiter = mock(ActivationRateLimiter.class);
        filter = new ActivationRateLimitFilter(rateLimiter);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void allowsActivationRequestWhenRateLimitIsNotExceeded()
            throws Exception {

        var request = activationRequest("192.0.2.10");
        var response = new MockHttpServletResponse();

        when(rateLimiter.tryConsume("192.0.2.10"))
                .thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(rateLimiter).tryConsume("192.0.2.10");
        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus())
                .isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void returns429WhenRateLimitIsExceeded()
            throws Exception {

        var request = activationRequest("192.0.2.10");
        var response = new MockHttpServletResponse();

        when(rateLimiter.tryConsume("192.0.2.10"))
                .thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(response.getHeader("Retry-After"))
                .isEqualTo("60");
        assertThat(response.getContentType())
                .startsWith("application/problem+json");
        assertThat(response.getContentAsString())
                .contains("Activation request limit exceeded");

        verify(rateLimiter).tryConsume("192.0.2.10");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doesNotRateLimitUnrelatedEndpoint()
            throws Exception {

        var request = new MockHttpServletRequest(
                "POST",
                "/api/products");
        request.setRemoteAddr("192.0.2.10");

        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(rateLimiter, never())
                .tryConsume("192.0.2.10");
        verify(filterChain).doFilter(request, response);
    }

    private MockHttpServletRequest activationRequest(
            String clientAddress) {

        var request = new MockHttpServletRequest(
                "POST",
                "/api/activations");
        request.setRemoteAddr(clientAddress);
        return request;
    }
}
