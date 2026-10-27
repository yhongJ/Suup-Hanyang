package com.suuphanyang.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 학교 이메일 인증 완료 사용자 */
@Entity
@Table(name = "app_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AppUser(String email, LocalDateTime createdAt) {
        this.email = email;
        this.createdAt = createdAt;
    }
}
