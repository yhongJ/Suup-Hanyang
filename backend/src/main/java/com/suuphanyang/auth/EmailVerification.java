package com.suuphanyang.auth;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public EmailVerification(String email, String codeHash, LocalDateTime expiresAt, LocalDateTime createdAt) {
        this.email = email;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public boolean isExpired(LocalDateTime now) { return !now.isBefore(expiresAt); }
    public void increaseAttempts() { this.attempts++; }
    public void markUsed() { this.used = true; }
}
