package com.suuphanyang.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);

    /** 사용자 행 잠금. 동시 체크 방지용 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AppUser> findWithLockById(Long id);
}
