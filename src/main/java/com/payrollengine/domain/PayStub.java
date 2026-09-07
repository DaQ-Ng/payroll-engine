package com.payrollengine.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pay_stubs", uniqueConstraints = @UniqueConstraint(name = "uk_pay_stub_run_employee", columnNames = {"pay_run_id", "employee_id"}))
public class PayStub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pay_run_id", nullable = false)
    private Long payRunId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal regularHours;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal overtimeHours;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal grossPay;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal federalWithholding;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal socialSecurityEmployee;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal medicareEmployee;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal additionalMedicareEmployee;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal socialSecurityEmployer;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal medicareEmployer;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal netPay;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected PayStub() {
        // JPA
    }

    public PayStub(Long payRunId, Long employeeId, BigDecimal regularHours, BigDecimal overtimeHours,
                   BigDecimal grossPay, BigDecimal federalWithholding, BigDecimal socialSecurityEmployee,
                   BigDecimal medicareEmployee, BigDecimal additionalMedicareEmployee,
                   BigDecimal socialSecurityEmployer, BigDecimal medicareEmployer, BigDecimal netPay) {
        this.payRunId = payRunId;
        this.employeeId = employeeId;
        this.regularHours = regularHours;
        this.overtimeHours = overtimeHours;
        this.grossPay = grossPay;
        this.federalWithholding = federalWithholding;
        this.socialSecurityEmployee = socialSecurityEmployee;
        this.medicareEmployee = medicareEmployee;
        this.additionalMedicareEmployee = additionalMedicareEmployee;
        this.socialSecurityEmployer = socialSecurityEmployer;
        this.medicareEmployer = medicareEmployer;
        this.netPay = netPay;
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

    public BigDecimal getRegularHours() {
        return regularHours;
    }

    public BigDecimal getOvertimeHours() {
        return overtimeHours;
    }

    public BigDecimal getGrossPay() {
        return grossPay;
    }

    public BigDecimal getFederalWithholding() {
        return federalWithholding;
    }

    public BigDecimal getSocialSecurityEmployee() {
        return socialSecurityEmployee;
    }

    public BigDecimal getMedicareEmployee() {
        return medicareEmployee;
    }

    public BigDecimal getAdditionalMedicareEmployee() {
        return additionalMedicareEmployee;
    }

    public BigDecimal getSocialSecurityEmployer() {
        return socialSecurityEmployer;
    }

    public BigDecimal getMedicareEmployer() {
        return medicareEmployer;
    }

    public BigDecimal getNetPay() {
        return netPay;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
