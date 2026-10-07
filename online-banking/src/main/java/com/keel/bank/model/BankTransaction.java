package com.keel.bank.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bank_transactions", indexes = @Index(columnList = "accountNumber, createdAt"))
public class BankTransaction {
    public enum Type { DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 10)
    private String accountNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Type type;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;
    @Column(length = 10)
    private String counterparty;
    @Column(length = 100)
    private String note;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public BankTransaction() {}
    public BankTransaction(String acct, Type type, BigDecimal amount, BigDecimal balanceAfter, String counterparty, String note) {
        this.accountNumber = acct; this.type = type; this.amount = amount;
        this.balanceAfter = balanceAfter; this.counterparty = counterparty; this.note = note;
    }
    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public Type getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getCounterparty() { return counterparty; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
