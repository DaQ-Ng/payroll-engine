package com.payrollengine.dto;

import java.math.BigDecimal;

public record LedgerBalanceResponse(BigDecimal totalDebits, BigDecimal totalCredits, boolean balanced) {
}
