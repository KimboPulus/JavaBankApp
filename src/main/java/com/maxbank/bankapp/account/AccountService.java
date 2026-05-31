package com.maxbank.bankapp.account;

import com.maxbank.bankapp.user.User;
import com.maxbank.bankapp.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AccountResponse createAccount(String username, CreateAccountRequest request) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new EntityNotFoundException("User was not found."));

        Account account = new Account(
                nextAccountNumber(),
                BigDecimal.ZERO,
                request.currency(),
                request.accountType()
        );
        account.setUser(user);
        Account saved = accountRepository.save(account);
        return AccountResponse.from(saved);
    }

    @Transactional
    public void closeAccount(String username, String accountNumber) {
        Account account = accountRepository.findByAccountNumberAndUserUsernameIgnoreCase(accountNumber, username)
                .orElseThrow(() -> new EntityNotFoundException("Account was not found."));

        if (account.isClosed()) {
            throw new IllegalArgumentException("This account is already closed.");
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Move the money out before closing this account.");
        }

        account.setClosed(true);
    }

    private String nextAccountNumber() {
        String accountNumber;
        do {
            StringBuilder value = new StringBuilder("PL");
            for (int i = 0; i < 26; i++) {
                value.append(random.nextInt(10));
            }
            accountNumber = value.toString();
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
}
