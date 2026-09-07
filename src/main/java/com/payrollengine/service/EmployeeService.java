package com.payrollengine.service;

import com.payrollengine.domain.Employee;
import com.payrollengine.domain.EmploymentType;
import com.payrollengine.domain.PayStub;
import com.payrollengine.dto.EmployeeRequest;
import com.payrollengine.repository.EmployeeRepository;
import com.payrollengine.repository.PayStubRepository;
import com.payrollengine.service.exception.InvalidEmployeeConfigurationException;
import com.payrollengine.service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PayStubRepository payStubRepository;

    public EmployeeService(EmployeeRepository employeeRepository, PayStubRepository payStubRepository) {
        this.employeeRepository = employeeRepository;
        this.payStubRepository = payStubRepository;
    }

    public Employee create(EmployeeRequest request) {
        if (request.employmentType() == EmploymentType.SALARY && request.annualSalary() == null) {
            throw new InvalidEmployeeConfigurationException("annualSalary is required for SALARY employees");
        }
        if (request.employmentType() == EmploymentType.HOURLY && request.hourlyRate() == null) {
            throw new InvalidEmployeeConfigurationException("hourlyRate is required for HOURLY employees");
        }

        Employee employee = new Employee(
                request.firstName(), request.lastName(), request.ssnLastFour(), request.filingStatus(),
                request.employmentType(), request.payFrequency(), request.annualSalary(), request.hourlyRate());
        return employeeRepository.save(employee);
    }

    public Employee get(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No employee with id " + id));
    }

    public List<Employee> list() {
        return employeeRepository.findAll();
    }

    public List<PayStub> payStubsFor(Long employeeId) {
        get(employeeId); // 404s if the employee doesn't exist
        return payStubRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
    }
}
