package org.example.board_cafe_kiosk_2603.controller.admin.login;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.config.OtpStore;
import org.example.board_cafe_kiosk_2603.config.SuperKeyProperties;
import org.example.board_cafe_kiosk_2603.mapper.admin.manager.ManagerMapper;
import org.example.board_cafe_kiosk_2603.security.ManagerUserDetailsService;
import org.example.board_cafe_kiosk_2603.security.dto.ManagerDTO;
import org.example.board_cafe_kiosk_2603.service.admin.sms.MailSenderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;

@Log4j2
@Controller
@RequestMapping("/login")
@RequiredArgsConstructor
public class LoginController {
    /* 관리자 - 2차 인증 컨트롤러 */

    // STAFF -> 타이머 없는 단순 이메일 입력
    // ADMIN -> 타이머 유지, sendOtp (Ajax)

    private final ManagerMapper managerMapper;
    private final MailSenderService mailSenderService;
    private final OtpStore otpStore;
    private final ManagerUserDetailsService managerUserDetailsService;  // 완전 로그인 처리에 사용
    private final SuperKeyProperties superKey;  // 포트폴리오용 슈퍼키

    // ManagerLoginSuccessHandler -> 1차 인증 후 'PRE_AUTH_USER' 세션 저장
    // LoginController -> 2차 인증 화면 노출 + 검증 + 완전 로그인 처리
    // PRE_AUTH_USER: 1차 인증 통과한 loginId -> 2차 인증 완료 시 제거

    /* STAFF: 이메일 확인 페이지 이동 */
    @GetMapping("/verifyEmail")
    public String verifyEmailPage(HttpSession session, Model model) {
        // PRE_AUTH_USER 없으면 1차 인증 안 한 것 → 로그인 페이지로
        if (session.getAttribute("PRE_AUTH_USER") == null) {
            log.warn("--- [verifyEmail GET] PRE_AUTH_USER 없음 → 로그인 페이지로 리다이렉트 ---");
            return "redirect:/common/login";
        }
        log.info("--- [verifyEmail GET] STAFF 이메일 인증 페이지 진입 ---");
        return "login/verify_email";
    }

    /* STAFF: 입력한 이메일과 DB의 이메일 일치 여부 확인 */
    @PostMapping("/verifyEmail")
    public String verifyEmail(@RequestParam("email") String inputEmail,
                              HttpSession session,
                              Model model) {

        String loginId = (String) session.getAttribute("PRE_AUTH_USER");

        // 세션 만료 방어
        if (loginId == null) {
            log.warn("--- [verifyEmail POST] 세션 만료 → 재로그인 유도 ---");
            return "redirect:/common/login?error=session";
        }

        log.info("--- [verifyEmail POST] loginId: {}, 입력 이메일: {} ---", loginId, inputEmail);

        // DB 이메일 조회
        String dbEmail = managerMapper.findEmailByLoginId(loginId).orElse(null);

        if (dbEmail == null || !dbEmail.equals(inputEmail.trim())) {
            log.warn("--- [verifyEmail POST] 이메일 불일치 | DB: {}, 입력: {} ---", dbEmail, inputEmail);
            return "redirect:/login/verifyEmail?error=email";
        }

        // 이메일 일치 → 완전 로그인 처리
        log.info("--- [verifyEmail POST] 이메일 일치 → STAFF 완전 로그인 처리 ---");
        completeLogin(loginId, session);

        return "redirect:/admin/dashboard";
    }

    /* ADMIN: OTP 인증 페이지 GET */
    @GetMapping("/verifyEmailOtp")
    public String verifyEmailOtpPage(HttpSession session) {
        if (session.getAttribute("PRE_AUTH_USER") == null) {
            log.warn("--- [verifyEmailOtp GET] PRE_AUTH_USER 없음 → 로그인 페이지로 리다이렉트 ---");
            return "redirect:/common/login";
        }
        log.info("--- [verifyEmailOtp GET] ADMIN OTP 인증 페이지 진입 ---");
        return "login/verify_otp";
    }

