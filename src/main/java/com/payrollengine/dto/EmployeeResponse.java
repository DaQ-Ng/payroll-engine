package com.payrollengine.dto;

import com.payrollengine.domain.Employee;
import com.payrollengine.domain.EmploymentType;
import com.payrollengine.domain.FilingStatus;
import com.payrollengine.domain.PayFrequency;

import java.math.BigDecimal;

public record EmployeeResponse(
        Long id,
        String firstName,
        String lastName,
        String ssnLastFour,
        FilingStatus filingStatus,
        EmploymentType employmentType,
        PayFrequency payFrequency,
        BigDecimal annualSalary,
        BigDecimal hourlyRate,
        boolean active,
        BigDecimal ytdGrossWages,
        BigDecimal ytdFederalWithholding
) {
    public static EmployeeResponse from(Employee e) {
        return new EmployeeResponse(
                e.getId(), e.getFirstName(), e.getLastName(), e.getSsnLastFour(),
                e.getFilingStatus(), e.getEmploymentType(), e.getPayFrequency(),
                e.getAnnualSalary(), e.getHourlyRate(), e.isActive(),
                e.getYtdGrossWages(), e.getYtdFederalWithholding()
        );
    }
}
