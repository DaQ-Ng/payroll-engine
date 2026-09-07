package com.payrollengine.dto;

import com.payrollengine.domain.PayPeriod;
import com.payrollengine.domain.PayPeriodStatus;

import java.time.LocalDate;

public record PayPeriodResponse(Long id, LocalDate startDate, LocalDate endDate, LocalDate payDate,
                                 PayPeriodStatus status) {
    public static PayPeriodResponse from(PayPeriod p) {
        return new PayPeriodResponse(p.getId(), p.getStartDate(), p.getEndDate(), p.getPayDate(), p.getStatus());
    }
}
