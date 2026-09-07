package com.payrollengine.service;

import com.payrollengine.domain.LedgerAccountCode;
import com.payrollengine.domain.LedgerEntry;
import com.payrollengine.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Posts balanced double-entry ledger entries for one employee's pay.
 *
 * <p>Per employee, per pay run:
 * <pre>
 *   Dr Payroll Expense                  grossPay
 *   Dr Employer Payroll Tax Expense     employerFica
 *   Cr Federal Withholding Payable                    federalWithholding
 *   Cr FICA Payable                                    employeeFica + employerFica
 *   Cr Cash                                            netPay
 * </pre>
 * Debits (grossPay + employerFica) always equal credits (federalWithholding +
 * employeeFica + employerFica + netPay) because netPay is defined as
 * {@code grossPay - federalWithholding - employeeFica}. That identity is
 * exactly what {@link #verifyGloballyBalanced()} checks — if it ever returns
 * false, there's a bug in the calculation, not just the bookkeeping.
 */
@Service
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    public void postPayrollEntries(Long payRunId, Long employeeId, PayrollCalculationResult result) {
        BigDecimal employeeFica = result.socialSecurityEmployee()
                .add(result.medicareEmployee())
                .add(result.additionalMedicareEmployee());
        BigDecimal employerFica = result.socialSecurityEmployer().add(result.medicareEmployer());
        BigDecimal totalFicaPayable = employeeFica.add(employerFica);

        List<LedgerEntry> entries = List.of(
                LedgerEntry.debit(payRunId, employeeId, LedgerAccountCode.PAYROLL_EXPENSE,
                        result.grossPay(), "Gross wages"),
                LedgerEntry.debit(payRunId, employeeId, LedgerAccountCode.EMPLOYER_PAYROLL_TAX_EXPENSE,
                        employerFica, "Employer FICA match"),
                LedgerEntry.credit(payRunId, employeeId, LedgerAccountCode.FEDERAL_WITHHOLDING_PAYABLE,
                        result.federalWithholding(), "Federal income tax withheld"),
                LedgerEntry.credit(payRunId, employeeId, LedgerAccountCode.FICA_PAYABLE,
                        totalFicaPayable, "Employee + employer FICA payable"),
                LedgerEntry.credit(payRunId, employeeId, LedgerAccountCode.CASH,
                        result.netPay(), "Net pay disbursed")
        );

        ledgerEntryRepository.saveAll(entries);
    }

    /** True iff every ledger entry ever posted still balances globally. Used
     * as a system-health check, not per-request — see LedgerController. */
    public boolean verifyGloballyBalanced() {
        return ledgerEntryRepository.sumAllDebits().compareTo(ledgerEntryRepository.sumAllCredits()) == 0;
    }
}
