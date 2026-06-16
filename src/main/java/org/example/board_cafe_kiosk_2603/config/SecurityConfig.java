package org.example.board_cafe_kiosk_2603.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.security.KioskAuthorizationManager;
import org.example.board_cafe_kiosk_2603.security.KioskUserDetailsService;
import org.example.board_cafe_kiosk_2603.security.ManagerUserDetailsService;
import org.example.board_cafe_kiosk_2603.security.handler.ManagerLoginSuccessHandler;
import org.example.board_cafe_kiosk_2603.security.handler.Handler403;
import org.example.board_cafe_kiosk_2603.security.handler.KioskLoginSuccessHandler;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;
import java.nio.file.Path;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final DataSource dataSource;  // DB 연결 정보 (Remember-Me용)
    private final KioskUserDetailsService kioskUserDetailsService;  // '키오스크' 로그인 로직
    private final KioskLoginSuccessHandler kioskLoginSuccessHandler;  // 키오스크 로그인 성공 시 처리
    private final ManagerUserDetailsService managerUserDetailsService;  // '관리자' 로그인 로직
    private final ManagerLoginSuccessHandler managerLoginSuccessHandler;  // 관리자 로그인 성공 시 처리
    private final KioskAuthorizationManager kioskAuthorizationManager;

    /* 정적 리소스(CSS, JS, Image, etc) 보안 필터링에서 제외대상 설정 */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        log.info("--- [SecurityConfig] webSecurityCustomizer: 정적 리소스 보안 제외 설정 ---");
        return (web) -> web.ignoring()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**", "/webjars/**");

    }

    // ── Chain 1: 키오스크 (/kiosk/**) ──────────────────────────
    @Bean
    @Order(1)
    public SecurityFilterChain kioskChain(HttpSecurity http) throws Exception {
        log.info("--- [SecurityConfig] Kiosk Security Chain 구성 시작 ---");

        http
                .securityMatcher("/kiosk/**")
                .authorizeHttpRequests(auth -> {
                    log.info("  [kioskChain] 권한 규칙 설정: 로그인 URL permitAll, 관리자 주문 모니터링 URL은 ADMIN/STAFF/SUPER, 나머지 → ROLE_TABLE");
                    auth
                            // 로그인 관련 URL 허용
                            .requestMatchers("/kiosk/login",
                                    "/kiosk/login-process"
                            ).permitAll()
                            // 관리자 대시보드 연동 및 정산 페이지 접근 허용
                            .requestMatchers(
                                    "/kiosk/checkout",
                                    "/kiosk/cleaning_wait",
                                    "/kiosk/order/active",
                                    "/kiosk/table/status",
                                    "/kiosk/payment/prepare",
                                    "/kiosk/point/lookup",
                                    "/kiosk/toss/success",
                                    "/kiosk/toss/fail"
                            ).hasAnyRole("ADMIN", "STAFF", "SUPER", "TABLE")
                            // 나머지 키오스크 영역 → TABLE 권한 필요
                            .anyRequest().hasRole("TABLE");
                })
                // userDetailsService는 authorizeHttpRequests 이후에 설정
                .userDetailsService(kioskUserDetailsService)
                .formLogin(config -> {
                    log.info("  [kioskChain] formLogin 설정: loginPage=/kiosk/login, processUrl=/kiosk/login-process");
                    config
                            .loginPage("/kiosk/login")
                            .loginProcessingUrl("/kiosk/login-process")
                            .usernameParameter("tableNumber")
                            .passwordParameter("password")
                            .successHandler(kioskLoginSuccessHandler)
                            .failureUrl("/kiosk/login?error")
                            .permitAll();
                })
                .rememberMe(remember -> {
                    log.info("  [kioskChain] rememberMe 설정: 유효기간 30일, DB 토큰 저장소 사용");
                    remember
                            .rememberMeParameter("remember-me")
                            .tokenRepository(persistentTokenRepository())
                            .tokenValiditySeconds(60 * 60 * 24 * 30)
                            .userDetailsService(kioskUserDetailsService);
                })
                .logout(logout -> {
                    log.info("  [kioskChain] logout 설정: /kiosk/logout → /kiosk/login 리다이렉트");
                    logout
                            .logoutUrl("/kiosk/logout")
                            .logoutSuccessUrl("/kiosk/login")
                            .deleteCookies("JSESSIONID", "remember-me");
                })
                .exceptionHandling(config -> {
                    log.info("  [kioskChain] exceptionHandling 설정: 403 → Handler403");
                    config.accessDeniedHandler(accessDeniedHandler());
                })
                // 개발단계, csrf 비활성화
                .csrf(AbstractHttpConfigurer::disable);

        log.info("--- [SecurityConfig] Kiosk Security Chain 구성 완료 ---");
        return http.build();
    }

    // ── Chain 2: 관리자 ──────────────────────────
    @Bean
    @Order(2)
    public SecurityFilterChain adminChain(HttpSecurity http) throws Exception {
        log.info("--- [SecurityConfig] Admin Security Chain 구성 시작 ---");

        http
                .securityMatcher(
                        "/admin/**",
                        "/common/**",
                        "/login/**",
                        "/forgot-password/**",
                        "/error"
                )
                .authorizeHttpRequests(auth -> {
                    log.info("  [adminChain] 권한 규칙 설정 시작");
                    auth
                            // 공개 허용 URL
                            // 인증 없이 접근 가능한 URL
                            .requestMatchers(
                                    "/common/login",
                                    "/common/logout",
                                    "/admin/login",
                                    "/admin/login-process",
                                    "/admin/find_pw",
                                    "/forgot-password/**",
                                    "/error",
                                    // 2차 인증 엔드포인트 (PRE_AUTH_USER 세션으로 내부 검증)
                                    "/login/verifyEmail",
                                    "/login/verifyEmailOtp",
                                    "/login/sendOtp",
                                    "/ws/**",  // WebSocket 엔드포인트
                                    "/app/**"  // WebSocket /app/** 경로
                            ).permitAll()
                            // 관리자 영역 → ADMIN, STAFF, SUPER 권한 필요
                            .requestMatchers("/admin/**").hasAnyRole("ADMIN", "STAFF", "SUPER")
                            // 나머지 인증만 필요
                            .anyRequest().authenticated();
                    log.info("  [adminChain] 권한 규칙 설정 완료");
                })
                .userDetailsService(managerUserDetailsService)
                .formLogin(config -> {
                    log.info("  [adminChain] formLogin 설정: loginPage=/common/login, processUrl=/admin/login-process");
                    config
                            .loginPage("/admin/login")
                            .loginProcessingUrl("/admin/login-process")
                            .usernameParameter("username")
                            .passwordParameter("password")
                            .successHandler(managerLoginSuccessHandler)
                            .failureUrl("/admin/login?error")
                            .permitAll();
                })
                .logout(logout -> {
                    log.info("  [adminChain] logout 설정: /admin/logout → /common/login 리다이렉트");
                    logout
                            .logoutUrl("/admin/logout")
                            .logoutSuccessUrl("/common/login")
                            .invalidateHttpSession(true)
                            .deleteCookies("JSESSIONID");
                })
                .exceptionHandling(config -> {
                    log.info("  [adminChain] exceptionHandling 설정");
                    config
                            .accessDeniedHandler(accessDeniedHandler())
                            // 미인증 접근 시 관리자 로그인 페이지로 이동 (기본값 override)
                            .authenticationEntryPoint((request, response, authException) -> {
                                log.warn("  [adminChain][AuthEntryPoint] 미인증 접근 감지 → URI: {}, 예외: {}",
                                        request.getRequestURI(), authException.getMessage());
                                response.sendRedirect("/admin/login");
                            });
                })
                // 개발단계, csrf 비활성화
//                .csrf(AbstractHttpConfigurer::disable);
                // WebSocket은 CSRF 보호 불필요
                .csrf(csrf -> csrf
                        // WebSocket
                        .ignoringRequestMatchers("/ws/**", "/app/**")
                        // AJAX 엔드포인트 예외 추가 (fetch()는 CSRF 토큰 미포함)
                        .ignoringRequestMatchers("/login/**")
                        .ignoringRequestMatchers("/forgot-password/**")
                        .ignoringRequestMatchers("/admin/staff/**")
                        // 대시보드 API들의 CSRF 검사를 건너뜁니다.
                        .ignoringRequestMatchers("/admin/dashboard/**")
                        .ignoringRequestMatchers("/admin/macro/**")
                        // 요금정책
                        .ignoringRequestMatchers("/admin/policy/**")
                        // 카테고리
                        .ignoringRequestMatchers("/admin/category/**")
                );

        log.info("--- [SecurityConfig] Admin Security Chain 구성 완료 ---");
        return http.build();
    }

    /* 자동 로그인 정보를 DB에 보관하는 저장소 설정 */
    // Remember-me DB 저장소
    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        log.info("--- [SecurityConfig] PersistentTokenRepository(Remember-Me DB 저장소) 초기화 ---");
        JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);
        repo.setCreateTableOnStartup(false);  // 테이블명 커스텀 설정 비활
        return repo;
    }

    /* 권한 거부(403 Forbidden) 시 발생할 이벤트 핸들러 */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        log.info("--- [SecurityConfig] AccessDeniedHandler(Handler403) 등록 ---");
        return new Handler403();
    }
}
