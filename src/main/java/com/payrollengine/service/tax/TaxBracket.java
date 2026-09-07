package com.payrollengine.service.tax;

import java.math.BigDecimal;

/** One marginal-rate bracket: {@code rate} applies to annual income above
 * {@code lowerBound}, up to the next bracket's lowerBound (or unbounded for
 * the top bracket). */
public record TaxBracket(BigDecimal lowerBound, BigDecimal rate) {
}
