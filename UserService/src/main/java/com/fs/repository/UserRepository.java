package com.fs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.fs.domain.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByName(String name);
    boolean existsByName(String name);
}
