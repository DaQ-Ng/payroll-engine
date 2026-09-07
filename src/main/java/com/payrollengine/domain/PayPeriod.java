package com.payrollengine.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "pay_periods")
public class PayPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private LocalDate payDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayPeriodStatus status = PayPeriodStatus.OPEN;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected PayPeriod() {
        // JPA
    }

    public PayPeriod(LocalDate startDate, LocalDate endDate, LocalDate payDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.payDate = payDate;
    }

    public void markProcessed() {
        this.status = PayPeriodStatus.PROCESSED;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalDate getPayDate() {
        return payDate;
    }

    public PayPeriodStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
