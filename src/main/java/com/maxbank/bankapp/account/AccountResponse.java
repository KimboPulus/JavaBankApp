package com.maxbank.bankapp.account;

import java.math.BigDecimal;

public record AccountResponse(
        String accountNumber,
        BigDecimal balance,
        CurrencyCode currency,
        AccountType accountType
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountNumber(),
                account.getBalance(),
                account.getCurrency(),
                account.getAccountType()
        );
    }
}
