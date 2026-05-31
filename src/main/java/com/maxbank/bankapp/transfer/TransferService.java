package com.maxbank.bankapp.transfer;

import com.maxbank.bankapp.account.Account;
import com.maxbank.bankapp.account.AccountRepository;
import com.maxbank.bankapp.transaction.Transaction;
import com.maxbank.bankapp.transaction.TransactionRepository;
import com.maxbank.bankapp.transaction.TransactionStatus;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransferService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransferResponse transfer(String username, TransferRequest request) {
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive.");
        }
        if (request.senderAccountNumber().equals(request.receiverAccountNumber())) {
            throw new IllegalArgumentException("Pick a different account to receive the transfer.");
        }

        Account sender = accountRepository.findLockedByAccountNumber(request.senderAccountNumber())
                .orElseThrow(() -> new EntityNotFoundException("Sender account was not found."));
        Account receiver = accountRepository.findLockedByAccountNumber(request.receiverAccountNumber())
                .orElseThrow(() -> new EntityNotFoundException("Receiver account was not found."));

        if (!sender.getUser().getUsername().equalsIgnoreCase(username)) {
            throw new AccessDeniedException("You can only transfer money from your own accounts.");
        }
        if (sender.getCurrency() != receiver.getCurrency()) {
            throw new IllegalArgumentException("Both accounts must use the same currency.");
        }
        if (sender.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalArgumentException("Not enough money on the sender account.");
        }

        sender.setBalance(sender.getBalance().subtract(request.amount()));
        receiver.setBalance(receiver.getBalance().add(request.amount()));

        Transaction transaction = transactionRepository.save(
                new Transaction(sender, receiver, request.amount(), TransactionStatus.COMPLETED)
        );
        return TransferResponse.from(transaction);
    }
}
