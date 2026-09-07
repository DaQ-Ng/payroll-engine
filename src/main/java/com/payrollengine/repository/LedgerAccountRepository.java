package com.payrollengine.repository;

import com.payrollengine.domain.LedgerAccount;
import com.payrollengine.domain.LedgerAccountCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {

    Optional<LedgerAccount> findByCode(LedgerAccountCode code);
}
