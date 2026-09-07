package com.payrollengine.service.tax;

import com.payrollengine.domain.FilingStatus;
import com.payrollengine.domain.PayFrequency;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Implements the structure of the IRS Publication 15-T "Percentage Method
 * for Automated Payroll Systems" (Standard withholding, post-2019 Form W-4,
 * no Step 2/3/4 adjustments modeled).
 *
 * <p><b>The bracket thresholds below are illustrative approximations of the
 * 2024 published tables, not verified official figures.</b> The point of
 * this class is to get the *algorithm* right — annualize the period wage,
 * apply progressive marginal brackets, de-annualize the result — which is
 * the part that's easy to get subtly wrong (e.g. applying the marginal rate
 * to the whole annual wage instead of just the amount above each threshold).
 * A production system would source the current table from the IRS
 * publication for the applicable tax year instead of hardcoding it.
 */
@Component
public class FederalWithholdingCalculator {

    private static final Map<FilingStatus, List<TaxBracket>> ANNUAL_BRACKETS = Map.of(
            FilingStatus.SINGLE, List.of(
                    bracket(0, 0.00),
                    bracket(6_000, 0.10),
                    bracket(17_600, 0.12),
                    bracket(53_150, 0.22),
                    bracket(106_525, 0.24),
                    bracket(197_300, 0.32),
                    bracket(250_525, 0.35),
                    bracket(626_350, 0.37)
            ),
            FilingStatus.MARRIED_FILING_JOINTLY, List.of(
                    bracket(0, 0.00),
                    bracket(17_100, 0.10),
                    bracket(29_200, 0.12),
                    bracket(83_550, 0.22),
                    bracket(178_650, 0.24),
                    bracket(340_100, 0.32),
                    bracket(431_900, 0.35),
                    bracket(647_850, 0.37)
            ),
            FilingStatus.HEAD_OF_HOUSEHOLD, List.of(
                    bracket(0, 0.00),
                    bracket(13_900, 0.10),
                    bracket(24_200, 0.12),
                    bracket(63_750, 0.22),
                    bracket(95_550, 0.24),
                    bracket(197_300, 0.32),
                    bracket(250_500, 0.35),
                    bracket(626_350, 0.37)
            )
    );

    private static TaxBracket bracket(long lowerBound, double rate) {
        return new TaxBracket(BigDecimal.valueOf(lowerBound), BigDecimal.valueOf(rate));
    }

    /** Withholding for a single pay period, rounded to the cent. */
    public BigDecimal calculatePeriodWithholding(BigDecimal periodGrossPay, FilingStatus filingStatus,
                                                  PayFrequency payFrequency) {
        BigDecimal periodsPerYear = BigDecimal.valueOf(payFrequency.getPeriodsPerYear());
        BigDecimal annualizedWages = periodGrossPay.multiply(periodsPerYear);

        BigDecimal annualTax = calculateAnnualTax(annualizedWages, filingStatus);

        return annualTax.divide(periodsPerYear, 2, RoundingMode.HALF_UP);
    }

    BigDecimal calculateAnnualTax(BigDecimal annualTaxableWages, FilingStatus filingStatus) {
        List<TaxBracket> brackets = ANNUAL_BRACKETS.get(filingStatus);
        BigDecimal tax = BigDecimal.ZERO;

        for (int i = 0; i < brackets.size(); i++) {
            BigDecimal lower = brackets.get(i).lowerBound();
            if (annualTaxableWages.compareTo(lower) <= 0) {
                break;
            }
            BigDecimal upper = (i + 1 < brackets.size()) ? brackets.get(i + 1).lowerBound() : null;
            BigDecimal bracketCeiling = (upper == null) ? annualTaxableWages : upper.min(annualTaxableWages);
            BigDecimal taxableInBracket = bracketCeiling.subtract(lower);
            if (taxableInBracket.signum() <= 0) {
                continue;
            }
            tax = tax.add(taxableInBracket.multiply(brackets.get(i).rate()));
        }

        return tax.setScale(2, RoundingMode.HALF_UP);
    }
}
