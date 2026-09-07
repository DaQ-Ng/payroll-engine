package com.payrollengine.api;

import com.payrollengine.dto.PayRunRequest;
import com.payrollengine.dto.PayRunResponse;
import com.payrollengine.service.PayrollRunService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pay-runs")
public class PayRunController {

    private final PayrollRunService payrollRunService;

    public PayRunController(PayrollRunService payrollRunService) {
        this.payrollRunService = payrollRunService;
    }

    /**
     * Trigger payroll for a pay period. Idempotent: calling this twice for
     * the same payPeriodId returns the same result both times (the second
     * response has {@code idempotentReplay: true}) rather than double-paying
     * anyone — see PayrollRunService for how.
     */
    @PostMapping
    public ResponseEntity<PayRunResponse> create(@Valid @RequestBody PayRunRequest request) {
        PayRunResponse response = payrollRunService.createPayRun(request);
        HttpStatus status = response.idempotentReplay() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/{id}")
    public PayRunResponse get(@PathVariable Long id) {
        return payrollRunService.getPayRun(id);
    }
}
