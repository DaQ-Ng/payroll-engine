package com.payrollengine.service;

import com.payrollengine.domain.LedgerEntry;
import com.payrollengine.repository.LedgerEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    private LedgerService ledgerService;

    @Test
    void postedEntriesAlwaysBalanceRegardlessOfTheAmountsInvolved() {
        ledgerService = new LedgerService(ledgerEntryRepository);

        PayrollCalculationResult result = new PayrollCalculationResult(
                new BigDecimal("40.00"), BigDecimal.ZERO,
                new BigDecimal("4000.00"),   // grossPay
                new BigDecimal("450.00"),    // federalWithholding
                new BigDecimal("248.00"),    // ssEmployee
                new BigDecimal("248.00"),    // ssEmployer
                new BigDecimal("58.00"),     // medicareEmployee
                new BigDecimal("58.00"),     // medicareEmployer
                BigDecimal.ZERO,             // additionalMedicareEmployee
                new BigDecimal("3244.00"),   // netPay = 4000 - 450 - 248 - 58
                new BigDecimal("4000.00"), new BigDecimal("4000.00"));

        ledgerService.postPayrollEntries(1L, 42L, result);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(ledgerEntryRepository).saveAll(captor.capture());

        List<LedgerEntry> entries = captor.getValue();
        BigDecimal totalDebits = entries.stream().map(LedgerEntry::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = entries.stream().map(LedgerEntry::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(totalDebits).isEqualByComparingTo(totalCredits);
    }

    @Test
    void verifyGloballyBalancedComparesRepositoryTotals() {
        ledgerService = new LedgerService(ledgerEntryRepository);
        when(ledgerEntryRepository.sumAllDebits()).thenReturn(new BigDecimal("500.00"));
        when(ledgerEntryRepository.sumAllCredits()).thenReturn(new BigDecimal("500.00"));

        assertThat(ledgerService.verifyGloballyBalanced()).isTrue();
    }

    @Test
    void verifyGloballyBalancedDetectsAnImbalance() {
        ledgerService = new LedgerService(ledgerEntryRepository);
        when(ledgerEntryRepository.sumAllDebits()).thenReturn(new BigDecimal("500.00"));
        when(ledgerEntryRepository.sumAllCredits()).thenReturn(new BigDecimal("499.00"));

        assertThat(ledgerService.verifyGloballyBalanced()).isFalse();
    }
}
