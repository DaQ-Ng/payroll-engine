package com.payrollengine.api;

import com.payrollengine.dto.EmployeeRequest;
import com.payrollengine.dto.EmployeeResponse;
import com.payrollengine.dto.PayStubResponse;
import com.payrollengine.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        var employee = employeeService.create(request);
        var response = EmployeeResponse.from(employee);
        return ResponseEntity.created(URI.create("/api/employees/" + employee.getId())).body(response);
    }

    @GetMapping("/{id}")
    public EmployeeResponse get(@PathVariable Long id) {
        return EmployeeResponse.from(employeeService.get(id));
    }

    @GetMapping
    public List<EmployeeResponse> list() {
        return employeeService.list().stream().map(EmployeeResponse::from).toList();
    }

    @GetMapping("/{id}/pay-stubs")
    public List<PayStubResponse> payStubs(@PathVariable Long id) {
        var employee = employeeService.get(id);
        String name = employee.getFirstName() + " " + employee.getLastName();
        return employeeService.payStubsFor(id).stream()
                .map(stub -> PayStubResponse.from(stub, name))
                .toList();
    }
}
