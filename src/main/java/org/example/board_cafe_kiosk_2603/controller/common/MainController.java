package org.example.board_cafe_kiosk_2603.controller.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.example.board_cafe_kiosk_2603.domain.common.cafeTableSession.CafeTableSession;
import org.example.board_cafe_kiosk_2603.service.admin.cafeTable.CafeTableService;
import org.example.board_cafe_kiosk_2603.service.admin.cafeTable.TableSessionAdminService;
import org.example.board_cafe_kiosk_2603.service.kiosk.tableSession.TableSessionKioskService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Log4j2
@Controller
@RequiredArgsConstructor
public class MainController {

    private final TableSessionAdminService tableSessionAdminService;
    private final TableSessionKioskService tableSessionKioskService;
    private final CafeTableService cafeTableService;

    // ===========================================================
    // 공통 로그인 페이지 라우팅
    // ===========================================================

    @GetMapping("/")
    public String root() {
        return "redirect:/common/login";
    }

    @GetMapping("/common/login")
    public String loginPage() {
        return "common/login";
    }

    @GetMapping("/admin/login")
    public String adminLoginPage() {
        return "login/admin_login";
    }

    @GetMapping("/kiosk/login")
    public String kioskLoginPage(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        return "login/kiosk_login";
    }

    // ===========================================================
    // ★ 수정: @PostMapping("/kiosk/login") 제거
    //
    //    기존 문제:
    //      - @PostMapping("/kiosk/login")이 Spring Security 인증을 우회하여
    //        KioskLoginSuccessHandler를 경유하지 않음
    //      - partySize/packageId 세션 복구 로직이 실행되지 않았음
    //
    //    해결:
    //      - 로그인 POST 처리는 SecurityConfig의
    //        .loginProcessingUrl("/kiosk/login-process")에 전적으로 위임
    //      - 인증 성공 후 KioskLoginSuccessHandler가 세션 복구 및 리다이렉트 담당
    //
    //    뷰(kiosk_login.html) 수정 필요:
    //      form action="/kiosk/login"  → form action="/kiosk/login-process"
    // ===========================================================

