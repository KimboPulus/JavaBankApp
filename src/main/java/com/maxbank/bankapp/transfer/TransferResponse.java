package com.maxbank.bankapp.transfer;

import com.maxbank.bankapp.transaction.Transaction;
import com.maxbank.bankapp.transaction.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        Long transactionId,
        String senderAccountNumber,
        String receiverAccountNumber,
        BigDecimal amount,
        Instant timestamp,
        TransactionStatus status
) {
    public static TransferResponse from(Transaction transaction) {
        return new TransferResponse(
                transaction.getId(),
                transaction.getSenderAccount().getAccountNumber(),
                transaction.getReceiverAccount().getAccountNumber(),
                transaction.getAmount(),
                transaction.getTimestamp(),
                transaction.getStatus()
        );
    }
}
