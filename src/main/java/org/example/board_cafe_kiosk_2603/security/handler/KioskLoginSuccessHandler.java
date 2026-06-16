package org.example.board_cafe_kiosk_2603.security.handler;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.common.cafeTableSession.CafeTableSession; // ★ 수정: 올바른 패키지 경로
import org.example.board_cafe_kiosk_2603.security.dto.KioskDTO;
import org.example.board_cafe_kiosk_2603.service.admin.cafeTable.CafeTableService;
import org.example.board_cafe_kiosk_2603.service.admin.cafeTable.TableSessionAdminService;
import org.example.board_cafe_kiosk_2603.service.kiosk.cafePackage.CafePackageService;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

@Log4j2
@Component
@RequiredArgsConstructor
public class KioskLoginSuccessHandler implements AuthenticationSuccessHandler {
    // instanceof 분기가 생기는 순간 단일 책임 원칙(SRP) 위반되므로,
    // Kiosk, Admin - LoginSuccessHandler 2EA의 파일로 관리

    // 목적 - 테이블 세션 관리, access_token 갱신, DB 동기화

    private final CafeTableService cafeTableService;

    // ★ 추가: 재로그인 시 활성 세션 상세 정보(CafeTableSession 객체)를
    //         HTTP 세션에 복구하기 위해 주입
    private final TableSessionAdminService tableSessionAdminService;
    private final CafePackageService cafePackageService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        KioskDTO kiosk = (KioskDTO) authentication.getPrincipal();
        int tableId = kiosk.getTableId();
        log.info("--- [KioskLoginSuccess] 로그인 성공 | tableId: {}, tableNumber: {} ---",
                tableId, kiosk.getUsername());

        // ────────────────────────────────────────────────────────
        // 1. 기본 세션 세팅
        //    tableNumber는 Integer로 저장
        //    이유: CartController 등에서 (Integer) session.getAttribute("tableNumber")로
        //          캐스팅하므로 String 저장 시 ClassCastException 발생
        //
        //    ★ cart는 활성 세션 존재 여부 확인 후 조건부 초기화
        //      (기존: 로그인마다 무조건 빈 리스트로 덮어씀 → 재로그인 시 장바구니 소멸)
        // ────────────────────────────────────────────────────────
        request.getSession().setAttribute("tableNumber", Integer.parseInt(kiosk.getUsername()));
        request.getSession().setAttribute("tableId", tableId);
        request.getSession().setMaxInactiveInterval(60 * 60 * 8); // 세션 유지 8시간

        // ────────────────────────────────────────────────────────
        // 2. access_token 갱신
        //    로그인 성공 시마다 새 UUID 발급 후 DB 저장
        //    자정 초기화 후 재로그인 시에도 토큰 복구됨
        // ────────────────────────────────────────────────────────
        String newToken = UUID.randomUUID().toString();
        cafeTableService.updateAccessToken(tableId, newToken);

        // ────────────────────────────────────────────────────────
        // 3. ★ 수정: getActiveSession() 단일 호출로 통합
        //
        //    기존 흐름 (DB 쿼리 2번):
        //      ① cafeTableService.findActiveSessionByTableId()  → activeSessionId(Long) 반환
        //      ② tableSessionAdminService.getActiveSession()    → CafeTableSession 객체 반환
        //
        //    개선 흐름 (DB 쿼리 1번):
        //      ① tableSessionAdminService.getActiveSession()    → CafeTableSession 객체 반환
        //         null 여부로 활성 세션 존재 판단까지 통합
        //
        //    cafe_table.current_session_id 동기화 검사는
        //    CafeTableSession.getId()로 대체
        // ────────────────────────────────────────────────────────
        CafeTableSession activeSession = tableSessionAdminService.getActiveSession(tableId);
        Long activeSessionId = (activeSession != null) ? activeSession.getId() : null;

        if (activeSession != null) {

            // ────────────────────────────────────────────────────
            // 3-1. cafe_table 동기화
            //      cafe_table.current_session_id 와 실제 활성 세션 ID가 다를 경우 복구
            //      (서버 재기동, 강제 초기화 등으로 불일치 발생 방어)
            // ────────────────────────────────────────────────────
            Long currentSessionId = cafeTableService.findCurrentSessionId(tableId);
            String tableStatus = cafeTableService.getTableStatus(tableId);
            boolean needsSync = currentSessionId == null
                    || !currentSessionId.equals(activeSessionId)
                    || !"OCCUPIED".equals(tableStatus);

            if (needsSync) {
                log.warn("--- [KioskLoginSuccess] 데이터 불일치 감지 | " +
                                "cafe_table.current_session_id: {}, table_session.id: {}, status: {} → 복구 시작 ---",
                        currentSessionId, activeSessionId, tableStatus);

                cafeTableService.syncTableWithSession(tableId, activeSessionId);
            }

            // ────────────────────────────────────────────────────
            // 3-2. ★ 핵심 수정: 활성 세션 정보를 HTTP 세션에 복구
            //
            //      재로그인 시 /kiosk/session/start 를 거치지 않으므로
            //      partySize, packageId, sessionStartTime 이 HTTP 세션에 없는 문제 해결
            //
            //      [durationMinutes 복구 제외 이유]
            //      durationMinutes 는 cafe_package 테이블 소속 컬럼으로
            //      CafeTableSession 도메인 범위 밖에 해당함
            //      → CafeTableSession 에 필드를 추가하면 도메인 책임이 오염됨
            //      → packageId 를 세션에 저장해두면 Controller/View 레이어에서
            //        packageId 기반으로 조회하여 사용 가능하므로 여기서는 생략
            // ────────────────────────────────────────────────────
            request.getSession().setAttribute("partySize",        activeSession.getInitialGuestCnt());
            request.getSession().setAttribute("packageId",        activeSession.getPackageId());
            long checkInMillis = activeSession.getCheckInTime()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            request.getSession().setAttribute("sessionStartTime", checkInMillis);
            if (activeSession.getPackageId() != null) {
                var pkg = cafePackageService.getById(activeSession.getPackageId());
                if (pkg != null) {
                    request.getSession().setAttribute("durationMinutes", pkg.getDurationMinutes());
                }
            }

            // ────────────────────────────────────────────────────
            // 3-3. ★ 수정: cart 조건부 초기화
            //      활성 세션이 있을 때는 기존 cart 유지
            //      세션에 cart 자체가 없을 때만 빈 리스트로 초기화
            // ────────────────────────────────────────────────────
            if (request.getSession().getAttribute("cart") == null) {
                request.getSession().setAttribute("cart", new ArrayList<>());
            }

            // 진행 중인 세션 있음 → 기본 메뉴 탭(음료)로 이동
            log.info("--- [KioskLoginSuccess] 활성 세션 재진입 | tableId: {}, redirect: /kiosk/drinks ---", tableId);
            response.sendRedirect("/kiosk/drinks");

        } else {
            // ────────────────────────────────────────────────────
            // 4. 활성 세션 없음 → 최초 로그인 또는 정산 완료 후 신규 입장
            //    cart를 새 빈 리스트로 초기화
            // ────────────────────────────────────────────────────
            request.getSession().setAttribute("cart", new ArrayList<>());
            log.info("--- [KioskLoginSuccess] 신규 세션 진입 | tableId: {}, redirect: /kiosk/session/start ---", tableId);
            response.sendRedirect("/kiosk/session/start");
        }
    }

    @PostConstruct
    public void init() {
        log.info("--- [KioskLoginSuccessHandler] 주입된 서비스: {} / {} ---",
                cafeTableService.getClass().getName(),
                tableSessionAdminService.getClass().getName());
    }
}
