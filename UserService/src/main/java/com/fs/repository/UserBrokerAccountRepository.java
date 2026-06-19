package com.fs.repository;

import com.fs.domain.UserBrokerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserBrokerAccountRepository extends JpaRepository<UserBrokerAccount, Long> {

    List<UserBrokerAccount> findByUserBrokerIdOrderByIsDefaultDesc(Long userBrokerId);

    Optional<UserBrokerAccount> findByUserBrokerIdAndAccountId(Long userBrokerId, Long accountId);

    Optional<UserBrokerAccount> findByUserBroker_User_IdAndUserBroker_Broker_CodeAndAccount_ExternalAccountId(
            Long userId, String brokerCode, String externalAccountId);

    boolean existsByUserBrokerIdAndAccountId(Long userBrokerId, Long accountId);

    @Modifying
    @Query("UPDATE UserBrokerAccount uba SET uba.isDefault = false WHERE uba.userBroker.id = :userBrokerId")
    void clearDefaultForUserBroker(@Param("userBrokerId") Long userBrokerId);
}
