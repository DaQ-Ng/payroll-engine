package com.payrollengine.service;

import java.math.BigDecimal;

/** Pure calculation output for one employee's pay in one pay run — not yet
 * persisted. Kept separate from the entity so the math can be unit tested
 * without touching a database. */
public record PayrollCalculationResult(
        BigDecimal regularHours,
        BigDecimal overtimeHours,
        BigDecimal grossPay,
        BigDecimal federalWithholding,
        BigDecimal socialSecurityEmployee,
        BigDecimal socialSecurityEmployer,
        BigDecimal medicareEmployee,
        BigDecimal medicareEmployer,
        BigDecimal additionalMedicareEmployee,
        BigDecimal netPay,
        BigDecimal ytdSocialSecurityWageDelta,
        BigDecimal ytdMedicareWageDelta
) {
}
