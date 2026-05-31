package com.maxbank.bankapp.transfer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank String senderAccountNumber,
        @NotBlank String receiverAccountNumber,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {
}
