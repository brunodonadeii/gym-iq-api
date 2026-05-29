package com.gymiq.repository;

import com.gymiq.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {

    Optional<PasswordResetToken> findByTokenHashAndUsedFalse(String tokenHash);

    Optional<PasswordResetToken> findTopByUserUserIdAndUsedFalseOrderByCreatedAtDesc(Integer userId);

    List<PasswordResetToken> findByUserUserIdAndUsedFalse(Integer userId);
}
