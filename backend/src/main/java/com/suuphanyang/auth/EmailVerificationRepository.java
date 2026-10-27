package com.suuphanyang.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    /**
     * FOR UPDATE 잠금. 동시 요청으로 시도 횟수 우회 방지.
     * 트랜잭션 안에서만 호출.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerification> findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    long countByEmailAndCreatedAtAfter(String email, LocalDateTime after);
}
