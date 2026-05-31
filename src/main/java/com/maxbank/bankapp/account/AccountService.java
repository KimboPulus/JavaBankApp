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
