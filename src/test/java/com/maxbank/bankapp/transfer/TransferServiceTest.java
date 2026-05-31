package com.maxbank.bankapp.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maxbank.bankapp.account.Account;
import com.maxbank.bankapp.account.AccountRepository;
import com.maxbank.bankapp.account.AccountType;
import com.maxbank.bankapp.account.CurrencyCode;
import com.maxbank.bankapp.transaction.TransactionRepository;
import com.maxbank.bankapp.user.Role;
import com.maxbank.bankapp.user.User;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransferService transferService;

    @Test
    void transferFailsWhenBalanceIsTooLow() {
        User senderUser = new User("max", "hash", "max@example.com", Role.USER);
        Account sender = new Account("PL111", new BigDecimal("20.00"), CurrencyCode.PLN, AccountType.CHECKING);
        sender.setUser(senderUser);
        Account receiver = new Account("PL222", new BigDecimal("10.00"), CurrencyCode.PLN, AccountType.CHECKING);
        receiver.setUser(new User("ana", "hash", "ana@example.com", Role.USER));

        when(accountRepository.findLockedByAccountNumber("PL111")).thenReturn(Optional.of(sender));
        when(accountRepository.findLockedByAccountNumber("PL222")).thenReturn(Optional.of(receiver));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transfer("max", new TransferRequest("PL111", "PL222", new BigDecimal("50.00")))
        );

        assertEquals("Not enough money on the sender account.", exception.getMessage());
        assertEquals(new BigDecimal("20.00"), sender.getBalance());
        assertEquals(new BigDecimal("10.00"), receiver.getBalance());
        verify(transactionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
