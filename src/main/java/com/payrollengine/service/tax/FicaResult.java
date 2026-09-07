package com.payrollengine.service.tax;

import java.math.BigDecimal;

public record FicaResult(
        BigDecimal socialSecurityTaxableWages,
        BigDecimal employeeSocialSecurity,
        BigDecimal employerSocialSecurity,
        BigDecimal employeeMedicare,
        BigDecimal employerMedicare,
        BigDecimal additionalMedicareEmployee
) {
    public BigDecimal totalEmployeeFica() {
        return employeeSocialSecurity.add(employeeMedicare).add(additionalMedicareEmployee);
    }

    public BigDecimal totalEmployerFica() {
        return employerSocialSecurity.add(employerMedicare);
    }
}
