package com.payrollengine.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Hours worked by one employee in the pay period. Required only for
 * HOURLY employees; ignored for SALARY employees, whose gross pay is
 * derived from annualSalary / periodsPerYear instead. */
public record TimeEntryRequest(
        @NotNull Long employeeId,
        @NotNull @PositiveOrZero BigDecimal regularHours,
        @NotNull @PositiveOrZero BigDecimal overtimeHours
) {
}