    // ===========================================================
    // ===== ⛔️ 디버깅용 (운영 시 제거) ⛔️ =====
    @GetMapping("/kiosk/force-logout")
    public String kioskForceLogout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            log.info("강제 로그아웃 - 테이블: {}", session.getAttribute("tableNumber"));
            session.invalidate();
        }
        return "redirect:/kiosk/login";
    }
    // ===== //⛔️ 디버깅용 ⛔️ =====

    // ===========================================================
    // 키오스크 POST 흐름
    // screensaver → headcount → phone_login → package_selection → menu
    // ===========================================================

    /* [1단계] 인원수 선택 처리 - partySize 세션 저장 */
    @PostMapping("/kiosk/headcount")
    public String headcountProcess(@RequestParam int partySize,
                                   HttpSession session) {
        session.setAttribute("partySize", partySize);
        return "redirect:/kiosk/phone_login";
    }

    /* [2단계] 전화번호 처리 - customerPhone 세션 저장 */
    @PostMapping("/kiosk/phone_login")
    public String phoneLoginProcess(@RequestParam(required = false) String phone,
                                    HttpSession session) {
        if (phone != null && !phone.isEmpty()) {
            session.setAttribute("customerPhone", phone);
        } else {
            session.removeAttribute("customerPhone");
        }
        return "redirect:/kiosk/package_selection";
    }

    /* [3단계] 패키지 선택 처리 - table_session DB 저장 */
    @PostMapping("/kiosk/package_selection")
    public String packageSelectionProcess(@RequestParam int packageId,
                                          HttpSession session) {
        int tableId   = (int) session.getAttribute("tableId");
        int partySize = (int) session.getAttribute("partySize");

        // 이미 활성 세션이 있으면 새로 생성하지 않고 기존 세션 정보 유지
        CafeTableSession activeSession = tableSessionAdminService.getActiveSession(tableId);
        if (activeSession != null) {
            session.setAttribute("partySize", activeSession.getInitialGuestCnt());
            session.setAttribute("packageId", activeSession.getPackageId()); // ★ 추가
            log.info("기존 활성 세션 존재 — 기존 인원수: {}명, packageId: {}",
                    activeSession.getInitialGuestCnt(), activeSession.getPackageId());
            return "redirect:/kiosk/drinks";
        }

        // 활성 세션 없으면 신규 생성 후 cafe_table 동기화
        Long newSessionId = tableSessionKioskService.createSession(tableId, packageId, partySize);
        cafeTableService.syncTableWithSession(tableId, newSessionId);

        // ★ 추가: packageId를 세션에 저장
        //    기존 코드에서 누락되어 buildMenuModel()의 packageId가 항상 null이었음
        session.setAttribute("packageId", packageId);

        log.info("table_session 생성 + cafe_table 동기화 완료 — tableId: {}, packageId: {}, partySize: {}, sessionId: {}",
                tableId, packageId, partySize, newSessionId);

        return "redirect:/kiosk/drinks";
    }

    /* [4단계] 메인 메뉴 페이지
     * ★ 수정: partySize 세션 유효성 검사 추가
     *         세션에 partySize 없으면 /kiosk/session/start 로 리다이렉트
     *         (기존: 검증 없이 그냥 화면 진입 → 비정상 렌더링)
     */
    @GetMapping("/kiosk/menu")
    public String mainMenuPage(HttpSession session,
                               HttpServletResponse response) throws IOException {
        Object tableIdObj = session.getAttribute("tableId");
        if (!(tableIdObj instanceof Integer tableId)) {
            log.warn("--- [MainController] /kiosk/menu 진입 시 tableId 없음 → /kiosk/session/start 리다이렉트 ---");
            response.sendRedirect("/kiosk/session/start");
            return null;
        }

        Long recoverSessionId = cafeTableService.findActiveSessionByTableId(tableId);
        String tableStatus = cafeTableService.getTableStatus(tableId);
        Long currentSessionId = cafeTableService.findCurrentSessionId(tableId);
        if (recoverSessionId != null &&
                (!"OCCUPIED".equals(tableStatus) || currentSessionId == null || !recoverSessionId.equals(currentSessionId))) {
            cafeTableService.syncTableWithSession(tableId, recoverSessionId);
            tableStatus = "OCCUPIED";
            log.warn("--- [MainController] 메뉴 진입 전 상태/세션 자동 복구 완료 (tableId: {}, sessionId: {}) ---",
                    tableId, recoverSessionId);
        }

        if (!"OCCUPIED".equals(tableStatus)) {
            log.warn("--- [MainController] /kiosk/menu 진입 차단: 대시보드 상태가 OCCUPIED 아님 (tableId: {}, status: {}) ---",
                    tableId, tableStatus);
            session.removeAttribute("partySize");
            session.removeAttribute("packageId");
            session.removeAttribute("sessionStartTime");
            session.removeAttribute("durationMinutes");
            response.sendRedirect("/kiosk/session/start");
            return null;
        }

        CafeTableSession activeSession = tableSessionAdminService.getActiveSession(tableId);
        if (activeSession == null) {
            if (recoverSessionId != null) {
                cafeTableService.syncTableWithSession(tableId, recoverSessionId);
                activeSession = tableSessionAdminService.getActiveSession(tableId);
                log.warn("--- [MainController] OCCUPIED 상태-세션 불일치 복구 완료 (tableId: {}, sessionId: {}) ---",
                        tableId, recoverSessionId);
            } else {
                log.warn("--- [MainController] OCCUPIED 상태지만 활성 세션 없음 (tableId: {}) - 대시보드 기준으로 메뉴 진입 허용 ---",
                        tableId);
            }
        }

        if (activeSession != null) {
            session.setAttribute("partySize", activeSession.getInitialGuestCnt());
            session.setAttribute("packageId", activeSession.getPackageId());
            long checkInMillis = activeSession.getCheckInTime()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            session.setAttribute("sessionStartTime", checkInMillis);
        }

        log.info("키오스크 -> 메뉴 진입 검증 완료(대시보드 기준) | tableNumber: {}, partySize: {}, packageId: {}",
                session.getAttribute("tableNumber"),
                session.getAttribute("partySize"),
                session.getAttribute("packageId"));

        return "redirect:/kiosk/drinks";
    }

    // ===========================================================
    // 관리자
    // ===========================================================

    @GetMapping("/admin/find_pw")
    public String findPwPage() {
        return "login/find_pw";
    }

    // AI Service
    @GetMapping("/kiosk/ai")
    public void aiService() {
    }
}
