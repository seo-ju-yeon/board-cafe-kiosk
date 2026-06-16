package org.example.board_cafe_kiosk_2603.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Log4j2
@Component  //SecurityConfig에서 생성자 주입을 받기 위해 빈 등록
public class ManagerLoginSuccessHandler implements AuthenticationSuccessHandler {
    // instanceof 분기가 생기는 순간 단일 책임 원칙(SRP) 위반되므로,
    // Kiosk, Admin - LoginSuccessHandler 2EA의 파일로 관리

    // 목적 - ROLE 기반 2차 인증 분기

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        // 1. Role 확인 (ADMIN, SUPER -> OTP | STAFF -> 이메일 확인)
        boolean isOtpRequired = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SUPER"));
        log.info("--- [ManagerLoginSuccess] 1차 인증 성공, loginId: {}, isOtpRequired: {} ---", authentication.getName(), isOtpRequired);

        HttpSession session = request.getSession();
        // 2. 세션에 loginId 임시 저장 (2차 인증 전까지 loginId 보관)
        session.setAttribute("PRE_AUTH_USER", authentication.getName());

        // SecurityContext 제거 - 2차 인증 전까지 완전 로그인 차단
        // 3. 세션에서 SecurityContext 먼저 제거
        /*
        - clearContext()보다 반드시 먼저 실행해야함.
        - Spring Security가 응답 완료 후 현재 SecurityContext를 세션에 자동 저장하는데,
        - ClearContext()이후 자동 저장이 발생하면 빈 Context가 세션에 덮어써질 수 있음
         */
//        SecurityContextHolder.clearContext();
        session.removeAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );
        // 4. 스레드 로컬 SecurityContext 제거 (2차 인증 전까지 완전 로그인 차단)
        SecurityContextHolder.clearContext();
        log.info("--- [ManagerLoginSuccess] SecurityContext 제거 완료, loginId: {} ---", authentication.getName());

        // Role에 따라 2차 인증 페이지로 분기
        if (isOtpRequired) {
            // ADMIN → OTP 인증 페이지
            log.info("--- [ManagerLoginSuccess] ADMIN/SUPER → OTP 인증 페이지 ---");
            response.sendRedirect("/login/verifyEmailOtp");
        } else {
            // STAFF → 이메일 확인 페이지
            log.info("--- [ManagerLoginSuccess] STAFF → 이메일 확인 페이지 ---");
            response.sendRedirect("/login/verifyEmail");
        }

    }
}
