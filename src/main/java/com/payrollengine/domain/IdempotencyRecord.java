package com.payrollengine.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Backs the client-supplied {@code Idempotency-Key} header (see
 * IdempotencyKeyFilter) — a second, complementary idempotency mechanism to
 * the pay_runs.pay_period_id uniqueness constraint. That one guarantees
 * correctness regardless of what the client sends; this one lets a client
 * safely retry *any* POST (not just pay runs) after an ambiguous failure
 * (e.g. a timeout where it doesn't know if the first attempt landed) and
 * get back the exact original response instead of re-executing side
 * effects.
 *
 * <p>Follows the same claim-then-complete shape as PayRunClaimService: a
 * row is inserted with {@code responseStatus == null} the moment a key is
 * first seen (relying on the unique constraint on {@code idempotency_key}
 * to let exactly one concurrent request win), then updated with the real
 * response once the request finishes.
 */
@Entity
@Table(name = "idempotency_keys", uniqueConstraints = @UniqueConstraint(name = "uk_idempotency_key", columnNames = "idempotency_key"))
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    /** SHA-256 hex of method+path+body — lets us detect a client reusing
     * the same key for a genuinely different request, which is a client
     * bug, not a safe retry. */
    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    /** Null until the original request completes — a null status is how a
     * concurrent request detects "still in flight, not a bug, just retry." */
    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", length = 8000)
    private String responseBody;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant completedAt;

    protected IdempotencyRecord() {
        // JPA
    }

    public IdempotencyRecord(String idempotencyKey, String requestFingerprint) {
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
    }

    public void completeWith(int status, String body) {
        this.responseStatus = status;
        // Demo-scoped cap; a production version would store this in a table
        // or blob store without a fixed size instead of truncating.
        this.responseBody = body.length() > 8000 ? body.substring(0, 8000) : body;
        this.completedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
