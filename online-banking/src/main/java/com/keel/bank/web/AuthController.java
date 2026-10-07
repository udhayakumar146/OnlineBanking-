package com.keel.bank.web;

import com.keel.bank.dto.Dtos.*;
import com.keel.bank.service.BankService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final BankService bank;
    public AuthController(BankService bank) { this.bank = bank; }

    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse register(@Valid @RequestBody RegisterRequest r, HttpServletRequest req) {
        Long id = bank.register(r);
        startSession(req, id);
        return bank.profile(id);
    }

    @PostMapping("/login")
    public ProfileResponse login(@Valid @RequestBody LoginRequest r, HttpServletRequest req) {
        Long id = bank.login(r);
        startSession(req, id);
        return bank.profile(id);
    }

    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s != null) s.invalidate();
    }

    @GetMapping("/me")
    public ProfileResponse me(@RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        return bank.profile(userId);
    }

    private void startSession(HttpServletRequest req, Long id) {
        HttpSession old = req.getSession(false);
        if (old != null) old.invalidate();          // prevents session fixation
        req.getSession(true).setAttribute(AuthInterceptor.USER_ID, id);
    }
}
