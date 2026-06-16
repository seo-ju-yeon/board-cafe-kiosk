package org.example.board_cafe_kiosk_2603.service.admin.sms;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Log4j2
@Service
@RequiredArgsConstructor
public class MailSenderService {
    private final JavaMailSender mailSender;

    @Value("${myapp.custom.mail.sender.mailFrom}")
    private String mailFrom;

    @Value("${myapp.custom.mail.sender.mailFromName}")
    private String mailFromName;

    // 6자리 난수 생성 메서드 (OTP용)
    public String generateVerificationCode() {
        int code = java.util.concurrent.ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }

    /* OTP 인증 메일 발송 */
    // 사용처: 2차 인증(LoginController), 비밀번호 찾기(ForgotPasswordController)
    public void sendMailForAlarm(String to, String verificationCode)
            throws MessagingException, UnsupportedEncodingException {

        log.info("메일 발송 시작[sendMailForAlarm] - To: {}, Code: {}", to, verificationCode);

        String subject = "[보드카페 키오스크] 본인인증 인증번호입니다.";
        String body = "<div style='margin:20px; font-family:sans-serif; line-height:1.6;'>"
                + "<h2>안녕하세요, 보드카페 키오스크 서비스입니다.</h2>"
                + "<p>요청하신 본인인증을 위한 인증번호를 안내해 드립니다.</p>"
                + "<div style='margin:20px 0; padding:20px; background:#f4f4f4; text-align:center; border-radius:5px;'>"
                + "  <span style='font-size:30px; font-weight:bold; color:#007bff; letter-spacing:5px;'>"
                + verificationCode
                + "  </span>"
                + "</div>"
                + "<p>해당 인증번호를 화면에 입력해 주세요.</p>"
                + "<p style='color:red;'>인증번호는 3분간 유효합니다.</p>"
                + "<br><p>감사합니다.</p>"
                + "</div>";

        send(to, subject, body);
        log.info("--- [sendMailForAlarm] 인증 메일 발송 완료 ---");
        log.info("수신자: {}, 인증번호: {}", to, verificationCode);
    }

    // ──────────────────────────────────────────────
    // ✅ [추가] 임시 비밀번호 발송
    // 사용처: ForgotPasswordController.verifyOtp
    // ──────────────────────────────────────────────

    public void sendTempPassword(String to, String tempPassword)
            throws MessagingException, UnsupportedEncodingException {

        log.info("--- [sendTempPassword] 발송 시작 | To: {} ---", to);

        String subject = "[보드카페 키오스크] 임시 비밀번호가 발급되었습니다.";
        String body = "<div style='margin:20px; font-family:sans-serif; line-height:1.6;'>"
                + "<h2>안녕하세요, 보드카페 키오스크 서비스입니다.</h2>"
                + "<p>요청하신 임시 비밀번호를 안내해 드립니다.</p>"
                + "<div style='margin:20px 0; padding:20px; background:#f4f4f4; text-align:center; border-radius:5px;'>"
                + "  <span style='font-size:26px; font-weight:bold; color:#dc3545; letter-spacing:4px;'>"
                + tempPassword
                + "  </span>"
                + "</div>"
                + "<p>위 임시 비밀번호로 로그인 후 <strong>반드시 비밀번호를 변경</strong>해 주세요.</p>"
                + "<p style='color:red;'>임시 비밀번호는 외부에 노출되지 않도록 주의하세요.</p>"
                + "<br><p>감사합니다.</p>"
                + "</div>";

        send(to, subject, body);
        log.info("--- [sendTempPassword] 발송 완료 | To: {} ---", to);
    }

    // 공통 메일 발송 헬퍼
    private void send(String to, String subject, String htmlBody)
            throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = mailSender.createMimeMessage();
        message.addRecipients(MimeMessage.RecipientType.TO, to);
        message.setSubject(subject);
        message.setText(htmlBody, "utf-8", "html");
        message.setFrom(new InternetAddress(mailFrom, mailFromName, "UTF-8"));
        mailSender.send(message);
    }
}
