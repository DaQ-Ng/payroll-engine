package com.payrollengine.dto;

import com.payrollengine.domain.PayRun;
import com.payrollengine.domain.PayRunStatus;

import java.time.Instant;
import java.util.List;

public record PayRunResponse(
        Long id,
        Long payPeriodId,
        PayRunStatus status,
        boolean idempotentReplay,
        Instant triggeredAt,
        Instant completedAt,
        List<PayStubResponse> payStubs
) {
    public static PayRunResponse from(PayRun run, boolean idempotentReplay, List<PayStubResponse> stubs) {
        return new PayRunResponse(run.getId(), run.getPayPeriodId(), run.getStatus(), idempotentReplay,
                run.getTriggeredAt(), run.getCompletedAt(), stubs);
    }
}
