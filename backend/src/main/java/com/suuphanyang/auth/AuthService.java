package com.suuphanyang.auth;

import com.suuphanyang.auth.AuthDtos.LoginResponse;
import com.suuphanyang.auth.AuthDtos.MeResponse;
import com.suuphanyang.common.ApiException;
import com.suuphanyang.common.Hashes;
import com.suuphanyang.config.AppProperties;
import com.suuphanyang.user.AppUser;
import com.suuphanyang.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmailVerificationRepository verificationRepository;
    private final AuthTokenRepository tokenRepository;
    private final AppUserRepository userRepository;
    private final CodeMailer mailer;
    private final AppProperties props;
    private final Clock clock;

    @Transactional
    public void sendCode(String rawEmail) {
        String email = normalize(rawEmail);
        AppProperties.Auth cfg = props.auth();
        if (!email.endsWith("@" + cfg.allowedDomain())) {
            throw ApiException.badRequest("@" + cfg.allowedDomain() + " 주소만 인증할 수 있어요.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (verificationRepository.countByEmailAndCreatedAtAfter(email, now.minusMinutes(cfg.rateWindowMinutes()))
                >= cfg.maxSendsPerWindow()) {
            throw ApiException.tooMany("코드를 너무 자주 요청했어요. " + cfg.rateWindowMinutes() + "분 뒤에 다시 시도하세요.");
        }
        String code = Hashes.sixDigitCode();
        verificationRepository.save(new EmailVerification(
                email, Hashes.sha256(code), now.plusMinutes(cfg.codeTtlMinutes()), now));
        mailer.sendCode(email, code);
    }

    /**
     * 틀려도 시도 횟수는 저장해야 하므로 ApiException은 롤백 안 함.
     * 인증 행은 FOR UPDATE로 잠금.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public LoginResponse verify(String rawEmail, String code) {
        String email = normalize(rawEmail);
        LocalDateTime now = LocalDateTime.now(clock);

        EmailVerification ev = verificationRepository.findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> ApiException.badRequest("인증 코드를 먼저 요청하세요."));
        if (ev.isExpired(now)) {
            throw ApiException.badRequest("코드가 만료됐어요. 다시 요청하세요.");
        }
        if (ev.getAttempts() >= props.auth().maxAttempts()) {
            throw ApiException.badRequest("시도 횟수를 넘었어요. 코드를 다시 요청하세요.");
        }
        if (!Hashes.safeEquals(ev.getCodeHash(), Hashes.sha256(code))) {
            ev.increaseAttempts();
            throw ApiException.badRequest("코드가 맞지 않아요. 메일을 다시 확인하세요.");
        }
        ev.markUsed();

        AppUser user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(new AppUser(email, now)));

        String token = Hashes.randomToken();
        tokenRepository.save(new AuthToken(
                Hashes.sha256(token), user, now.plusDays(props.auth().tokenTtlDays()), now));
        return new LoginResponse(token, email);
    }

    @Transactional(readOnly = true)
    public Optional<Long> resolveUserId(String token) {
        LocalDateTime now = LocalDateTime.now(clock);
        return tokenRepository.findByTokenHash(Hashes.sha256(token))
                .filter(t -> t.getExpiresAt().isAfter(now))
                .map(t -> t.getUser().getId());
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("사용자를 찾을 수 없어요."));
        return new MeResponse(user.getId(), user.getEmail());
    }

    @Transactional
    public void logout(String token) {
        tokenRepository.deleteByTokenHash(Hashes.sha256(token));
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
