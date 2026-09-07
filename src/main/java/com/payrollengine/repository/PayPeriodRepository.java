package com.payrollengine.repository;

import com.payrollengine.domain.PayPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayPeriodRepository extends JpaRepository<PayPeriod, Long> {
}
