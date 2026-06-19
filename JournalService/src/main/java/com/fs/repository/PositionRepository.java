package com.fs.repository;

import com.fs.domain.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    Optional<Position> findByUserIdAndFigi(Long userId, String figi);

    List<Position> findByUserId(Long userId);
}
