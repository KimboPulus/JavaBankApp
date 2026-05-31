package com.maxbank.bankapp.account;

import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @GetMapping
    public List<AccountResponse> myAccounts(Principal principal) {
        return accountRepository.findByUserUsernameIgnoreCaseOrderByAccountNumber(principal.getName())
                .stream()
                .map(AccountResponse::from)
                .toList();
    }
}
