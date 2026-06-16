package org.example.board_cafe_kiosk_2603.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

@Log4j2
public class Handler403 implements AccessDeniedHandler {

    /* 키오스크 계정이 관리자 페이지에 접속하려 할때 발생하는 403 Forbidden(권한 거부) 상황 처리하는 커스텀 핸들러 */
    /*
     * 403 Forbidden 커스텀 핸들러
     *
     * Ajax 요청 → 403 상태코드 + JSON 응답
     *             (fetch()가 리다이렉트를 따라가지 않으므로 JSON으로 처리)
     * 일반 요청 → 리다이렉트
     *             키오스크: /kiosk/login
     *             관리자  : /common/login?error=ACCESS_DENIED
     */

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException)
            throws IOException, ServletException {

        log.warn("--- [Handler403] ACCESS DENIED | URI: {}, Method: {} ---",
                request.getRequestURI(), request.getMethod());

        response.setStatus(HttpStatus.FORBIDDEN.value());

        // Ajax 여부 판단
        // Content-Type: application/json     → fetch() RequestBody JSON
        // Content-Type: application/x-www-form-urlencoded → fetch() URLSearchParams
        // X-Requested-With: XMLHttpRequest   → jQuery Ajax 호환
        String contentType = request.getHeader("Content-Type");
        String requestedWith = request.getHeader("X-Requested-With");

        boolean isAjax = (contentType != null
                && (contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)
                || contentType.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE)))
                || "XMLHttpRequest".equals(requestedWith);

        log.info("--- [Handler403] isAjax: {}, Content-Type: {} ---", isAjax, contentType);

        if (isAjax) {
            // Ajax 요청 → JSON 응답
            // fetch()는 리다이렉트를 자동으로 따라가므로 페이지가 이동되어버림
            // 상태코드 403 + JSON으로 내려야 JS에서 정상적으로 오류 처리 가능
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"ACCESS_DENIED\",\"message\":\"접근 권한이 없습니다.\"}");
            log.warn("--- [Handler403] Ajax 403 응답 | URI: {} ---", request.getRequestURI());
        } else {
            // 일반 요청 → 리다이렉트
//            String redirectUrl = request.getRequestURI().startsWith("/kiosk")
//                    ? "/kiosk/login"
//                    : "/common/login?error=ACCESS_DENIED";
//            log.warn("--- [Handler403] 일반 요청 리다이렉트 | {} → {} ---",
//                    request.getRequestURI(), redirectUrl);
//            response.sendRedirect(redirectUrl);
            // [+] 일반 요청일 경우 리다이렉트
            String uri = request.getRequestURI();

            if (uri.startsWith("/kiosk/")) {
                // 키오스크 관련 경로에서 권한 거부 시
                response.sendRedirect("/kiosk/login");
            } else {
                // 그 외(관리자 페이지 등) 경로에서 권한 거부 시
                response.sendRedirect("/admin/login");
            }

            log.warn("— [Handler403] 리다이렉트 수행 | URI: {} —", uri);
        }
    }
}
