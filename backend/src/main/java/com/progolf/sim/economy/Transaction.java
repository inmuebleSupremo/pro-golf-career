package com.progolf.sim.economy;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A single immutable, auditable entry in a {@link FinancialAccount}'s ledger (spec: financial-identity,
 * REQ-184). {@code amount} is signed — positive for money in, negative for money out — and
 * {@code resultingBalance} is the account's available funds immediately after this entry, so the ledger
 * fully reconstructs the account's financial history.
 */
public record Transaction(
        int sequence,
        LocalDate date,
        TransactionType type,
        double amount,
        double resultingBalance,
        String description) {

    public Transaction {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(description, "description");
        if (!Double.isFinite(amount) || !Double.isFinite(resultingBalance)) {
            throw new IllegalArgumentException("transaction amounts must be finite");
        }
    }
}
