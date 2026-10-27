package com.suuphanyang.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** application.yml의 app.* 설정 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String zone,
        String currentSemester,
        String adminKey,
        List<String> corsOrigins,
        Auth auth,
        Rule rule,
        Mail mail
) {
    public record Auth(String allowedDomain, int codeTtlMinutes, int maxAttempts,
                       int rateWindowMinutes,   // 제한 적용 시간(분)
                       int maxSendsPerWindow,   // 이메일당 발송 횟수
                       int ipSendLimit,         // IP당 발송 횟수
                       int ipVerifyLimit,       // IP당 확인 횟수
                       int ipAdminLimit,        // IP당 관리자 API 횟수
                       int tokenTtlDays) {
        public java.time.Duration rateWindow() {
            return java.time.Duration.ofMinutes(rateWindowMinutes);
        }
    }

    /** 빈 강의실 판정 규칙 */
    public record Rule(int checkOpensBeforeMinutes, int minEnded, int freshMinutes) {}

    public record Mail(String mode, String from) {}
}
