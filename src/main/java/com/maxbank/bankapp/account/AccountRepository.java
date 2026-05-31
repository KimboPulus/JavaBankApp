package com.maxbank.bankapp.account;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByUserUsernameIgnoreCaseAndClosedFalseOrderByAccountNumber(String username);

    Optional<Account> findByAccountNumberAndUserUsernameIgnoreCase(String accountNumber, String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a join fetch a.user where a.accountNumber = :accountNumber and a.closed = false")
    Optional<Account> findLockedByAccountNumber(@Param("accountNumber") String accountNumber);
}
