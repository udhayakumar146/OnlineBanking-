package com.keel.bank.web;

import com.keel.bank.dto.Dtos.*;
import com.keel.bank.service.BankService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BankController {
    private final BankService bank;
    public BankController(BankService bank) { this.bank = bank; }

    @GetMapping("/account")
    public ProfileResponse account(@RequestAttribute(AuthInterceptor.USER_ID) Long uid) { return bank.profile(uid); }

    @PostMapping("/deposit")
    public ProfileResponse deposit(@RequestAttribute(AuthInterceptor.USER_ID) Long uid, @Valid @RequestBody AmountRequest r) {
        return bank.deposit(uid, r.amount());
    }

    @PostMapping("/withdraw")
    public ProfileResponse withdraw(@RequestAttribute(AuthInterceptor.USER_ID) Long uid, @Valid @RequestBody AmountRequest r) {
        return bank.withdraw(uid, r.amount());
    }

    @PostMapping("/transfer")
    public ProfileResponse transfer(@RequestAttribute(AuthInterceptor.USER_ID) Long uid, @Valid @RequestBody TransferRequest r) {
        return bank.transfer(uid, r);
    }

    @GetMapping("/transactions")
    public List<TxResponse> transactions(@RequestAttribute(AuthInterceptor.USER_ID) Long uid) { return bank.history(uid); }
}
