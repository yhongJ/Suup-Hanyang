package com.suuphanyang.auth;

/** 인증 코드 발송. app.mail.mode로 구현체 선택 */
public interface CodeMailer {
    void sendCode(String email, String code);
}
