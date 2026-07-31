package com.moveout.kb_backend.auth.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ERROR_CODE_ATTRIBUTE = "jwtErrorCode";
    public static final String ERROR_MESSAGE_ATTRIBUTE = "jwtErrorMessage";

    private static final String HEADER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(HEADER_PREFIX)) {
            String token = header.substring(HEADER_PREFIX.length());
            try {
                UUID userId = jwtProvider.getUserId(token);
                var authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException e) {
                request.setAttribute(ERROR_CODE_ATTRIBUTE, "AUTH_005");
                request.setAttribute(ERROR_MESSAGE_ATTRIBUTE, "Access Token이 만료되었습니다.");
            } catch (JwtException | IllegalArgumentException e) {
                request.setAttribute(ERROR_CODE_ATTRIBUTE, "AUTH_004");
                request.setAttribute(ERROR_MESSAGE_ATTRIBUTE, "유효하지 않은 토큰입니다.");
            }
        }
        filterChain.doFilter(request, response);
    }
}
