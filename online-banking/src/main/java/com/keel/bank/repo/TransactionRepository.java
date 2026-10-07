package com.keel.bank.repo;

import com.keel.bank.model.BankTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<BankTransaction, Long> {
    List<BankTransaction> findTop50ByAccountNumberOrderByCreatedAtDescIdDesc(String accountNumber);
}
