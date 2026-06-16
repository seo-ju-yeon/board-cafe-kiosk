package org.example.board_cafe_kiosk_2603.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Log4j2
@Configuration
public class MailSenderConfig {
    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.properties.mail.smtp.socketFactory.port}")
    private int socketPort;

    @Value("${spring.mail.properties.mail.smtp.auth}")
    private boolean auth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean starttls;

    @Value("${spring.mail.properties.mail.smtp.starttls.required:true}")
    private boolean starttlsRequired;

    @Value("${spring.mail.properties.mail.smtp.socketFactory.fallback}")
    private boolean fallback;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.host}")
    private String host;

    @Bean
    public JavaMailSender getJavaMailSender() {
        log.info("--- [MailSenderConfig] JavaMailSender Bean 생성 시작 ---");

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        log.info("메일 서버 호스트: {}", host);
        log.info("메일 서버 포트: {}", port);
        log.info("인증 계정: {}", username);
        // password는 보안을 고려하여 로드 '여부'로 log 기록
        log.info("비밀번호 로드 여부: {}", (password != null && !password.isEmpty()));

        mailSender.setJavaMailProperties(getProperties());
        mailSender.setDefaultEncoding("UTF-8");
        log.info("--- [MailSenderConfig] JavaMailSender Bean 설정 완료 ---"); // 로그 추가
        return mailSender;
    }

    private Properties getProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.ssl.enable", true);  // 해당 코드로 아래의 주석 2줄을 대체
//        props.put("mail.smtp.starttls.enable", starttls);
//        props.put("mail.smtp.starttls.required", starttlsRequired);
        props.put("mail.smtp.socketFactory.fallback", fallback);
        props.put("mail.smtp.socketFactory.port", socketPort);
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        return props;
    }
}
