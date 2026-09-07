package com.payrollengine.repository;

import com.payrollengine.domain.PayRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayRunRepository extends JpaRepository<PayRun, Long> {

    Optional<PayRun> findByPayPeriodId(Long payPeriodId);
}
