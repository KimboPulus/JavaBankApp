package com.maxbank.bankapp.account;

import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
        @NotNull CurrencyCode currency,
        @NotNull AccountType accountType
) {
}
