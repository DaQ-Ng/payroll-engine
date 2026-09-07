package com.payrollengine.repository;

import com.payrollengine.domain.PayStub;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayStubRepository extends JpaRepository<PayStub, Long> {

    List<PayStub> findByPayRunId(Long payRunId);

    List<PayStub> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
}
