package com.payrollengine.service.tax;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FicaCalculatorTest {

    private final FicaCalculator calculator = new FicaCalculator();

    @Test
    void normalPaycheckBelowAllCapsIsTaxedInFull() {
        FicaResult result = calculator.calculate(new BigDecimal("4000.00"), BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(result.socialSecurityTaxableWages()).isEqualByComparingTo("4000.00");
        assertThat(result.employeeSocialSecurity()).isEqualByComparingTo("248.00"); // 4000 * 6.2%
        assertThat(result.employerSocialSecurity()).isEqualByComparingTo(result.employeeSocialSecurity());
        assertThat(result.employeeMedicare()).isEqualByComparingTo("58.00"); // 4000 * 1.45%
        assertThat(result.employerMedicare()).isEqualByComparingTo(result.employeeMedicare());
        assertThat(result.additionalMedicareEmployee()).isEqualByComparingTo("0.00");
    }

    @Test
    void socialSecurityStopsExactlyAtTheWageBase() {
        // YTD already at the 2024 wage base ($168,600) — no more SS tax owed
        // this year no matter how much more this paycheck pays.
        FicaResult result = calculator.calculate(
                new BigDecimal("10000.00"), FicaCalculator.SOCIAL_SECURITY_WAGE_BASE, BigDecimal.ZERO);

        assertThat(result.socialSecurityTaxableWages()).isEqualByComparingTo("0.00");
        assertThat(result.employeeSocialSecurity()).isEqualByComparingTo("0.00");
        // Medicare has no cap, so it's still owed in full.
        assertThat(result.employeeMedicare()).isEqualByComparingTo("145.00");
    }

    @Test
    void socialSecurityIsPartiallyTaxedWhenThisPaycheckCrossesTheWageBase() {
        // 1,000 of room left before the cap; this paycheck is 5,000.
        BigDecimal ytdBefore = FicaCalculator.SOCIAL_SECURITY_WAGE_BASE.subtract(new BigDecimal("1000.00"));
        FicaResult result = calculator.calculate(new BigDecimal("5000.00"), ytdBefore, BigDecimal.ZERO);

        assertThat(result.socialSecurityTaxableWages()).isEqualByComparingTo("1000.00");
        assertThat(result.employeeSocialSecurity()).isEqualByComparingTo("62.00"); // 1000 * 6.2%
    }

    @Test
    void additionalMedicareDoesNotApplyBelowThreshold() {
        FicaResult result = calculator.calculate(new BigDecimal("5000.00"), BigDecimal.ZERO, new BigDecimal("50000.00"));
        assertThat(result.additionalMedicareEmployee()).isEqualByComparingTo("0.00");
    }

    @Test
    void additionalMedicareAppliesOnlyToTheAmountOverThreshold() {
        // YTD medicare wages of 199,000 + this 5,000 paycheck crosses
        // 200,000 by 4,000 — only that 4,000 is subject to the extra 0.9%.
        FicaResult result = calculator.calculate(new BigDecimal("5000.00"), BigDecimal.ZERO, new BigDecimal("199000.00"));
        assertThat(result.additionalMedicareEmployee()).isEqualByComparingTo("36.00"); // 4000 * 0.9%
    }

    @Test
    void additionalMedicareHasNoEmployerMatch() {
        FicaResult result = calculator.calculate(new BigDecimal("50000.00"), BigDecimal.ZERO, new BigDecimal("199000.00"));
        // Employer Medicare is the flat 1.45% match only — never includes the additional 0.9%.
        BigDecimal expectedEmployerMedicare = new BigDecimal("50000.00").multiply(FicaCalculator.MEDICARE_RATE)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        assertThat(result.employerMedicare()).isEqualByComparingTo(expectedEmployerMedicare);
    }
}
