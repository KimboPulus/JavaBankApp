package com.maxbank.bankapp.config;

import com.maxbank.bankapp.account.Account;
import com.maxbank.bankapp.account.AccountType;
import com.maxbank.bankapp.account.CurrencyCode;
import com.maxbank.bankapp.user.Role;
import com.maxbank.bankapp.user.User;
import com.maxbank.bankapp.user.UserRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DataInitializer {
    @Bean
    CommandLineRunner demoUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.existsByUsernameIgnoreCase("max")) {
                return;
            }

            User max = new User("max", passwordEncoder.encode("password123"), "max@example.com", Role.USER);
            max.addAccount(new Account("PL10101010101010101010101010", new BigDecimal("2450.00"), CurrencyCode.PLN, AccountType.CHECKING));
            max.addAccount(new Account("PL20202020202020202020202020", new BigDecimal("9000.00"), CurrencyCode.PLN, AccountType.SAVINGS));

            User ana = new User("ana", passwordEncoder.encode("password123"), "ana@example.com", Role.USER);
            ana.addAccount(new Account("PL30303030303030303030303030", new BigDecimal("1275.00"), CurrencyCode.PLN, AccountType.CHECKING));

            userRepository.save(max);
            userRepository.save(ana);
        };
    }
}
