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

/**
 * 관리자 로그인 1차 인증 성공 후 2차 인증 단계로 분기하는 성공 핸들러입니다.
 *
 * <p>Spring Security의 기본 로그인 처리는 1차 인증 성공 시 즉시 인증 상태를 세션에 저장합니다.
 * 이 프로젝트에서는 이메일 확인 또는 OTP 인증을 추가로 통과해야 관리자 화면에 접근할 수 있으므로,
 * 로그인 성공 직후 {@code PRE_AUTH_USER}만 세션에 보관하고 {@code SecurityContext}는 제거합니다.</p>
 *
 * <p>2차 인증이 완료되면 {@code LoginController}에서 인증 객체를 다시 생성하여
 * 세션에 저장함으로써 최종 로그인을 완료합니다.</p>
 */
@Log4j2
@Component  //SecurityConfig에서 생성자 주입을 받기 위해 빈 등록
public class ManagerLoginSuccessHandler implements AuthenticationSuccessHandler {
    /**
     * 관리자 권한에 따라 OTP 또는 이메일 확인 화면으로 이동시킵니다.
     *
     * <p>{@code ADMIN}, {@code SUPER} 권한은 OTP 인증으로 이동하고,
     * {@code STAFF} 권한은 이메일 확인 단계로 이동합니다.</p>
     *
     * @param request 로그인 요청
     * @param response 리다이렉트 응답
     * @param authentication 1차 인증에 성공한 사용자 인증 정보
     * @throws IOException 리다이렉트 처리 중 입출력 예외가 발생한 경우
     * @throws ServletException 서블릿 처리 중 예외가 발생한 경우
     */
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
