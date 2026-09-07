package com.payrollengine.service.tax;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * FICA: Social Security (capped at an annual wage base, matched by the
 * employer) and Medicare (uncapped, matched by the employer, plus an
 * employee-only Additional Medicare Tax above a flat wage threshold).
 *
 * <p>Wage-base and threshold figures are the actual 2024 statutory amounts
 * (unlike the illustrative federal brackets, these change rarely and are
 * simple enough to state exactly): Social Security wage base $168,600,
 * combined 6.2% rate each side; Medicare 1.45% each side, uncapped;
 * Additional Medicare 0.9%, employee-only, on wages over $200,000 — per IRS
 * rules this $200,000 employer-withholding trigger is a flat figure
 * regardless of the employee's filing status; true reconciliation against
 * the filing-status-specific threshold happens on the employee's own return.
 */
@Component
public class FicaCalculator {

    static final BigDecimal SOCIAL_SECURITY_WAGE_BASE = new BigDecimal("168600.00");
    static final BigDecimal SOCIAL_SECURITY_RATE = new BigDecimal("0.062");
    static final BigDecimal MEDICARE_RATE = new BigDecimal("0.0145");
    static final BigDecimal ADDITIONAL_MEDICARE_RATE = new BigDecimal("0.009");
    static final BigDecimal ADDITIONAL_MEDICARE_THRESHOLD = new BigDecimal("200000.00");

    public FicaResult calculate(BigDecimal currentGrossPay, BigDecimal ytdSocialSecurityWagesBefore,
                                 BigDecimal ytdMedicareWagesBefore) {

        BigDecimal ssRemainingRoom = SOCIAL_SECURITY_WAGE_BASE.subtract(ytdSocialSecurityWagesBefore);
        BigDecimal socialSecurityTaxableWages = ssRemainingRoom.signum() <= 0
                ? BigDecimal.ZERO
                : currentGrossPay.min(ssRemainingRoom);

        BigDecimal employeeSocialSecurity = round(socialSecurityTaxableWages.multiply(SOCIAL_SECURITY_RATE));
        BigDecimal employerSocialSecurity = employeeSocialSecurity; // same taxable base, same rate

        BigDecimal employeeMedicare = round(currentGrossPay.multiply(MEDICARE_RATE));
        BigDecimal employerMedicare = employeeMedicare;

        BigDecimal ytdMedicareWagesAfter = ytdMedicareWagesBefore.add(currentGrossPay);
        BigDecimal amountOverThreshold = ytdMedicareWagesAfter.subtract(ADDITIONAL_MEDICARE_THRESHOLD);
        BigDecimal additionalMedicareTaxableWages = amountOverThreshold.signum() <= 0
                ? BigDecimal.ZERO
                : currentGrossPay.min(amountOverThreshold);
        BigDecimal additionalMedicareEmployee = round(additionalMedicareTaxableWages.multiply(ADDITIONAL_MEDICARE_RATE));

        return new FicaResult(
                socialSecurityTaxableWages.setScale(2, RoundingMode.HALF_UP),
                employeeSocialSecurity,
                employerSocialSecurity,
                employeeMedicare,
                employerMedicare,
                additionalMedicareEmployee
        );
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
