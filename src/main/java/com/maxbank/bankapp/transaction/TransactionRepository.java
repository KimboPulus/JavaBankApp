package com.maxbank.bankapp.transaction;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySenderAccountUserUsernameIgnoreCaseOrReceiverAccountUserUsernameIgnoreCaseOrderByTimestampDesc(
            String senderUsername,
            String receiverUsername
    );
}
