package com.payrollengine.dto;

import com.payrollengine.domain.LedgerAccountCode;
import com.payrollengine.domain.LedgerEntry;

import java.math.BigDecimal;

public record LedgerEntryResponse(
        Long id,
        Long payRunId,
        Long employeeId,
        LedgerAccountCode accountCode,
        BigDecimal debitAmount,
        BigDecimal creditAmount,
        String description
) {
    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(entry.getId(), entry.getPayRunId(), entry.getEmployeeId(),
                entry.getAccountCode(), entry.getDebitAmount(), entry.getCreditAmount(), entry.getDescription());
    }
}
