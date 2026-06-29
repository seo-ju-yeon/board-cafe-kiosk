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

/**
 * 애플리케이션의 인증 및 인가 정책을 정의하는 Spring Security 설정 클래스입니다.
 *
 * <p>키오스크 사용자와 관리자 사용자의 로그인 흐름이 다르기 때문에
 * {@link SecurityFilterChain}을 두 개로 분리하여 관리합니다.
 * {@code @Order(1)}의 키오스크 체인은 {@code /kiosk/**} 요청을 처리하고,
 * {@code @Order(2)}의 관리자 체인은 {@code /admin/**}, {@code /login/**}
 * 등의 관리자 및 공통 인증 요청을 처리합니다.</p>
 *
 * <p>각 체인은 서로 다른 {@code UserDetailsService}, 로그인 성공 핸들러,
 * 로그인 페이지, 권한 규칙을 사용합니다. 이를 통해 테이블 기반 키오스크 인증과
 * 관리자 계정 기반 인증을 하나의 애플리케이션 안에서 분리해 운영할 수 있습니다.</p>
 *
 * <p>또한 Remember-Me 토큰 저장소와 403 Access Denied 핸들러를 Bean으로 등록하여
 * 인증 유지 및 권한 예외 처리를 공통으로 관리합니다.</p>
 */
@Log4j2
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final DataSource dataSource;  // DB 연결 정보 (Remember-Me용)
    private final KioskUserDetailsService kioskUserDetailsService;  // '키오스크' 로그인 로직
    private final KioskLoginSuccessHandler kioskLoginSuccessHandler;  // 키오스크 로그인 성공 시 처리
    private final ManagerUserDetailsService managerUserDetailsService;  // '관리자' 로그인 로직
    private final ManagerLoginSuccessHandler managerLoginSuccessHandler;  // 관리자 로그인 성공 시 처리

    /**
     * 정적 리소스를 Spring Security 필터 체인에서 제외합니다.
     *
     * <p>CSS, JavaScript, 이미지, 폰트와 같은 정적 파일은 인증 여부와 관계없이
     * 로드되어야 하므로 보안 필터를 거치지 않도록 설정합니다.</p>
     *
     * @return 정적 리소스 제외 규칙이 적용된 WebSecurityCustomizer
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        log.info("--- [SecurityConfig] webSecurityCustomizer: 정적 리소스 보안 제외 설정 ---");
        return (web) -> web.ignoring()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**", "/webjars/**");

    }

    /**
     * 키오스크 화면 전용 보안 필터 체인을 구성합니다.
     *
     * <p>{@code /kiosk/**} 요청에만 적용되며, 테이블 번호 기반 로그인 흐름을 사용합니다.
     * 로그인 및 로그인 처리 URL은 공개하고, 나머지 키오스크 기능은 기본적으로
     * {@code ROLE_TABLE} 권한을 가진 사용자만 접근할 수 있도록 제한합니다.</p>
     *
     * <p>결제, 정산, 테이블 상태 조회처럼 관리자 화면과 연동되는 일부 키오스크 API는
     * {@code ADMIN}, {@code STAFF}, {@code SUPER}, {@code TABLE} 권한을 허용합니다.</p>
     *
     * <p>Remember-Me 기능은 DB 기반 토큰 저장소를 사용하며,
     * 키오스크 세션을 일정 기간 유지할 수 있도록 설정합니다.</p>
     *
     * @param http Spring Security HTTP 보안 설정 객체
     * @return 키오스크 요청에 적용할 SecurityFilterChain
     * @throws Exception 보안 필터 체인 구성 중 예외가 발생한 경우
     */
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
                // 키오스크 전용 화면은 테이블 세션 기반 흐름이므로 CSRF 검사를 비활성화
                .csrf(AbstractHttpConfigurer::disable);

        log.info("--- [SecurityConfig] Kiosk Security Chain 구성 완료 ---");
        return http.build();
    }

    /**
     * 관리자 및 공통 인증 영역의 보안 필터 체인을 구성합니다.
     *
     * <p>{@code /admin/**}, {@code /login/**}, {@code /forgot-password/**}
     * 등의 요청에 적용됩니다. 관리자 로그인은 관리자 계정 기반
     * {@link ManagerUserDetailsService}를 사용하며, 로그인 성공 후에는
     * {@link ManagerLoginSuccessHandler}에서 후속 처리를 수행합니다.</p>
     *
     * <p>관리자 페이지는 {@code ADMIN}, {@code STAFF}, {@code SUPER} 권한을 가진
     * 사용자만 접근할 수 있도록 제한합니다. 이메일 인증, OTP 발송, 비밀번호 찾기,
     * WebSocket 연결에 필요한 일부 엔드포인트는 인증 없이 접근할 수 있도록 허용합니다.</p>
     *
     * <p>AJAX 요청과 WebSocket 요청 중 CSRF 토큰을 포함하기 어려운 경로는
     * CSRF 검사 예외 대상으로 등록합니다.</p>
     *
     * @param http Spring Security HTTP 보안 설정 객체
     * @return 관리자 및 공통 인증 요청에 적용할 SecurityFilterChain
     * @throws Exception 보안 필터 체인 구성 중 예외가 발생한 경우
     */
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
                    log.info("  [adminChain] formLogin 설정: loginPage=/admin/login, processUrl=/admin/login-process");
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
                // WebSocket 및 fetch 기반 일부 AJAX 엔드포인트는 CSRF 예외로 분리
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
    /**
     * Remember-Me 인증 토큰을 데이터베이스에 저장하기 위한 저장소를 생성합니다.
     *
     * <p>{@link JdbcTokenRepositoryImpl}을 사용하여 자동 로그인 토큰을 MariaDB에 저장합니다.
     * 토큰 테이블은 애플리케이션 시작 시 자동 생성하지 않고,
     * 사전에 생성된 테이블을 사용하도록 설정합니다.</p>
     *
     * @return DB 기반 Remember-Me 토큰 저장소
     */
    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        log.info("--- [SecurityConfig] PersistentTokenRepository(Remember-Me DB 저장소) 초기화 ---");
        JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);
        repo.setCreateTableOnStartup(false);  // 테이블명 커스텀 설정 비활
        return repo;
    }

    /* 권한 거부(403 Forbidden) 시 발생할 이벤트 핸들러 */
    /**
     * 권한이 없는 사용자가 보호된 리소스에 접근했을 때 사용할 403 처리 핸들러를 등록합니다.
     *
     * @return 커스텀 AccessDeniedHandler 구현체
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        log.info("--- [SecurityConfig] AccessDeniedHandler(Handler403) 등록 ---");
        return new Handler403();
    }
}
