package org.example.board_cafe_kiosk_2603.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 포트폴리오 시연용
 * 데이터베이스에 등록되어 있는 ID, PW, email 인증 통과 시 SUPER_OTP를 통해 즉시 로그인 가능
 */
@Component
@ConfigurationProperties(prefix = "portfolio.super-key")
@Getter
@Setter
public class SuperKeyProperties {

    // SUPER_OTP
    // 실제 발송된 OTP와 달라도 입력값이 SUPER_OTP 값이면 인증 통과 (모든 계정 적용)

    private String id;   // 슈퍼 계정 loginId (DB에 실제 존재해야 함)
    private String otp;  // 슈퍼패스 OTP
    private String tempPasswd;  // 슈퍼패스 사용 시 임시 비밀번호

    /* 슈퍼패스 OTP 여부 확인 */
    public boolean isSuperOtp(String inputOtp) {
        return otp != null && otp.equals(inputOtp);
    }
}