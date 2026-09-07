package com.payrollengine.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * One leg of a double-entry posting. A single business event (e.g. "pay
 * employee #5") produces multiple LedgerEntry rows whose debits and credits
 * must sum to zero — see LedgerService#verifyBalanced. Storing debit/credit
 * as two non-negative columns (rather than one signed amount) mirrors how
 * real general ledgers are modeled and makes "is this a debit or credit
 * account" unambiguous at the row level.
 */
@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pay_run_id", nullable = false)
    private Long payRunId;

    @Column(name = "employee_id")
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerAccountCode accountCode;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal debitAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal creditAmount;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected LedgerEntry() {
        // JPA
    }

    public LedgerEntry(Long payRunId, Long employeeId, LedgerAccountCode accountCode,
                        BigDecimal debitAmount, BigDecimal creditAmount, String description) {
        this.payRunId = payRunId;
        this.employeeId = employeeId;
        this.accountCode = accountCode;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
        this.description = description;
    }

    public static LedgerEntry debit(Long payRunId, Long employeeId, LedgerAccountCode accountCode,
                                     BigDecimal amount, String description) {
        return new LedgerEntry(payRunId, employeeId, accountCode, amount, BigDecimal.ZERO, description);
    }

    public static LedgerEntry credit(Long payRunId, Long employeeId, LedgerAccountCode accountCode,
                                      BigDecimal amount, String description) {
        return new LedgerEntry(payRunId, employeeId, accountCode, BigDecimal.ZERO, amount, description);
    }

    public Long getId() {
        return id;
    }

    public Long getPayRunId() {
        return payRunId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public LedgerAccountCode getAccountCode() {
        return accountCode;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
