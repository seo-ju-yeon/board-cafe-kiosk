package org.example.board_cafe_kiosk_2603.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 애플리케이션의 메일 발송 기능에 필요한 {@link JavaMailSender} Bean을 설정하는 클래스입니다.
 *
 * <p>{@code application.properties} 또는 외부 설정 파일에 정의된
 * {@code spring.mail.*} 값을 주입받아 SMTP 서버 접속 정보를 구성합니다.</p>
 *
 * <p>주로 비밀번호 찾기, 이메일 인증, OTP 발송 등 관리자/사용자 인증 과정에서
 * 메일을 전송하기 위한 공통 메일 발송 인프라로 사용됩니다.</p>
 *
 * <p>보안상 메일 계정 비밀번호는 로그에 직접 출력하지 않고,
 * 로드 여부만 기록합니다.</p>
 */
@Log4j2
@Configuration
public class MailSenderConfig {
    /**
     * SMTP 서버 접속 포트입니다.
     */
    @Value("${spring.mail.port}")
    private int port;

    /**
     * SSL 소켓 팩토리에서 사용할 포트입니다.
     */
    @Value("${spring.mail.properties.mail.smtp.socketFactory.port}")
    private int socketPort;

    /**
     * SMTP 인증 사용 여부입니다.
     */
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

    /**
     * SMTP 설정이 적용된 {@link JavaMailSender} Bean을 생성합니다.
     *
     * <p>호스트, 포트, 계정 정보, SSL 관련 속성을 설정한 뒤
     * 애플리케이션 전역에서 주입받아 사용할 수 있는 메일 발송 객체를 반환합니다.</p>
     *
     * <p>메일 본문에서 한글이 깨지지 않도록 기본 인코딩은 {@code UTF-8}로 설정합니다.</p>
     *
     * @return SMTP 설정이 완료된 JavaMailSender Bean
     */
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
        log.info("--- [MailSenderConfig] JavaMailSender Bean 설정 완료 ---");
        return mailSender;
    }

    /**
     * SMTP SSL 연결에 필요한 JavaMail 속성을 생성합니다.
     *
     * <p>현재 설정은 SSL 소켓 팩토리 기반 연결을 사용하며,
     * {@code mail.smtp.ssl.enable} 값을 통해 SSL을 활성화합니다.</p>
     *
     * @return JavaMailSender에 적용할 SMTP 속성
     */
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
