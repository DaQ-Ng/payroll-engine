package com.payrollengine.config;

import com.payrollengine.domain.IdempotencyRecord;
import com.payrollengine.repository.IdempotencyRecordRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Stripe-style {@code Idempotency-Key} support for any mutating request.
 *
 * <p>Complementary to (not a replacement for) the pay_runs.pay_period_id
 * uniqueness constraint: that guarantees correctness no matter what the
 * client does, while this lets a client that genuinely can't tell whether
 * its first attempt landed (e.g. it timed out waiting for the response)
 * retry safely and get back the exact original response, for *any* POST
 * endpoint — not just pay runs — without re-executing side effects.
 *
 * <p>Uses the same claim-then-complete shape as PayRunClaimService: insert
 * a placeholder row keyed on the client's idempotency key (the unique
 * constraint is the real guarantee under concurrency), forward the
 * request, then fill in the response. A concurrent request for the same
 * key sees the placeholder's null {@code responseStatus} and is told to
 * retry rather than being served a half-finished result.
 */
public class IdempotencyKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "Idempotency-Key";

    private final IdempotencyRecordRepository repository;

    public IdempotencyKeyFilter(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader(HEADER);
        if (!"POST".equalsIgnoreCase(request.getMethod()) || key == null || key.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String fingerprint = fingerprint(request.getMethod(), request.getRequestURI(), cachedRequest.getCachedBody());

        IdempotencyRecord placeholder = new IdempotencyRecord(key, fingerprint);
        try {
            repository.saveAndFlush(placeholder);
        } catch (DataIntegrityViolationException e) {
            handleExistingKey(key, fingerprint, response);
            return;
        }

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        chain.doFilter(cachedRequest, wrappedResponse);

        String responseBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
        placeholder.completeWith(wrappedResponse.getStatus(), responseBody);
        repository.save(placeholder);

        wrappedResponse.copyBodyToResponse();
    }

    private void handleExistingKey(String key, String fingerprint, HttpServletResponse response) throws IOException {
        IdempotencyRecord existing = repository.findByIdempotencyKey(key).orElse(null);
        if (existing == null) {
            // Vanishingly rare: it was deleted between our failed insert and
            // this lookup. Ask the client to just retry with a fresh attempt.
            writeJson(response, 409, "{\"error\":\"Conflict\",\"message\":\"Idempotency key state changed concurrently; retry.\"}");
            return;
        }
        if (!existing.getRequestFingerprint().equals(fingerprint)) {
            writeJson(response, 422, "{\"error\":\"Unprocessable Entity\",\"message\":\"Idempotency-Key '"
                    + key + "' was already used with a different request body or path.\"}");
            return;
        }
        if (existing.getResponseStatus() == null) {
            writeJson(response, 409, "{\"error\":\"Conflict\",\"message\":\"A request with Idempotency-Key '"
                    + key + "' is still being processed; retry shortly.\"}");
            return;
        }
        writeJson(response, existing.getResponseStatus(), existing.getResponseBody());
    }

    private void writeJson(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(body);
    }

    private String fingerprint(String method, String path, byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(method.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            digest.update(path.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            digest.update(body);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
