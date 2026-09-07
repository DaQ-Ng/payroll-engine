package com.payrollengine.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One payroll run for one pay period. The unique constraint on
 * {@code pay_period_id} (see V1 migration) is the actual idempotency
 * guarantee: two concurrent "process this pay period" requests race to
 * insert this row, exactly one wins, and the loser is redirected to the
 * winner's result instead of double-processing. See PayrollRunService.
 */
@Entity
@Table(name = "pay_runs", uniqueConstraints = @UniqueConstraint(name = "uk_pay_run_period", columnNames = "pay_period_id"))
public class PayRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pay_period_id", nullable = false)
    private Long payPeriodId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayRunStatus status;

    @Column(nullable = false)
    private Instant triggeredAt = Instant.now();

    private Instant completedAt;

    protected PayRun() {
        // JPA
    }

    public PayRun(Long payPeriodId) {
        this.payPeriodId = payPeriodId;
        this.status = PayRunStatus.PROCESSING;
    }

    public void markCompleted() {
        this.status = PayRunStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void markFailed() {
        this.status = PayRunStatus.FAILED;
        this.completedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPayPeriodId() {
        return payPeriodId;
    }

    public PayRunStatus getStatus() {
        return status;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
