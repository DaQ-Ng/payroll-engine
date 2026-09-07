package com.payrollengine.repository;

import com.payrollengine.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByPayRunId(Long payRunId);

    @Query("select coalesce(sum(l.debitAmount), 0) from LedgerEntry l")
    BigDecimal sumAllDebits();

    @Query("select coalesce(sum(l.creditAmount), 0) from LedgerEntry l")
    BigDecimal sumAllCredits();
}
