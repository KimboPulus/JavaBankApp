package com.maxbank.bankapp.common;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String message,
        Map<String, String> details
) {
    public static ApiError of(int status, String message) {
        return new ApiError(Instant.now(), status, message, Map.of());
    }

    public static ApiError of(int status, String message, Map<String, String> details) {
        return new ApiError(Instant.now(), status, message, details);
    }
}
