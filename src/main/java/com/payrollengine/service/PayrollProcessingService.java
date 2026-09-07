package com.payrollengine.service;

import com.payrollengine.domain.Employee;
import com.payrollengine.domain.EmploymentType;
import com.payrollengine.domain.PayPeriod;
import com.payrollengine.domain.PayRun;
import com.payrollengine.domain.PayStub;
import com.payrollengine.dto.PayRunResponse;
import com.payrollengine.dto.PayStubResponse;
import com.payrollengine.dto.TimeEntryRequest;
import com.payrollengine.repository.EmployeeRepository;
import com.payrollengine.repository.PayPeriodRepository;
import com.payrollengine.repository.PayRunRepository;
import com.payrollengine.repository.PayStubRepository;
import com.payrollengine.service.exception.InvalidEmployeeConfigurationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The actual pay-computation transaction, kept in its own bean (rather than
 * a method on PayrollRunService) so {@code @Transactional} is applied via
 * Spring's proxy on a real external call — calling an {@code @Transactional}
 * method on {@code this} from within the same class silently skips the
 * proxy and runs with no transaction at all, a classic Spring AOP trap.
 */
@Service
public class PayrollProcessingService {

    private final PayPeriodRepository payPeriodRepository;
    private final PayRunRepository payRunRepository;
    private final EmployeeRepository employeeRepository;
    private final PayStubRepository payStubRepository;
    private final PayrollCalculationService calculationService;
    private final LedgerService ledgerService;

    public PayrollProcessingService(PayPeriodRepository payPeriodRepository, PayRunRepository payRunRepository,
                                     EmployeeRepository employeeRepository, PayStubRepository payStubRepository,
                                     PayrollCalculationService calculationService, LedgerService ledgerService) {
        this.payPeriodRepository = payPeriodRepository;
        this.payRunRepository = payRunRepository;
        this.employeeRepository = employeeRepository;
        this.payStubRepository = payStubRepository;
        this.calculationService = calculationService;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public PayRunResponse process(Long payRunId, Long payPeriodId, List<TimeEntryRequest> timeEntries) {
        PayRun payRun = payRunRepository.findById(payRunId)
                .orElseThrow(() -> new IllegalStateException("Claimed pay run " + payRunId + " vanished"));
        PayPeriod payPeriod = payPeriodRepository.findById(payPeriodId)
                .orElseThrow(() -> new IllegalStateException("Pay period " + payPeriodId + " vanished"));

        Map<Long, TimeEntryRequest> timeEntriesByEmployee = (timeEntries == null ? List.<TimeEntryRequest>of() : timeEntries)
                .stream()
                .collect(Collectors.toMap(TimeEntryRequest::employeeId, Function.identity()));

        List<Employee> activeEmployees = employeeRepository.findByActiveTrue();
        List<PayStubResponse> responses = new ArrayList<>();

        for (Employee employeeStub : activeEmployees) {
            // Re-fetch with a pessimistic lock: belt-and-suspenders against a
            // second pay run somehow racing to update the same employee's
            // YTD figures concurrently.
            Employee employee = employeeRepository.findWithLockById(employeeStub.getId())
                    .orElseThrow(() -> new IllegalStateException("Employee vanished mid-run"));

            BigDecimal regularHours = BigDecimal.ZERO;
            BigDecimal overtimeHours = BigDecimal.ZERO;
            if (employee.getEmploymentType() == EmploymentType.HOURLY) {
                TimeEntryRequest entry = timeEntriesByEmployee.get(employee.getId());
                if (entry == null) {
                    throw new InvalidEmployeeConfigurationException(
                            "Hourly employee " + employee.getId() + " has no time entry for this pay run");
                }
                regularHours = entry.regularHours();
                overtimeHours = entry.overtimeHours();
            }

            PayrollCalculationResult result = calculationService.calculate(employee, regularHours, overtimeHours);

            PayStub stub = new PayStub(
                    payRun.getId(), employee.getId(), result.regularHours(), result.overtimeHours(),
                    result.grossPay(), result.federalWithholding(), result.socialSecurityEmployee(),
                    result.medicareEmployee(), result.additionalMedicareEmployee(),
                    result.socialSecurityEmployer(), result.medicareEmployer(), result.netPay());
            payStubRepository.save(stub);

            ledgerService.postPayrollEntries(payRun.getId(), employee.getId(), result);

            employee.applyYtdDelta(result.grossPay(), result.ytdSocialSecurityWageDelta(),
                    result.ytdMedicareWageDelta(), result.federalWithholding());
            employeeRepository.save(employee);

            responses.add(PayStubResponse.from(stub, employee.getFirstName() + " " + employee.getLastName()));
        }

        payRun.markCompleted();
        payRunRepository.save(payRun);
        payPeriod.markProcessed();
        payPeriodRepository.save(payPeriod);

        return PayRunResponse.from(payRun, false, responses);
    }
}
