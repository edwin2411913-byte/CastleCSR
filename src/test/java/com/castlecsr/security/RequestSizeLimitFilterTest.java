package com.castlecsr.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RequestSizeLimitFilterTest {

    private RequestSizeLimitFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RequestSizeLimitFilter(1024);
        chain = mock(FilterChain.class);
    }

    @Test
    void payloadDentroDelLimite_continuaLaCadena() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/csr/generar");
        request.setContent(new byte[512]);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void payloadExcesivo_devuelve413SinContinuar() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/csr/generar");
        request.setContent(new byte[2048]);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void rutasNoApi_noSeFiltran() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/index.html");
        request.setContent(new byte[2048]);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}