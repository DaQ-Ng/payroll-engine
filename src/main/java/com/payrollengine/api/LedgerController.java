package com.payrollengine.api;

import com.payrollengine.dto.LedgerBalanceResponse;
import com.payrollengine.dto.LedgerEntryResponse;
import com.payrollengine.repository.LedgerEntryRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerController(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @GetMapping("/entries")
    public List<LedgerEntryResponse> entriesForRun(@RequestParam Long payRunId) {
        return ledgerEntryRepository.findByPayRunId(payRunId).stream()
                .map(LedgerEntryResponse::from)
                .toList();
    }

    /** System-health check: every debit ever posted should still equal every
     * credit ever posted. A false here means a calculation bug, not just a
     * bookkeeping error — see LedgerService's class doc. */
    @GetMapping("/verify")
    public LedgerBalanceResponse verify() {
        var debits = ledgerEntryRepository.sumAllDebits();
        var credits = ledgerEntryRepository.sumAllCredits();
        return new LedgerBalanceResponse(debits, credits, debits.compareTo(credits) == 0);
    }
}
