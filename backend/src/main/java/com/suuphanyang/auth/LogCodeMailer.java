package com.suuphanyang.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 개발용. 코드를 콘솔에 출력함.
 * app.mail.mode=log일 때만 사용. prod에서는 비활성.
 */
@Slf4j
@Component
@Profile("!prod")
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "log")
public class LogCodeMailer implements CodeMailer {
    @Override
    public void sendCode(String email, String code) {
        log.info("[개발용 메일] {} 로 보낼 인증 코드: {}", email, code);
    }
}
