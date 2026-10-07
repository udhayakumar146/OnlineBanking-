package com.keel.bank.service;

import com.keel.bank.dto.Dtos.*;
import com.keel.bank.model.*;
import com.keel.bank.model.BankTransaction.Type;
import com.keel.bank.repo.*;
import com.keel.bank.web.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankService {
    private final UserRepository users;
    private final AccountRepository accounts;
    private final TransactionRepository txs;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public BankService(UserRepository users, AccountRepository accounts, TransactionRepository txs) {
        this.users = users; this.accounts = accounts; this.txs = txs;
    }

    @Transactional
    public Long register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email))
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists. Sign in instead.");
        User u = new User();
        u.setFullName(r.fullName().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        users.save(u);
        Account a = new Account();
        a.setOwner(u);
        a.setAccountNumber(newAccountNumber());
        accounts.save(a);
        return u.getId();
    }

    @Transactional(readOnly = true)
    public Long login(LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase()).orElse(null);
        if (u == null || !encoder.matches(r.password(), u.getPasswordHash()))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
        return u.getId();
    }

    @Transactional(readOnly = true)
    public ProfileResponse profile(Long userId) {
        return toProfile(accounts.findByOwnerId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found.")));
    }

    @Transactional
    public ProfileResponse deposit(Long userId, BigDecimal amount) {
        Account a = lockOwn(userId);
        BigDecimal amt = money(amount);
        a.setBalance(a.getBalance().add(amt));
        txs.save(new BankTransaction(a.getAccountNumber(), Type.DEPOSIT, amt, a.getBalance(), null, "Deposit"));
        return toProfile(a);
    }

    @Transactional
    public ProfileResponse withdraw(Long userId, BigDecimal amount) {
        Account a = lockOwn(userId);
        BigDecimal amt = money(amount);
        requireFunds(a, amt);
        a.setBalance(a.getBalance().subtract(amt));
        txs.save(new BankTransaction(a.getAccountNumber(), Type.WITHDRAWAL, amt, a.getBalance(), null, "Withdrawal"));
        return toProfile(a);
    }

    @Transactional
    public ProfileResponse transfer(Long userId, TransferRequest r) {
        String fromNo = accounts.findByOwnerId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found.")).getAccountNumber();
        String toNo = r.toAccount().trim();
        if (fromNo.equals(toNo))
            throw new ApiException(HttpStatus.BAD_REQUEST, "You can't transfer to your own account.");

        // Lock both rows in a fixed order so two opposite transfers can't deadlock.
        Account from, to;
        if (fromNo.compareTo(toNo) < 0) { from = lockNumber(fromNo); to = lockNumber(toNo); }
        else { to = lockNumber(toNo); from = lockNumber(fromNo); }

        BigDecimal amt = money(r.amount());
        requireFunds(from, amt);
        from.setBalance(from.getBalance().subtract(amt));
        to.setBalance(to.getBalance().add(amt));
        String note = r.note() == null || r.note().isBlank() ? null : r.note().trim();
        txs.save(new BankTransaction(fromNo, Type.TRANSFER_OUT, amt, from.getBalance(), toNo, note));
        txs.save(new BankTransaction(toNo, Type.TRANSFER_IN, amt, to.getBalance(), fromNo, note));
        return toProfile(from);
    }

    @Transactional(readOnly = true)
    public List<TxResponse> history(Long userId) {
        Account a = accounts.findByOwnerId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found."));
        return txs.findTop50ByAccountNumberOrderByCreatedAtDescIdDesc(a.getAccountNumber())
                .stream().map(TxResponse::of).toList();
    }

    private Account lockOwn(Long userId) {
        return accounts.lockByOwnerId(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found."));
    }
    private Account lockNumber(String no) {
        return accounts.lockByNumber(no).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No account found with number " + no + "."));
    }
    private void requireFunds(Account a, BigDecimal amt) {
        if (a.getBalance().compareTo(amt) < 0)
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient balance for this amount.");
    }
    private BigDecimal money(BigDecimal v) { return v.setScale(2, RoundingMode.UNNECESSARY); }
    private ProfileResponse toProfile(Account a) {
        return new ProfileResponse(a.getOwner().getFullName(), a.getOwner().getEmail(), a.getAccountNumber(), a.getBalance());
    }
    private String newAccountNumber() {
        String n;
        do { n = String.valueOf(1_000_000_000L + (long) (random.nextDouble() * 9_000_000_000L)); }
        while (accounts.existsByAccountNumber(n));
        return n;
    }
}
