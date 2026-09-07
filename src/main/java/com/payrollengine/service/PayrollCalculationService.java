package com.payrollengine.service;

import com.payrollengine.domain.Employee;
import com.payrollengine.domain.EmploymentType;
import com.payrollengine.service.tax.FederalWithholdingCalculator;
import com.payrollengine.service.tax.FicaCalculator;
import com.payrollengine.service.tax.FicaResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Computes gross-to-net pay for one employee for one pay period. Reads the
 * employee's pre-run YTD figures but never mutates them — the caller
 * (PayrollRunService) applies the YTD deltas only after the whole run
 * commits, which is what keeps a retried/failed run from double-counting. */
@Service
public class PayrollCalculationService {

    private final FederalWithholdingCalculator federalWithholdingCalculator;
    private final FicaCalculator ficaCalculator;

    public PayrollCalculationService(FederalWithholdingCalculator federalWithholdingCalculator,
                                      FicaCalculator ficaCalculator) {
        this.federalWithholdingCalculator = federalWithholdingCalculator;
        this.ficaCalculator = ficaCalculator;
    }

    public PayrollCalculationResult calculate(Employee employee, BigDecimal regularHours, BigDecimal overtimeHours) {
        BigDecimal grossPay = calculateGrossPay(employee, regularHours, overtimeHours);

        BigDecimal federalWithholding = federalWithholdingCalculator.calculatePeriodWithholding(
                grossPay, employee.getFilingStatus(), employee.getPayFrequency());

        FicaResult fica = ficaCalculator.calculate(
                grossPay, employee.getYtdSocialSecurityWages(), employee.getYtdMedicareWages());

        BigDecimal netPay = grossPay
                .subtract(federalWithholding)
                .subtract(fica.employeeSocialSecurity())
                .subtract(fica.employeeMedicare())
                .subtract(fica.additionalMedicareEmployee())
                .setScale(2, RoundingMode.HALF_UP);

        return new PayrollCalculationResult(
                regularHours,
                overtimeHours,
                grossPay,
                federalWithholding,
                fica.employeeSocialSecurity(),
                fica.employerSocialSecurity(),
                fica.employeeMedicare(),
                fica.employerMedicare(),
                fica.additionalMedicareEmployee(),
                netPay,
                fica.socialSecurityTaxableWages(),
                grossPay
        );
    }

    private BigDecimal calculateGrossPay(Employee employee, BigDecimal regularHours, BigDecimal overtimeHours) {
        if (employee.getEmploymentType() == EmploymentType.SALARY) {
            return employee.getAnnualSalary()
                    .divide(BigDecimal.valueOf(employee.getPayFrequency().getPeriodsPerYear()), 2, RoundingMode.HALF_UP);
        }

        BigDecimal rate = employee.getHourlyRate();
        BigDecimal overtimeRate = rate.multiply(BigDecimal.valueOf(1.5));
        return regularHours.multiply(rate)
                .add(overtimeHours.multiply(overtimeRate))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
