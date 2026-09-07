package com.payrollengine.service;

import com.payrollengine.domain.PayRun;
import com.payrollengine.domain.PayRunStatus;
import com.payrollengine.repository.PayRunRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.Optional;

/**
 * The actual idempotency guarantee for pay runs, isolated into its own
 * short-lived transactions on purpose.
 *
 * <p>{@link #claim} inserts a PROCESSING row and relies on the unique
 * constraint on {@code pay_runs.pay_period_id} (see V1 migration) to let
 * exactly one of two concurrent callers win. It runs in
 * {@code REQUIRES_NEW} rather than joining the caller's transaction for two
 * reasons: (1) after a JPA/Hibernate constraint-violation flush failure the
 * persistence context is no longer safely usable, so the loser must roll
 * back its own small transaction rather than poison a larger one; and (2)
 * the claim needs to be visible to other transactions (and durable)
 * immediately, independent of whether the caller's subsequent payroll
 * processing succeeds — which is exactly why a failure afterward needs an
 * explicit {@link #markFailed} compensating write instead of relying on
 * rollback to clean it up.
 */
@Service
public class PayRunClaimService {

    private final PayRunRepository payRunRepository;

    public PayRunClaimService(PayRunRepository payRunRepository) {
        this.payRunRepository = payRunRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Long> claim(Long payPeriodId) {
        // A previous attempt for this period failed part-way through; its
        // PayRun row is orphaned at FAILED (see markFailed) and must be
        // cleared before a fresh attempt can claim the period.
        payRunRepository.findByPayPeriodId(payPeriodId)
                .filter(run -> run.getStatus() == PayRunStatus.FAILED)
                .ifPresent(run -> {
                    payRunRepository.delete(run);
                    payRunRepository.flush();
                });

        try {
            PayRun run = new PayRun(payPeriodId);
            payRunRepository.saveAndFlush(run);
            return Optional.of(run.getId());
        } catch (DataIntegrityViolationException e) {
            // Someone else's PayRun row for this period committed first.
            // This transaction must not attempt any further work, so mark
            // it rollback-only and let the caller re-read the winner's row
            // in a fresh transaction.
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Optional.empty();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long payRunId) {
        payRunRepository.findById(payRunId).ifPresent(run -> {
            run.markFailed();
            payRunRepository.save(run);
        });
    }
}
