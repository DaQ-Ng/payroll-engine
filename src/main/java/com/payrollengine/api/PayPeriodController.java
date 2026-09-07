package com.payrollengine.api;

import com.payrollengine.domain.PayPeriod;
import com.payrollengine.dto.PayPeriodRequest;
import com.payrollengine.dto.PayPeriodResponse;
import com.payrollengine.repository.PayPeriodRepository;
import com.payrollengine.service.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/pay-periods")
public class PayPeriodController {

    private final PayPeriodRepository payPeriodRepository;

    public PayPeriodController(PayPeriodRepository payPeriodRepository) {
        this.payPeriodRepository = payPeriodRepository;
    }

    @PostMapping
    public ResponseEntity<PayPeriodResponse> create(@Valid @RequestBody PayPeriodRequest request) {
        PayPeriod period = new PayPeriod(request.startDate(), request.endDate(), request.payDate());
        payPeriodRepository.save(period);
        return ResponseEntity.created(URI.create("/api/pay-periods/" + period.getId()))
                .body(PayPeriodResponse.from(period));
    }

    @GetMapping("/{id}")
    public PayPeriodResponse get(@PathVariable Long id) {
        return payPeriodRepository.findById(id)
                .map(PayPeriodResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No pay period with id " + id));
    }

    @GetMapping
    public List<PayPeriodResponse> list() {
        return payPeriodRepository.findAll().stream().map(PayPeriodResponse::from).toList();
    }
}
