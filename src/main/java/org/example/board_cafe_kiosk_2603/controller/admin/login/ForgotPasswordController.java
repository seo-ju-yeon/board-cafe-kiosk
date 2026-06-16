package org.example.board_cafe_kiosk_2603.controller.admin.login;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.config.OtpStore;
import org.example.board_cafe_kiosk_2603.config.SuperKeyProperties;
import org.example.board_cafe_kiosk_2603.domain.admin.manager.Manager;
import org.example.board_cafe_kiosk_2603.mapper.admin.manager.ManagerMapper;
import org.example.board_cafe_kiosk_2603.service.admin.manager.ManagerService;
import org.example.board_cafe_kiosk_2603.service.admin.sms.MailSenderService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;

/**
 * 비밀번호 찾기 컨트롤러
 */
@Log4j2
@Controller
@RequestMapping("/forgot-password")
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final ManagerMapper managerMapper;
    private final ManagerService managerService;
    private final MailSenderService mailSenderService;
    private final OtpStore otpStore;
    private final SuperKeyProperties superKey;  // 포트폴리오용 슈퍼키

    // 비밀번호 찾기 첫 페이지 진입 (find_pw.html)
    @GetMapping
    public String findPwPage() {
        return "login/find_pw";
    }

    // STEP 1: 아이디 존재 확인 & 비활성 계정 차단
    @PostMapping("/verify-id")
    @ResponseBody  // AJAX 요청이므로 데이터만 반환
    public ResponseEntity<String> verifyId(@RequestParam("loginId") String loginId,
                                           HttpSession session) {

        log.info("--- [forgot/verify-id] 아이디 확인 요청 | loginId: {} ---", loginId);

        // 1) DB에서 아이디 존재 여부
        Manager manager = managerMapper.findByLoginId(loginId.trim()).orElse(null);

        if (manager == null) {
            log.warn("--- [forgot/verify-id] 존재하지 않는 아이디: {} ---", loginId);
            return ResponseEntity.status(404).body("존재하지 않는 아이디입니다.");
        }

        // 2) 비활성 계정 차단
        if (!manager.isActive()) {
            log.warn("--- [forgot/verify-id] 비활성화된 계정 접근 차단 | loginId: {} ---", loginId);
            return ResponseEntity.status(403).body("비활성화된 계정입니다. 관리자에게 문의해 주세요.");
        }

        // 3) 이메일 인증에서 사용하기 위해 세션에 아이디 저장
        session.setAttribute("FORGOT_ID", loginId.trim());
        log.info("--- [forgot/verify-id] 아이디 확인 성공 | loginId: {} ---", loginId);

        return ResponseEntity.ok("ok");
    }

    // STEP 2-a: 이메일 대조 및 OTP 발송
    @PostMapping("/send-otp")
    @ResponseBody  // AJAX 요청이므로 데이터만 반환
    public ResponseEntity<String> sendOtp(@RequestParam("email") String inputEmail,
                                          HttpSession session) {

        // 세션에서 1단계 때 저장한 아이디를 꺼냄
        String loginId = (String) session.getAttribute("FORGOT_ID");

        // 아이디 확인 없이 바로 이 경로로 접근한 경우 차단
        if (loginId == null) {
            log.warn("--- [forgot/send-otp] 세션 만료 ---");
            return ResponseEntity.status(401).body("세션이 만료되었습니다. 처음부터 다시 시도해 주세요.");
        }

        log.info("--- [forgot/send-otp] loginId: {}, 입력 이메일: {} ---", loginId, inputEmail);

        // 비활성 계정 재확인 (세션 탈취 등 우회 방어)
        Manager manager = managerMapper.findByLoginId(loginId).orElse(null);
        if (manager == null || !manager.isActive()) {
            log.warn("--- [forgot/send-otp] 비활성화 계정 또는 존재하지 않는 계정 | loginId: {} ---", loginId);
            session.removeAttribute("FORGOT_ID");
            return ResponseEntity.status(403).body("비활성화된 계정입니다. 관리자에게 문의해 주세요.");
        }

        // DB에 저장된 실제 이메일과 대조
        String dbEmail = managerMapper.findEmailByLoginId(loginId).orElse(null);

        // 사용자가 입력한 이메일과 DB 정보가 일치하는지 대조
        if (dbEmail == null || !dbEmail.equals(inputEmail.trim())) {
            log.warn("--- [forgot/send-otp] 이메일 불일치 | DB: {}, 입력: {} ---", dbEmail, inputEmail);
            return ResponseEntity.status(400).body("등록된 이메일 주소와 일치하지 않습니다.");
        }

        // OTP 생성 → 저장 → 발송
        String code = mailSenderService.generateVerificationCode();
        otpStore.save(dbEmail, code);

        try {
            mailSenderService.sendMailForAlarm(dbEmail, code);
            log.info("--- [forgot/send-otp] OTP 발송 완료 | 이메일: {} ---", dbEmail);
            return ResponseEntity.ok("인증번호가 발송되었습니다.");
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("--- [forgot/send-otp] 메일 발송 실패 | 원인: {} ---", e.getMessage());
            return ResponseEntity.status(500).body("메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    // STEP 2-b: OTP 검증 및 임시 비밀번호 발급·발송
    @PostMapping("/verify-otp")
    @ResponseBody  // AJAX 요청이므로 데이터만 반환
    public ResponseEntity<String> verifyOtp(@RequestParam("email") String inputEmail,
                                            @RequestParam("otp") String inputOtp,
                                            HttpSession session) {

        String loginId = (String) session.getAttribute("FORGOT_ID");

        if (loginId == null) {
            log.warn("--- [forgot/verify-otp] 세션 만료 ---");
            return ResponseEntity.status(401).body("세션이 만료되었습니다. 처음부터 다시 시도해 주세요.");
        }

        log.info("--- [forgot/verify-otp] loginId: {}, 이메일: {}, OTP: {} ---",
                loginId, inputEmail, inputOtp);

        // 이메일 재검증 (세션 탈취 방어)
        String dbEmail = managerMapper.findEmailByLoginId(loginId).orElse(null);
        if (dbEmail == null || !dbEmail.equals(inputEmail.trim())) {
            log.warn("--- [forgot/verify-otp] 이메일 불일치 ---");
            return ResponseEntity.status(400).body("등록된 이메일 주소와 일치하지 않습니다.");
        }

        // 1) OTP 검증: 실제 발송된 OTP 또는 슈퍼패스 OTP 중 하나라도 통과하면 인증 성공
        boolean usedSuperOtp = superKey.isSuperOtp(inputOtp.trim());
        boolean otpValid = otpStore.verify(dbEmail, inputOtp.trim()) || usedSuperOtp;

        if (!otpValid) {
            log.warn("--- [forgot/verify-otp] OTP 불일치 또는 만료 ---");
            return ResponseEntity.status(400).body("인증번호가 올바르지 않거나 만료되었습니다.");
        }

        // 2) 비밀번호 초기화 실행
        if (usedSuperOtp) {
            // 슈퍼패스 키 사용 시 미리 정해둔 고정 임시 비번으로 설정
            managerService.resetPasswordTo(loginId, superKey.getTempPasswd());
            log.info("--- [forgot/verify-otp] 슈퍼패스 임시 비밀번호 적용 | loginId: {}, tempPasswd: {} ---",
                    loginId, superKey.getTempPasswd());  // 슈퍼패스 사용 여부 로그 (시연 추적용)
        } else {
            // 일반 사용자는 랜덤 비번 생성 후 암호화하여 DB 저장
            String tempPassword = managerService.resetPassword(loginId);
            try {
                // 발급된 임시 비밀번호를 이메일로 다시 발송
                mailSenderService.sendTempPassword(dbEmail, tempPassword);
                log.info("--- [forgot/verify-otp] 임시 비밀번호 발송 완료 | loginId: {} ---", loginId);
            } catch (MessagingException | UnsupportedEncodingException e) {
                log.error("--- [forgot/verify-otp] 임시 비밀번호 메일 발송 실패 | 원인: {} ---", e.getMessage());
                return ResponseEntity.status(500).body("임시 비밀번호 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.");
            }
        }

        // 3) 보안을 위해 프로세스 종료 후 세션에서 정보 삭제 (세션 정리)
        // FORGOT_ID : verify-id 통과한 loginId → verify-otp 완료 시 제거
        session.removeAttribute("FORGOT_ID");
        log.info("--- [forgot/verify-otp] 비밀번호 재설정 완료 | loginId: {} ---", loginId);

        return ResponseEntity.ok("ok");
    }
}
