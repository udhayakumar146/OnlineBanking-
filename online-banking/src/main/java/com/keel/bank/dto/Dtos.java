package com.keel.bank.dto;

import com.keel.bank.model.BankTransaction;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(
            @NotBlank(message = "Enter your full name.") @Size(max = 100) String fullName,
            @NotBlank(message = "Enter your email.") @Email(message = "Enter a valid email address.") @Size(max = 150) String email,
            @NotBlank(message = "Choose a password.") @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters.") String password) {}

    public record LoginRequest(
            @NotBlank(message = "Enter your email.") String email,
            @NotBlank(message = "Enter your password.") String password) {}

    public record AmountRequest(
            @NotNull(message = "Enter an amount.")
            @DecimalMin(value = "0.01", message = "Amount must be at least 0.01.")
            @Digits(integer = 12, fraction = 2, message = "Amount can have at most 2 decimal places.") BigDecimal amount) {}

    public record TransferRequest(
            @NotBlank(message = "Enter the recipient account number.")
            @Pattern(regexp = "\\d{10}", message = "Account numbers have 10 digits.") String toAccount,
            @NotNull(message = "Enter an amount.")
            @DecimalMin(value = "0.01", message = "Amount must be at least 0.01.")
            @Digits(integer = 12, fraction = 2, message = "Amount can have at most 2 decimal places.") BigDecimal amount,
            @Size(max = 100, message = "Note can be up to 100 characters.") String note) {}

    public record ProfileResponse(String fullName, String email, String accountNumber, BigDecimal balance) {}

    public record TxResponse(Long id, String type, BigDecimal amount, BigDecimal balanceAfter,
                             String counterparty, String note, Instant createdAt) {
        public static TxResponse of(BankTransaction t) {
            return new TxResponse(t.getId(), t.getType().name(), t.getAmount(), t.getBalanceAfter(),
                    t.getCounterparty(), t.getNote(), t.getCreatedAt());
        }
    }
}
