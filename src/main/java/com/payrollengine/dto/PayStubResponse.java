package com.payrollengine.dto;

import com.payrollengine.domain.PayStub;

import java.math.BigDecimal;

public record PayStubResponse(
        Long id,
        Long employeeId,
        String employeeName,
        BigDecimal regularHours,
        BigDecimal overtimeHours,
        BigDecimal grossPay,
        BigDecimal federalWithholding,
        BigDecimal socialSecurityEmployee,
        BigDecimal medicareEmployee,
        BigDecimal additionalMedicareEmployee,
        BigDecimal netPay
) {
    public static PayStubResponse from(PayStub stub, String employeeName) {
        return new PayStubResponse(
                stub.getId(), stub.getEmployeeId(), employeeName,
                stub.getRegularHours(), stub.getOvertimeHours(), stub.getGrossPay(),
                stub.getFederalWithholding(), stub.getSocialSecurityEmployee(),
                stub.getMedicareEmployee(), stub.getAdditionalMedicareEmployee(), stub.getNetPay()
        );
    }
}
