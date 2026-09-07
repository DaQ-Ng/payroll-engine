package com.payrollengine.dto;

import com.payrollengine.domain.EmploymentType;
import com.payrollengine.domain.FilingStatus;
import com.payrollengine.domain.PayFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record EmployeeRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Pattern(regexp = "\\d{4}", message = "must be exactly 4 digits") String ssnLastFour,
        @NotNull FilingStatus filingStatus,
        @NotNull EmploymentType employmentType,
        @NotNull PayFrequency payFrequency,
        @Positive BigDecimal annualSalary,
        @Positive BigDecimal hourlyRate
) {
}
