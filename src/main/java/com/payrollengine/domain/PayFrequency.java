package com.payrollengine.domain;

/** Number of pay periods per year, used to annualize wages for tax calculations
 * and to pro-rate salaried employees' per-period gross pay. */
public enum PayFrequency {
    WEEKLY(52),
    BIWEEKLY(26),
    SEMIMONTHLY(24),
    MONTHLY(12);

    private final int periodsPerYear;

    PayFrequency(int periodsPerYear) {
        this.periodsPerYear = periodsPerYear;
    }

    public int getPeriodsPerYear() {
        return periodsPerYear;
    }
}
