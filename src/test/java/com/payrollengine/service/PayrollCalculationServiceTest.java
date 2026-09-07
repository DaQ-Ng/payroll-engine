package com.payrollengine.service;

import com.payrollengine.domain.Employee;
import com.payrollengine.domain.EmploymentType;
import com.payrollengine.domain.FilingStatus;
import com.payrollengine.domain.PayFrequency;
import com.payrollengine.service.tax.FederalWithholdingCalculator;
import com.payrollengine.service.tax.FicaCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PayrollCalculationServiceTest {

    private PayrollCalculationService service;

    @BeforeEach
    void setUp() {
        service = new PayrollCalculationService(new FederalWithholdingCalculator(), new FicaCalculator());
    }

    @Test
    void salariedEmployeeGrossPayIsAnnualSalaryDividedByPeriodsPerYear() {
        Employee employee = new Employee("Ada", "Lovelace", "1234", FilingStatus.SINGLE,
                EmploymentType.SALARY, PayFrequency.BIWEEKLY, new BigDecimal("104000.00"), null);

        PayrollCalculationResult result = service.calculate(employee, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(result.grossPay()).isEqualByComparingTo("4000.00"); // 104000 / 26
    }

    @Test
    void hourlyEmployeeGrossPayIncludesTimeAndAHalfOvertime() {
        Employee employee = new Employee("Grace", "Hopper", "5678", FilingStatus.SINGLE,
                EmploymentType.HOURLY, PayFrequency.WEEKLY, null, new BigDecimal("40.00"));

        PayrollCalculationResult result = service.calculate(employee, new BigDecimal("40"), new BigDecimal("5"));

        // 40 * 40.00 + 5 * 40.00 * 1.5 = 1600 + 300 = 1900
        assertThat(result.grossPay()).isEqualByComparingTo("1900.00");
    }

    @Test
    void netPayEqualsGrossMinusAllEmployeeSideTaxes() {
        Employee employee = new Employee("Katherine", "Johnson", "9012", FilingStatus.MARRIED_FILING_JOINTLY,
                EmploymentType.SALARY, PayFrequency.MONTHLY, new BigDecimal("120000.00"), null);

        PayrollCalculationResult result = service.calculate(employee, BigDecimal.ZERO, BigDecimal.ZERO);

        BigDecimal expectedNet = result.grossPay()
                .subtract(result.federalWithholding())
                .subtract(result.socialSecurityEmployee())
                .subtract(result.medicareEmployee())
                .subtract(result.additionalMedicareEmployee());

        assertThat(result.netPay()).isEqualByComparingTo(expectedNet);
    }

    @Test
    void netPayIsAlwaysLessThanGrossPayWhenAnyTaxIsOwed() {
        Employee employee = new Employee("Marie", "Curie", "3456", FilingStatus.SINGLE,
                EmploymentType.HOURLY, PayFrequency.WEEKLY, null, new BigDecimal("60.00"));

        PayrollCalculationResult result = service.calculate(employee, new BigDecimal("40"), BigDecimal.ZERO);

        assertThat(result.netPay()).isLessThan(result.grossPay());
    }

    @Test
    void ytdDeltasReflectThisPeriodsWagesForAnEmployeeStartingTheYearFresh() {
        Employee employee = new Employee("Rosalind", "Franklin", "7890", FilingStatus.SINGLE,
                EmploymentType.SALARY, PayFrequency.BIWEEKLY, new BigDecimal("78000.00"), null);

        PayrollCalculationResult result = service.calculate(employee, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(result.ytdSocialSecurityWageDelta()).isEqualByComparingTo(result.grossPay());
        assertThat(result.ytdMedicareWageDelta()).isEqualByComparingTo(result.grossPay());
    }
}
