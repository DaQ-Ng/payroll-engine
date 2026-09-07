package com.payrollengine.service.tax;

import com.payrollengine.domain.FilingStatus;
import com.payrollengine.domain.PayFrequency;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FederalWithholdingCalculatorTest {

    private final FederalWithholdingCalculator calculator = new FederalWithholdingCalculator();

    @Test
    void zeroIncomeMeansZeroWithholding() {
        BigDecimal tax = calculator.calculateAnnualTax(BigDecimal.ZERO, FilingStatus.SINGLE);
        assertThat(tax).isEqualByComparingTo("0.00");
    }

    @Test
    void incomeWithinZeroBracketIsNotTaxed() {
        // Single's 0% bracket runs up to 6,000.
        BigDecimal tax = calculator.calculateAnnualTax(new BigDecimal("5000"), FilingStatus.SINGLE);
        assertThat(tax).isEqualByComparingTo("0.00");
    }

    @Test
    void taxIsProgressiveNotFlatOnTheTopBracket() {
        // At exactly the top of the 10% bracket (17,600 for Single), tax
        // should be 10% of the amount in that bracket only (17,600 - 6,000),
        // not 10% of the full 17,600.
        BigDecimal tax = calculator.calculateAnnualTax(new BigDecimal("17600"), FilingStatus.SINGLE);
        BigDecimal expected = new BigDecimal("17600").subtract(new BigDecimal("6000"))
                .multiply(new BigDecimal("0.10"));
        assertThat(tax).isEqualByComparingTo(expected.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    void higherIncomeAccumulatesTaxAcrossMultipleBrackets() {
        BigDecimal lowerTax = calculator.calculateAnnualTax(new BigDecimal("50000"), FilingStatus.SINGLE);
        BigDecimal higherTax = calculator.calculateAnnualTax(new BigDecimal("150000"), FilingStatus.SINGLE);
        assertThat(higherTax).isGreaterThan(lowerTax);
    }

    @Test
    void marriedFilingJointlyOwesLessThanSingleAtTheSameIncome() {
        BigDecimal single = calculator.calculateAnnualTax(new BigDecimal("80000"), FilingStatus.SINGLE);
        BigDecimal married = calculator.calculateAnnualTax(new BigDecimal("80000"), FilingStatus.MARRIED_FILING_JOINTLY);
        assertThat(married).isLessThan(single);
    }

    @Test
    void periodWithholdingAnnualizesAndDeAnnualizesConsistently() {
        // A biweekly paycheck of $4,000 annualizes to $104,000/year.
        BigDecimal periodWithholding = calculator.calculatePeriodWithholding(
                new BigDecimal("4000.00"), FilingStatus.SINGLE, PayFrequency.BIWEEKLY);
        BigDecimal annualTaxAtThatRate = calculator.calculateAnnualTax(new BigDecimal("104000"), FilingStatus.SINGLE);
        BigDecimal expectedPerPeriod = annualTaxAtThatRate.divide(new BigDecimal("26"), 2, java.math.RoundingMode.HALF_UP);

        assertThat(periodWithholding).isEqualByComparingTo(expectedPerPeriod);
    }

    @Test
    void higherPayFrequencyProducesSmallerPerPeriodWithholdingForSameAnnualRate() {
        // Same effective annual wage ($104,000/yr), sliced into more periods.
        BigDecimal biweekly = calculator.calculatePeriodWithholding(
                new BigDecimal("4000.00"), FilingStatus.SINGLE, PayFrequency.BIWEEKLY);
        BigDecimal weekly = calculator.calculatePeriodWithholding(
                new BigDecimal("2000.00"), FilingStatus.SINGLE, PayFrequency.WEEKLY);

        assertThat(weekly).isLessThan(biweekly);
    }
}
