package com.maxbank.bankapp.auth;

import com.maxbank.bankapp.account.Account;
import com.maxbank.bankapp.account.AccountType;
import com.maxbank.bankapp.account.CurrencyCode;
import com.maxbank.bankapp.security.JwtService;
import com.maxbank.bankapp.user.Role;
import com.maxbank.bankapp.user.User;
import com.maxbank.bankapp.user.UserRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SecureRandom random = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new IllegalArgumentException("This username is already taken.");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("This email is already registered.");
        }

        User user = new User(
                request.username().trim(),
                passwordEncoder.encode(request.password()),
                request.email().trim(),
                Role.USER
        );
        user.addAccount(new Account(nextAccountNumber(), new BigDecimal("1000.00"), CurrencyCode.PLN, AccountType.CHECKING));
        User saved = userRepository.save(user);

        String token = jwtService.generateToken(userDetails(saved));
        return new AuthResponse(token, saved.getUsername(), saved.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        User user = userRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));
        String token = jwtService.generateToken(userDetails(user));
        return new AuthResponse(token, user.getUsername(), user.getRole());
    }

    private UserDetails userDetails(User user) {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getHashedPassword())
                .roles(user.getRole().name())
                .build();
    }

    private String nextAccountNumber() {
        StringBuilder value = new StringBuilder("PL");
        for (int i = 0; i < 26; i++) {
            value.append(random.nextInt(10));
        }
        return value.toString();
    }
}
