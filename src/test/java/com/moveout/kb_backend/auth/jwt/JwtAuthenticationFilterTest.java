package com.moveout.kb_backend.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_토큰이면_SecurityContext에_인증정보를_설정한다() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtProvider.getUserId("valid-token")).thenReturn(userId);

        MockHttpServletRequest request = requestWithToken("valid-token");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(userId);
    }

    @Test
    void 만료된_토큰이면_AUTH_005를_request_attribute에_남긴다() throws Exception {
        when(jwtProvider.getUserId("expired-token")).thenThrow(new ExpiredJwtException(null, null, "expired"));

        MockHttpServletRequest request = requestWithToken("expired-token");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.ERROR_CODE_ATTRIBUTE)).isEqualTo("AUTH_005");
    }

    @Test
    void 무효한_토큰이면_AUTH_004를_request_attribute에_남긴다() throws Exception {
        when(jwtProvider.getUserId("bad-token")).thenThrow(new MalformedJwtException("malformed"));

        MockHttpServletRequest request = requestWithToken("bad-token");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.ERROR_CODE_ATTRIBUTE)).isEqualTo("AUTH_004");
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
