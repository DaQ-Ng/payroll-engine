package com.payrollengine.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "ledger_accounts")
public class LedgerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private LedgerAccountCode code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerAccountType type;

    @Column(nullable = false)
    private String displayName;

    protected LedgerAccount() {
        // JPA
    }

    public LedgerAccount(LedgerAccountCode code, LedgerAccountType type, String displayName) {
        this.code = code;
        this.type = type;
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
    }

    public LedgerAccountCode getCode() {
        return code;
    }

    public LedgerAccountType getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }
}
