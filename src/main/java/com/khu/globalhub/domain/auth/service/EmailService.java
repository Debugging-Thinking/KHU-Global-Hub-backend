package com.khu.globalhub.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationEmail(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("[KHU Global Hub] 이메일 인증 코드");
        message.setText(
                "안녕하세요. KHU Global Hub입니다.\n\n" +
                "인증 코드: " + code + "\n\n" +
                "5분 이내에 입력해 주세요."
        );
        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("[KHU Global Hub] 비밀번호 재설정 코드");
        message.setText(
                "안녕하세요. KHU Global Hub입니다.\n\n" +
                "비밀번호 재설정 코드: " + code + "\n\n" +
                "10분 이내에 입력해 주세요.\n" +
                "본인이 요청하지 않았다면 이 메일을 무시하시고 즉시 비밀번호를 변경해 주세요."
        );
        mailSender.send(message);
    }
}
