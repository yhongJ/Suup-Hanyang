package com.suuphanyang.auth;

import com.suuphanyang.auth.AuthDtos.*;
import com.suuphanyang.common.RateLimiter;
import com.suuphanyang.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RateLimiter rateLimiter;
    private final AppProperties props;

    @PostMapping("/api/auth/email/send")
    public ResponseEntity<Void> sendCode(@Valid @RequestBody SendCodeRequest req, HttpServletRequest http) {
        // 대량 발송 방지
        AppProperties.Auth cfg = props.auth();
        rateLimiter.check("send:" + http.getRemoteAddr(), cfg.ipSendLimit(), cfg.rateWindow(),
                "요청이 너무 많아요. 잠시 후 다시 시도하세요.");
        authService.sendCode(req.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/auth/email/verify")
    public LoginResponse verify(@Valid @RequestBody VerifyRequest req, HttpServletRequest http) {
        AppProperties.Auth cfg = props.auth();
        rateLimiter.check("verify:" + http.getRemoteAddr(), cfg.ipVerifyLimit(), cfg.rateWindow(),
                "요청이 너무 많아요. 잠시 후 다시 시도하세요.");
        return authService.verify(req.email(), req.code());
    }

    @GetMapping("/api/me")
    public MeResponse me(@RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        return authService.me(userId);
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(@RequestAttribute(AuthInterceptor.TOKEN) String token) {
        authService.logout(token);
        return ResponseEntity.noContent().build();
    }
}
