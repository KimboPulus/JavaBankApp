package com.maxbank.bankapp.auth;

import com.maxbank.bankapp.user.Role;

public record AuthResponse(String token, String username, Role role) {
}