    // ──────────────────────────────────────────────
    // ADMIN: OTP 발송 Ajax (POST)
    //   verify_otp.html '인증 요청' 버튼 → fetch('/login/sendOtp')
    // ──────────────────────────────────────────────
    // verify_otp.html '인증 요청' 버튼 → fetch('/login/sendOtp')
    @PostMapping("/sendOtp")
    @ResponseBody  // AJAX 응답을 위해 데이터만 반환
    public ResponseEntity<String> sendOtp(@RequestParam("email") String inputEmail,
                                          HttpSession session) {

        // 1차 로그인 성공 시 세션에 담아두었던 로그인 아이디를 가져옴
        String loginId = (String) session.getAttribute("PRE_AUTH_USER");

        // 세션이 없을 경우
        if (loginId == null) {
            log.warn("--- [sendOtp] 세션 만료 ---");
            return ResponseEntity.status(401).body("세션이 만료되었습니다. 다시 로그인해 주세요.");
        }

        log.info("--- [sendOtp] loginId: {}, 입력 이메일: {} ---", loginId, inputEmail);

        // DB 이메일 조회 후 일치 여부 확인
        String dbEmail = managerMapper.findEmailByLoginId(loginId).orElse(null);

        // 사용자가 입력한 이메일과 DB의 이메일이 일치하는지 확인 (공백 제거 확인)
        if (dbEmail == null || !dbEmail.equals(inputEmail.trim())) {
            log.warn("--- [sendOtp] 이메일 불일치 | DB: {}, 입력: {} ---", dbEmail, inputEmail);
            // 400 Bad Request 반환
            return ResponseEntity.status(400).body("등록된 이메일 주소와 일치하지 않습니다.");
        }

        // OTP 생성 → 서버 메모리(optStore)에 임시 저장 → 메 발송
        String code = mailSenderService.generateVerificationCode();  // 6자리 랜덤 번호 생성
        otpStore.save(dbEmail, code);  // 인증 번호 검증을 위해 보관

        try {
            // 메일 전송 성공
            mailSenderService.sendMailForAlarm(dbEmail, code);
            log.info("--- [sendOtp] OTP 발송 완료 | 이메일: {} ---", dbEmail);
            return ResponseEntity.ok("OTP가 발송되었습니다.");
        } catch (MessagingException | UnsupportedEncodingException e) {
            // 메일 서버 오류 발생
            log.error("--- [sendOtp] 메일 발송 실패 | 이메일: {}, 원인: {} ---", dbEmail, e.getMessage());
            return ResponseEntity.status(500).body("메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    // ──────────────────────────────────────────────
    // ADMIN: OTP 검증 POST
    // ──────────────────────────────────────────────
    //   verify_otp.html 로그인 버튼 → fetch('/login/verifyEmailOtp')
    //   성공 시 { "redirect": "/admin/dashboard" } 응답
    //   실패 시 4xx + 오류 메시지 텍스트 응답
    //   JS가 응답을 받아 화면 갱신 or window.location.href 처리

    /* 사용자가 인증번호르 입력하고 로그인버튼을 눌렀을 때 실행 */
    @PostMapping("/verifyEmailOtp")
    @ResponseBody  // 폼 방식 → Ajax 방식으로 전환
    public ResponseEntity<String> verifyEmailOtp(@RequestParam("email") String inputEmail,
                                                 @RequestParam("otp") String inputOtp,
                                                 HttpSession session) {

        // 세션에서 인증 대기 중인 유저 아이디 확인
        String loginId = (String) session.getAttribute("PRE_AUTH_USER");

        if (loginId == null) {
            log.warn("--- [verifyEmailOtp POST] 세션 만료 ---");
            return ResponseEntity.status(401).body("세션이 만료되었습니다. 다시 로그인해 주세요.");
        }

        log.info("--- [verifyEmailOtp POST] loginId: {}, 입력 이메일: {}, OTP: {} ---",
                loginId, inputEmail, inputOtp);

        // DB 이메일 재검증
        String dbEmail = managerMapper.findEmailByLoginId(loginId).orElse(null);
        if (dbEmail == null || !dbEmail.equals(inputEmail.trim())) {
            log.warn("--- [verifyEmailOtp POST] 이메일 불일치 ---");
            return ResponseEntity.status(400).body("등록된 이메일 주소와 일치하지 않습니다.");
        }

        // OTP 검증 - 실제 OTP 또는 슈퍼패스 OTP 중 하나라도 통과하면 인증 성공
        boolean otpValid = otpStore.verify(dbEmail, inputOtp.trim()) || superKey.isSuperOtp(inputOtp.trim());
        if (!otpValid) {
            log.warn("--- [verifyEmailOtp POST] OTP 불일치 또는 만료 | 이메일: {} ---", dbEmail);
            return ResponseEntity.status(400).body("인증번호가 올바르지 않거나 만료되었습니다.");
        }
        // 슈퍼패스 사용 시 로그 기록
        if (superKey.isSuperOtp(inputOtp.trim())) {
            log.info("--- [verifyEmailOtp POST] 슈퍼패스 OTP 사용 | loginId: {} ---", loginId);
        }

        // OTP 일치 → 완전 로그인 처리
        log.info("--- [verifyEmailOtp POST] OTP 검증 성공 → ADMIN 완전 로그인 처리 ---");
        completeLogin(loginId, session);

        // AJAX 요청에 대한 응답으로 리다이렉트할 경로를 문자열로 보냄
        // JS가 받아서 window.location.href 로 이동할 리다이렉트 경로 반환
        return ResponseEntity.ok("/admin/dashboard");
    }

    // ──────────────────────────────────────────────
    // 공통: 완전 로그인 처리 (SecurityContext 복원)
    // ──────────────────────────────────────────────

    /**
     * 2차 인증 통과 후 Spring Security 인증 상태를 완성시킨다.
     * <p>
     * ManagerLoginSuccessHandler에서 SecurityContext를 의도적으로 제거했기 때문에
     * 여기서 UserDetails를 다시 로드하여 Authentication 객체를 세션에 저장해야
     * 이후 @PreAuthorize, hasRole() 등의 권한 검사가 정상 동작한다.
     *
     * @param loginId 1차 인증 시 PRE_AUTH_USER로 저장해둔 loginId
     * @param session 현재 HTTP 세션
     */
    /* 완전 로그인 처리 */
    /* Spring security Context를 수동으로 복구하여 로그인을 완성함 */
    // 1차 로그인 성공 시 시큐리티 권한을 바로 주지 않고 비워두었기 때문에, 수동으로 권한 부여
    private void completeLogin(String loginId, HttpSession session) {
        // 1. DB에서 해당 아이디의 상세 정보(UserDetails/ManagerDTO)를 다시 불러옴.
        // 권한(ROLE_ADMIN 등) 정보를 확실히 가져오기 위함.
        ManagerDTO managerDTO = (ManagerDTO) managerUserDetailsService.loadUserByUsername(loginId);

        // 2. 시큐리티 전용 인증 토큰(Authentication) 생성
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        managerDTO,  // principal: 인증된 사용자 정보
                        null,  // credentials: 비번은 이미 1차에서 검증했으므로 null
                        managerDTO.getAuthorities()  // 사용자의 권한 리스트 부여
                );

        // 3. 비어있는 새로운 SecurityContext 생성 및 인증 토큰 저장
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        // 현재 쓰레드의 시큐리티 저장소에 등록
        SecurityContextHolder.setContext(context);

        // 4. 세션에 시큐리티 컨텍스트(SecurityContext) 보관
        // Spring Security는 매 요청마다 세션에서 이 키값을 확인해 로그인을 유지함 (이게 없으면 다음 요청에서 인증 풀림)
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );

        // 5. 2차 인증 대기용 임시 세션 키 제거
        session.removeAttribute("PRE_AUTH_USER");

        log.info("--- [completeLogin] 완전 로그인 완료 | loginId: {}, authorities: {} ---",
                loginId, managerDTO.getAuthorities());
    }
}
