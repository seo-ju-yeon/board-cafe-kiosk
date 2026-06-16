package org.example.board_cafe_kiosk_2603.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.security.dto.KioskDTO;
import org.example.board_cafe_kiosk_2603.service.admin.cafeTable.CafeTableService;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;

@Log4j2
@Component
@RequiredArgsConstructor
public class KioskAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final CafeTableService cafeTableService;

    // 변경 적용 전
//    @Override
//    public AuthorizationDecision check(Supplier<Authentication> authentication,
//                                       RequestAuthorizationContext context) {
//        HttpServletRequest request = context.getRequest();
//        Object tableId = request.getSession().getAttribute("tableId");
//        Object tableNumber = request.getSession().getAttribute("tableNumber");
//        return new AuthorizationDecision(tableId != null || tableNumber != null);
//    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication,
                                       RequestAuthorizationContext context) {
        HttpServletRequest request = context.getRequest();
        String requestURI = request.getRequestURI();
        Authentication auth = authentication.get();

        log.info("--- [KioskAuthorizationManager] 접근 제어 검사 | URI: {} ---", requestURI);

        // 1. 미인증 → 차단
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("--- [KioskAuthorizationManager] 미인증 접근 차단 | URI: {} ---", requestURI);
            return new AuthorizationDecision(false);
        }

        log.info("--- [KioskAuthorizationManager] 인증 정보 | principal: {}, authorities: {} ---",
                auth.getName(), auth.getAuthorities());

        // 2. 관리자(ADMIN, STAFF) → 전체 허용
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_STAFF"));
        if (isAdmin) {
            log.info("--- [KioskAuthorizationManager] 관리자 접근 허용 | principal: {}, URI: {} ---",
                    auth.getName(), requestURI);
            return new AuthorizationDecision(true);
        }

        // 3. Remember-Me 복구 시 세션 재세팅
        if (auth.getPrincipal() instanceof KioskDTO kiosk) {
            Object sessionTableNumber = request.getSession().getAttribute("tableNumber");
            if (sessionTableNumber == null) {
                log.info("--- [KioskAuthorizationManager] Remember-Me 복구 → 세션 재세팅 | tableNumber: {} ---",
                        kiosk.getUsername());
                request.getSession().setAttribute("tableNumber", kiosk.getUsername());
//                log.info("--- [KioskAuthorizationManager] 세션 재세팅 완료 | sessionId: {} ---",
//                        request.getSession().getId());

                // ✅ 추가: access_token 갱신
//                String newToken = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
//                cafeTableService.updateAccessToken(kiosk.getTableId(), newToken);
//                request.getSession().setAttribute("accessToken", newToken);
//
//                log.info("--- [KioskAuthorizationManager] Remember-Me 복구 시 access_token 재발급 완료 ---");

                // ✅ access_token 갱신 조건을 세션이 아닌 DB 값 기준으로 변경
//                String currentToken = cafeTableService.getTableAccessToken(kiosk.getTableId()); // 추가 필요
//                if (currentToken == null || currentToken.trim().isEmpty()) {
//                    String newToken = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
//                    cafeTableService.updateAccessToken(kiosk.getTableId(), newToken);
//                    request.getSession().setAttribute("accessToken", newToken);
//                    log.info("--- [KioskAuthorizationManager] access_token 없음 → 재발급 완료 | tableId: {} ---",
//                            kiosk.getTableId());
//                }
                // ✅ access_token이 없으면 → 세션/쿠키 무효화 후 로그인으로 튕김
                String currentToken = cafeTableService.getTableAccessToken(kiosk.getTableId());
                if (currentToken == null || currentToken.trim().isEmpty()) {
                    log.warn("--- [KioskAuthorizationManager] access_token 없음 → 강제 로그아웃 | tableId: {} ---",
                            kiosk.getTableId());
                    request.getSession().invalidate();  // 세션 파기
                    return new AuthorizationDecision(false);  // 접근 거부 → 로그인 페이지로
                }
            }
        }

        // 4. 세션 tableNumber 확인
        Object tableNumber = request.getSession().getAttribute("tableNumber");
        if (tableNumber == null) {
            log.warn("--- [KioskAuthorizationManager] 세션에 tableNumber 없음 → 차단 | URI: {} ---", requestURI);
            return new AuthorizationDecision(false);
        }

        // 5. URL 숫자 세그먼트와 본인 테이블 번호 일치 검증
        String[] parts = requestURI.split("/");
        if (parts.length >= 3) {
            String urlSegment = parts[2];
            if (urlSegment.matches("\\d+")) {
                log.info("--- [KioskAuthorizationManager] URL 번호 검증 | urlSegment: {}, sessionTableNumber: {} ---",
                        urlSegment, tableNumber);
                if (!tableNumber.toString().equals(urlSegment)) {
                    log.warn("--- [KioskAuthorizationManager] 타 테이블 접근 차단 | 본인: {}, URL: {}, URI: {} ---",
                            tableNumber, urlSegment, requestURI);
                    return new AuthorizationDecision(false);
                }
            }
        }

        log.info("--- [KioskAuthorizationManager] 접근 허용 | tableNumber: {}, URI: {} ---",
                tableNumber, requestURI);
        return new AuthorizationDecision(true);
    }
}
