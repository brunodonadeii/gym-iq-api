package com.gymiq.repository;

import com.gymiq.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHashAndUsedFalse(String tokenHash);

    Optional<PasswordResetToken> findTopByUserUserIdAndUsedFalseOrderByCreatedAtDesc(UUID userId);

    List<PasswordResetToken> findByUserUserIdAndUsedFalse(UUID userId);

    long deleteByExpiresAtBefore(LocalDateTime dateTime);
}
