package com.payrollengine.integration;

import com.payrollengine.domain.Employee;
import com.payrollengine.domain.EmploymentType;
import com.payrollengine.domain.FilingStatus;
import com.payrollengine.domain.PayFrequency;
import com.payrollengine.domain.PayPeriod;
import com.payrollengine.dto.PayRunRequest;
import com.payrollengine.dto.PayRunResponse;
import com.payrollengine.repository.EmployeeRepository;
import com.payrollengine.repository.PayPeriodRepository;
import com.payrollengine.repository.PayStubRepository;
import com.payrollengine.service.PayrollRunService;
import com.payrollengine.service.exception.PayRunInProgressException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the idempotency guarantee under actual concurrent load, not just
 * "call the method twice sequentially." Two threads race to process the
 * same pay period; the DB unique constraint (see PayRunClaimService) must
 * let exactly one of them do the real work.
 */
@SpringBootTest
class PayrollRunConcurrencyTest {

    @Autowired
    private PayrollRunService payrollRunService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PayPeriodRepository payPeriodRepository;

    @Autowired
    private PayStubRepository payStubRepository;

    @BeforeEach
    void deactivateLeftoverEmployees() {
        employeeRepository.findAll().forEach(e -> {
            e.setActive(false);
            employeeRepository.save(e);
        });
    }

    @Test
    void twoConcurrentRequestsForTheSamePeriodNeverDoubleProcess() throws Exception {
        Employee employee = employeeRepository.save(new Employee(
                "Rosalind", "Franklin", "5555", FilingStatus.SINGLE, EmploymentType.SALARY,
                PayFrequency.BIWEEKLY, new BigDecimal("78000.00"), null));

        PayPeriod period = payPeriodRepository.save(new PayPeriod(
                LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 14), LocalDate.of(2024, 4, 19)));

        PayRunRequest request = new PayRunRequest(period.getId(), List.of());

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startingGun = new CountDownLatch(1);
        CountDownLatch bothReady = new CountDownLatch(threadCount);
        AtomicInteger nonReplaySuccesses = new AtomicInteger(0);
        AtomicInteger replaySuccesses = new AtomicInteger(0);
        AtomicInteger conflicts = new AtomicInteger(0);

        List<Callable<Void>> tasks = List.of(
                raceTask(request, startingGun, bothReady, nonReplaySuccesses, replaySuccesses, conflicts),
                raceTask(request, startingGun, bothReady, nonReplaySuccesses, replaySuccesses, conflicts)
        );

        List<Future<Void>> futures = tasks.stream().map(executor::submit).toList();
        bothReady.await(5, TimeUnit.SECONDS);
        startingGun.countDown(); // release both threads at once to maximize the chance of a real race

        for (Future<Void> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Exactly one thread should have actually processed the run.
        assertThat(nonReplaySuccesses.get()).isEqualTo(1);
        // The other should have either seen the completed run (replay) or
        // hit the documented in-progress race and retried into a replay —
        // raceTask() retries on conflict, so by the time we get here it
        // must have converged to a replay rather than a hard failure.
        assertThat(replaySuccesses.get()).isEqualTo(1);

        // The real proof: exactly one pay stub for this employee, and YTD
        // wages advanced exactly once, no matter how the race resolved.
        assertThat(payStubRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId())).hasSize(1);
        Employee reloaded = employeeRepository.findById(employee.getId()).orElseThrow();
        assertThat(reloaded.getYtdGrossWages()).isEqualByComparingTo("3000.00"); // 78000 / 26, exactly once
    }

    private Callable<Void> raceTask(PayRunRequest request, CountDownLatch startingGun, CountDownLatch bothReady,
                                     AtomicInteger nonReplaySuccesses, AtomicInteger replaySuccesses,
                                     AtomicInteger conflicts) {
        return () -> {
            bothReady.countDown();
            startingGun.await();

            for (int attempt = 0; attempt < 10; attempt++) {
                try {
                    PayRunResponse response = payrollRunService.createPayRun(request);
                    if (response.idempotentReplay()) {
                        replaySuccesses.incrementAndGet();
                    } else {
                        nonReplaySuccesses.incrementAndGet();
                    }
                    return null;
                } catch (PayRunInProgressException e) {
                    conflicts.incrementAndGet();
                    Thread.sleep(50); // the documented narrow window; a real client retries
                }
            }
            throw new IllegalStateException("Did not converge after 10 retries");
        };
    }
}
