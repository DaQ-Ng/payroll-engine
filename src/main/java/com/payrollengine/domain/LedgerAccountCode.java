package com.payrollengine.domain;

/** Fixed chart of accounts, seeded by a Flyway migration. Using an enum (rather
 * than free-text account names) means an invalid account code is a compile-time
 * error, not a runtime data-quality bug. */
public enum LedgerAccountCode {
    CASH,
    PAYROLL_EXPENSE,
    EMPLOYER_PAYROLL_TAX_EXPENSE,
    FEDERAL_WITHHOLDING_PAYABLE,
    FICA_PAYABLE
}
