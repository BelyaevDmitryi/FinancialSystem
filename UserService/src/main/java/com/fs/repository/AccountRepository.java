package com.fs.repository;

import com.fs.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByBrokerIdAndExternalAccountId(Long brokerId, String externalAccountId);

    boolean existsByBrokerIdAndExternalAccountId(Long brokerId, String externalAccountId);
}
