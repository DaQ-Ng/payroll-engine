package com.payrollengine.service;

import com.payrollengine.domain.PayPeriod;
import com.payrollengine.domain.PayRun;
import com.payrollengine.domain.PayRunStatus;
import com.payrollengine.dto.PayRunRequest;
import com.payrollengine.dto.PayRunResponse;
import com.payrollengine.dto.PayStubResponse;
import com.payrollengine.repository.EmployeeRepository;
import com.payrollengine.repository.PayPeriodRepository;
import com.payrollengine.repository.PayRunRepository;
import com.payrollengine.repository.PayStubRepository;
import com.payrollengine.service.exception.PayRunInProgressException;
import com.payrollengine.service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Entry point for triggering a payroll run. Deliberately holds no
 * {@code @Transactional} of its own: it coordinates two separate
 * transactional collaborators — {@link PayRunClaimService} (a fast,
 * independent claim on the pay period) and {@link PayrollProcessingService}
 * (the actual computation) — and reacting to a failure in the second by
 * compensating the first (marking the claim FAILED) only makes sense if
 * they're genuinely separate transactions.
 */
@Service
public class PayrollRunService {

    private final PayPeriodRepository payPeriodRepository;
    private final PayRunRepository payRunRepository;
    private final EmployeeRepository employeeRepository;
    private final PayStubRepository payStubRepository;
    private final PayRunClaimService claimService;
    private final PayrollProcessingService processingService;

    public PayrollRunService(PayPeriodRepository payPeriodRepository, PayRunRepository payRunRepository,
                              EmployeeRepository employeeRepository, PayStubRepository payStubRepository,
                              PayRunClaimService claimService, PayrollProcessingService processingService) {
        this.payPeriodRepository = payPeriodRepository;
        this.payRunRepository = payRunRepository;
        this.employeeRepository = employeeRepository;
        this.payStubRepository = payStubRepository;
        this.claimService = claimService;
        this.processingService = processingService;
    }

    public PayRunResponse createPayRun(PayRunRequest request) {
        PayPeriod payPeriod = payPeriodRepository.findById(request.payPeriodId())
                .orElseThrow(() -> new ResourceNotFoundException("No pay period with id " + request.payPeriodId()));

        Optional<PayRun> existing = payRunRepository.findByPayPeriodId(payPeriod.getId());
        if (existing.isPresent() && existing.get().getStatus() != PayRunStatus.FAILED) {
            return handleExisting(existing.get());
        }

        Optional<Long> claimedId = claimService.claim(payPeriod.getId());
        if (claimedId.isEmpty()) {
            // Lost the race (or our delete-then-insert collided with a
            // concurrent claim) — see what the winner left behind.
            PayRun winner = payRunRepository.findByPayPeriodId(payPeriod.getId())
                    .orElseThrow(() -> new PayRunInProgressException(
                            "Pay run for period " + payPeriod.getId() + " is being claimed by another request; retry shortly."));
            return handleExisting(winner);
        }

        try {
            return processingService.process(claimedId.get(), payPeriod.getId(), request.timeEntries());
        } catch (RuntimeException e) {
            claimService.markFailed(claimedId.get());
            throw e;
        }
    }

    public PayRunResponse getPayRun(Long payRunId) {
        PayRun run = payRunRepository.findById(payRunId)
                .orElseThrow(() -> new ResourceNotFoundException("No pay run with id " + payRunId));
        return PayRunResponse.from(run, false, buildStubResponses(run.getId()));
    }

    private PayRunResponse handleExisting(PayRun run) {
        if (run.getStatus() == PayRunStatus.COMPLETED) {
            List<PayStubResponse> stubs = buildStubResponses(run.getId());
            return PayRunResponse.from(run, true, stubs);
        }
        throw new PayRunInProgressException(
                "Pay run " + run.getId() + " for period " + run.getPayPeriodId() +
                        " is currently " + run.getStatus() + "; retry shortly.");
    }

    private List<PayStubResponse> buildStubResponses(Long payRunId) {
        return payStubRepository.findByPayRunId(payRunId).stream()
                .map(stub -> {
                    String name = employeeRepository.findById(stub.getEmployeeId())
                            .map(e -> e.getFirstName() + " " + e.getLastName())
                            .orElse("Unknown");
                    return PayStubResponse.from(stub, name);
                })
                .toList();
    }
}
