package org.example.board_cafe_kiosk_2603.service.admin.sms;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Log4j2
@SpringBootTest
class MailSenderServiceTest {
    @Autowired
    private MailSenderService mailSenderService;

    @Test
    public void sendMailTest() throws Exception {
        // 6자리 난수 생성
        String verificationCode = mailSenderService.generateVerificationCode();

        // 수신자 메일 주소 설정
        String toEmail = "wndus6110@naver.com";
        log.info("테스트 시작 - 수신자: {}, 생성된 인증번호: {}", toEmail, verificationCode);

        // 메일 발송 메서드 호출
        mailSenderService.sendMailForAlarm(toEmail, verificationCode);

        log.info("--- 테스트 메일 발송 요청 완료 ---");
    }

}