package com.payrollengine.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PayRunRequest(
        @NotNull Long payPeriodId,
        @Valid List<TimeEntryRequest> timeEntries
) {
}
