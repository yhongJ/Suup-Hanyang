package com.suuphanyang.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * prod 실행 시 위험한 설정이 있으면 시작 중단함.
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProdSafetyCheck {

    private static final int MIN_ADMIN_KEY_LENGTH = 32;

    private final AppProperties props;

    @PostConstruct
    void verify() {
        List<String> problems = new ArrayList<>();

        if (props.mail() == null || !"smtp".equals(props.mail().mode())) {
            problems.add("app.mail.mode 가 smtp 가 아니에요. 인증 코드가 메일로 가지 않아요.");
        }
        if (props.mail() == null || props.mail().from() == null
                || props.mail().from().isBlank() || props.mail().from().endsWith("@example.com")) {
            problems.add("MAIL_FROM 환경변수에 실제 발신 주소를 넣으세요.");
        }

        String key = props.adminKey();
        if (key == null || key.isBlank() || "change-me".equals(key) || key.length() < MIN_ADMIN_KEY_LENGTH) {
            problems.add("ADMIN_KEY 환경변수를 " + MIN_ADMIN_KEY_LENGTH
                    + "자 이상의 임의 문자열로 설정하세요. 예: openssl rand -base64 48");
        }

        if (props.corsOrigins() == null || props.corsOrigins().isEmpty()
                || props.corsOrigins().stream().anyMatch(o -> o.contains("localhost") || o.contains("127.0.0.1"))) {
            problems.add("CORS_ORIGINS 에 실제 서비스 주소(https://...)만 넣으세요.");
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException("운영 설정을 확인하세요:\n - " + String.join("\n - ", problems));
        }
    }
}
