package com.keel.bank.repo;

import com.keel.bank.model.Account;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByAccountNumber(String accountNumber);
    Optional<Account> findByOwnerId(Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber = :n")
    Optional<Account> lockByNumber(@Param("n") String number);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.owner.id = :id")
    Optional<Account> lockByOwnerId(@Param("id") Long ownerId);
}
