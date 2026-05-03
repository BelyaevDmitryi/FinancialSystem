package com.fs.repository;

import com.fs.domain.UserBroker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBrokerRepository extends JpaRepository<UserBroker, Long> {

    Optional<UserBroker> findByUserIdAndBrokerId(Long userId, Long brokerId);

    Optional<UserBroker> findByUserIdAndBroker_Code(Long userId, String brokerCode);

    List<UserBroker> findByUserIdOrderByCreatedAt(Long userId);

    boolean existsByUserIdAndBrokerId(Long userId, Long brokerId);
}
