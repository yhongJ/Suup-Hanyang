package com.suuphanyang.auth;

import com.suuphanyang.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** 운영용 SMTP 발송. spring.mail.* 필요 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "smtp")
public class SmtpCodeMailer implements CodeMailer {

    private final JavaMailSender mailSender;
    private final AppProperties props;

    @Override
    public void sendCode(String email, String code) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(props.mail().from());
        msg.setTo(email);
        msg.setSubject("[수업하냥] 학교 인증 코드");
        msg.setText("인증 코드: " + code + "\n\n"
                + props.auth().codeTtlMinutes() + "분 안에 입력하세요.\n"
                + "직접 요청하지 않았다면 이 메일은 무시해도 됩니다.");
        mailSender.send(msg);
    }
}
