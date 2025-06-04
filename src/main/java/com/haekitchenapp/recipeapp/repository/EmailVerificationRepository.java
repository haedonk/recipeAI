package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByUserIdAndVerificationCode(Long userId, String code);
    Optional<EmailVerification> findByUserId(Long userId);

}
