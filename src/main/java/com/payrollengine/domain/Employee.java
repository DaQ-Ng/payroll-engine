package com.payrollengine.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    /** Last 4 digits only — this is a demo system, never store a full SSN in plaintext. */
    @Column(nullable = false, length = 4)
    private String ssnLastFour;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FilingStatus filingStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayFrequency payFrequency;

    /** Required when employmentType == SALARY; ignored for HOURLY employees. */
    @Column(precision = 14, scale = 2)
    private BigDecimal annualSalary;

    /** Required when employmentType == HOURLY; ignored for SALARY employees. */
    @Column(precision = 8, scale = 2)
    private BigDecimal hourlyRate;

    @Column(nullable = false)
    private boolean active = true;

    // --- Year-to-date accumulators. These drive wage-base caps (Social
    // Security) and threshold triggers (Additional Medicare) and MUST only be
    // advanced once per pay run — see PayrollRunService for the idempotency
    // guard that protects this invariant. ---

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal ytdGrossWages = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal ytdSocialSecurityWages = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal ytdMedicareWages = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal ytdFederalWithholding = BigDecimal.ZERO;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Employee() {
        // JPA
    }

    public Employee(String firstName, String lastName, String ssnLastFour, FilingStatus filingStatus,
                     EmploymentType employmentType, PayFrequency payFrequency,
                     BigDecimal annualSalary, BigDecimal hourlyRate) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.ssnLastFour = ssnLastFour;
        this.filingStatus = filingStatus;
        this.employmentType = employmentType;
        this.payFrequency = payFrequency;
        this.annualSalary = annualSalary;
        this.hourlyRate = hourlyRate;
    }

    public void applyYtdDelta(BigDecimal grossDelta, BigDecimal ssWageDelta, BigDecimal medicareWageDelta,
                              BigDecimal federalWithholdingDelta) {
        this.ytdGrossWages = this.ytdGrossWages.add(grossDelta);
        this.ytdSocialSecurityWages = this.ytdSocialSecurityWages.add(ssWageDelta);
        this.ytdMedicareWages = this.ytdMedicareWages.add(medicareWageDelta);
        this.ytdFederalWithholding = this.ytdFederalWithholding.add(federalWithholdingDelta);
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getSsnLastFour() {
        return ssnLastFour;
    }

    public FilingStatus getFilingStatus() {
        return filingStatus;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public PayFrequency getPayFrequency() {
        return payFrequency;
    }

    public BigDecimal getAnnualSalary() {
        return annualSalary;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public BigDecimal getYtdGrossWages() {
        return ytdGrossWages;
    }

    public BigDecimal getYtdSocialSecurityWages() {
        return ytdSocialSecurityWages;
    }

    public BigDecimal getYtdMedicareWages() {
        return ytdMedicareWages;
    }

    public BigDecimal getYtdFederalWithholding() {
        return ytdFederalWithholding;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
