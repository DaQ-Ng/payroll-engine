package com.payrollengine.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Minimal API-key gate on {@code /api/**}. A real deployment would use
 * OAuth2/JWT with per-client scopes; this exists so the API isn't wide open
 * by default, not as a claim of production-grade auth — that trade-off is
 * intentional and documented in the README rather than hidden.
 */
public class ApiKeyFilter extends OncePerRequestFilter {

    private final String expectedApiKey;

    public ApiKeyFilter(String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String providedKey = request.getHeader("X-API-Key");
        if (expectedApiKey == null || expectedApiKey.isBlank() || expectedApiKey.equals(providedKey)) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Missing or invalid X-API-Key header\"}");
    }
}
