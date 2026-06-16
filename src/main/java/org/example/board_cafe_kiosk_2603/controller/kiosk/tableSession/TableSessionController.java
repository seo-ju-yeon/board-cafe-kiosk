package org.example.board_cafe_kiosk_2603.controller.kiosk.tableSession;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.kiosk.stock.kioskItem;
import org.example.board_cafe_kiosk_2603.service.admin.cafeTable.CafeTableService;
import org.example.board_cafe_kiosk_2603.service.admin.cafeTable.TableSessionAdminService;
import org.example.board_cafe_kiosk_2603.service.admin.product.GameService;
import org.example.board_cafe_kiosk_2603.service.admin.product.MenuService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Log4j2
@Controller
@RequestMapping("/kiosk")
@RequiredArgsConstructor
public class TableSessionController {

    private final GameService gameService;
    private final MenuService menuService;
    private final TableSessionAdminService tableSessionAdminService;
    private final CafeTableService cafeTableService;

    // ===========================================================
    // 진입 화면
    // ===========================================================

    @GetMapping("/screensaver")
    public String screensaver(HttpSession session, Model model) {
        Object tableIdObj = session.getAttribute("tableId");
        if (tableIdObj instanceof Integer tableId) {
            String status = cafeTableService.getTableStatus(tableId);
            if ("CLEANING".equals(status)) {
                model.addAttribute("tableNumber", session.getAttribute("tableNumber"));
                return "kiosk/cleaning_wait";
            }
        }

        model.addAttribute("tableNumber", session.getAttribute("tableNumber"));
        return "kiosk/screensaver";
    }

    @GetMapping("/headcount")
    public String headcount(HttpSession session, Model model) {
        Object tableIdObj = session.getAttribute("tableId");
        if (tableIdObj instanceof Integer tableId) {
            String status = cafeTableService.getTableStatus(tableId);
            if ("CLEANING".equals(status)) {
                model.addAttribute("tableNumber", session.getAttribute("tableNumber"));
                return "kiosk/cleaning_wait";
            }
        }

        model.addAttribute("tableNumber", session.getAttribute("tableNumber"));
        return "kiosk/headcount";
    }

    @GetMapping("/phone_login")
    public String phoneLogin(HttpSession session, Model model) {
        model.addAttribute("tableNumber", session.getAttribute("tableNumber"));
        model.addAttribute("partySize", session.getAttribute("partySize"));
        return "kiosk/phone_login";
    }

    @GetMapping("/cleaning_wait")
    public String cleaningWait(HttpSession session, Model model) {
        model.addAttribute("tableNumber", session.getAttribute("tableNumber"));
        return "kiosk/cleaning_wait";
    }

    // ★ 수정: /session/start → kiosk/headcount 반환
    //    기존에 "kiosk/screensaver"를 반환하고 있었으나
    //    이 엔드포인트의 목적은 인원수 입력 화면 진입이므로 수정
    @GetMapping("/session/start")
    public String sessionStart(HttpSession session, Model model) {
        Object tableIdObj = session.getAttribute("tableId");
        if (tableIdObj instanceof Integer tableId) {
            String status = cafeTableService.getTableStatus(tableId);
            if ("CLEANING".equals(status)) {
                model.addAttribute("tableNumber", session.getAttribute("tableNumber"));
                return "kiosk/cleaning_wait";
            }
        }

        model.addAttribute("tableNumber", session.getAttribute("tableNumber"));
        return "kiosk/screensaver";
    }

    @GetMapping("/table/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> tableStatus(HttpSession session) {
        Object tableIdObj = session.getAttribute("tableId");
        Map<String, Object> body = new LinkedHashMap<>();

        if (!(tableIdObj instanceof Integer tableId)) {
            body.put("success", false);
            body.put("message", "tableId 세션이 없습니다.");
            body.put("status", "UNKNOWN");
            return ResponseEntity.ok(body);
        }

        String status = cafeTableService.getTableStatus(tableId);
        if (status == null) {
            body.put("success", false);
            body.put("message", "테이블 상태를 찾을 수 없습니다.");
            body.put("status", "UNKNOWN");
            return ResponseEntity.ok(body);
        }

        body.put("success", true);
        body.put("status", status);
        return ResponseEntity.ok(body);
    }

    // ===========================================================
    // 메뉴 탭 화면 — DB 조회 및 렌더링 담당
    //
    // ★ 제거: @GetMapping("/menu")
    //    MainController에 이미 /kiosk/menu 가 존재하므로
    //    kioskController에 추가하면 Ambiguous mapping 에러 발생
    //    /kiosk/menu 진입·세션 검증은 MainController.mainMenuPage() 담당
    // ===========================================================

    // 게임 목록 조회
    @GetMapping("/games")
    public String games(HttpSession session, Model model) {
        String guardRedirect = guardActiveSession(session);
        if (guardRedirect != null) return guardRedirect;

        initCart(session);

        List<kioskItem> items = gameService.getByIsActive(true).stream()
                .filter(g -> g.getGameItemCount() > 0)
                .map(g -> kioskItem.builder()
                        .name(g.getName())
                        .price(0)
                        .imageUrl(g.getImageUrl())
                        .stock(g.getGameItemCount())
                        .build())
                .toList();

        buildMenuModel(model, session, "games", "게임", items);
        return "layout/kiosk_layout";
    }

    // 음료 목록 조회
    @GetMapping("/drinks")
    public String drinks(HttpSession session, Model model) {
        String guardRedirect = guardActiveSession(session);
        if (guardRedirect != null) return guardRedirect;

        initCart(session);

        List<kioskItem> items = menuService.getByType("DRINK").stream()
                .filter(m -> m.isAvailable() && !m.isDeleted())
                .map(m -> kioskItem.builder()
                        .name(m.getName())
                        .price(m.getPrice())
                        .imageUrl(m.getImageUrl())
                        .stock(-1)
                        .build())
                .toList();

        buildMenuModel(model, session, "drinks", "음료", items);
        return "layout/kiosk_layout";
    }

    // 음식 목록 조회
    @GetMapping("/food")
    public String food(HttpSession session, Model model) {
        String guardRedirect = guardActiveSession(session);
        if (guardRedirect != null) return guardRedirect;

        initCart(session);

        List<kioskItem> items = menuService.getByType("FOOD").stream()
                .filter(m -> m.isAvailable() && !m.isDeleted())
                .map(m -> kioskItem.builder()
                        .name(m.getName())
                        .price(m.getPrice())
                        .imageUrl(m.getImageUrl())
                        .stock(-1)
                        .build())
                .toList();

        buildMenuModel(model, session, "food", "음식", items);
        return "layout/kiosk_layout";
    }

    // 추가인원 목록 조회
    @GetMapping("/members")
    public String members(HttpSession session, Model model) {
        String guardRedirect = guardActiveSession(session);
        if (guardRedirect != null) return guardRedirect;

        initCart(session);

        List<kioskItem> items = menuService.getByType("GUEST").stream()
                .filter(m -> m.isAvailable() && !m.isDeleted())
                .map(m -> kioskItem.builder()
                        .name(m.getName())
                        .price(m.getPrice())
                        .imageUrl(m.getImageUrl())
                        .stock(-1)
                        .build())
                .toList();

        buildMenuModel(model, session, "members", "추가인원", items);
        return "layout/kiosk_layout";
    }

    // ===========================================================
    // 공통 유틸 메서드
    // ===========================================================

    /**
     * 화면에 필요한 공통 데이터를 Model에 담는다.
     * ★ 추가: packageId 포함 — 패키지 정보 표시 및 타이머 계산에 필요
     */
    private void buildMenuModel(Model model, HttpSession session,
                                String menuType, String title, List<kioskItem> items) {
        Object cart = session.getAttribute("cart");
        int cartCount = cart instanceof List ? ((List<?>) cart).size() : 0;

        model.addAttribute("tableNumber",      session.getAttribute("tableNumber"));
        model.addAttribute("partySize",        getPartySize(session));
        model.addAttribute("packageId",        session.getAttribute("packageId"));        // ★ 추가
        model.addAttribute("currentMenu",      menuType);
        model.addAttribute("pageTitle",        title);
        model.addAttribute("menuItems",        items);
        model.addAttribute("cartCount",        cartCount);
        model.addAttribute("sessionStartTime", session.getAttribute("sessionStartTime"));
        model.addAttribute("durationMinutes",  session.getAttribute("durationMinutes"));
    }

    /**
     * partySize 반환
     * ★ 수정: 세션에 값이 없을 때 IllegalStateException throw → 0 반환으로 변경
     *         throw 방식은 /games, /drinks, /food, /members 전부 500 에러 유발
     *         0 반환 시 뷰에서 "인원수 미선택" 상태로 안전하게 표시 가능
     *         /kiosk/menu 진입 시점에 MainController에서 이미 세션 검증을 하므로
     *         정상 흐름에서는 이 분기에 도달하지 않음
     */
    private int getPartySize(HttpSession session) {
        Object val = session.getAttribute("partySize");
        if (!(val instanceof Integer)) {
            log.warn("--- [KioskController] partySize 세션 없음 — 0 반환 ---");
            return 0;
        }
        return (Integer) val;
    }

    /**
     * 장바구니가 세션에 없으면 빈 리스트로 초기화 (null 방지)
     */
    private void initCart(HttpSession session) {
        if (session.getAttribute("cart") == null) {
            session.setAttribute("cart", new ArrayList<>());
        }
    }

    private String guardActiveSession(HttpSession session) {
        Object tableIdObj = session.getAttribute("tableId");
        if (!(tableIdObj instanceof Integer tableId)) {
            log.warn("--- [KioskController] 메뉴 탭 접근 차단: tableId 세션 없음 ---");
            return "redirect:/kiosk/session/start";
        }

        Long recoverSessionId = cafeTableService.findActiveSessionByTableId(tableId);
        String tableStatus = cafeTableService.getTableStatus(tableId);
        Long currentSessionId = cafeTableService.findCurrentSessionId(tableId);
        if (recoverSessionId != null &&
                (!"OCCUPIED".equals(tableStatus) || currentSessionId == null || !recoverSessionId.equals(currentSessionId))) {
            cafeTableService.syncTableWithSession(tableId, recoverSessionId);
            tableStatus = "OCCUPIED";
            log.warn("--- [KioskController] 메뉴 탭 진입 전 상태/세션 자동 복구 완료 (tableId: {}, sessionId: {}) ---",
                    tableId, recoverSessionId);
        }

        if (!"OCCUPIED".equals(tableStatus)) {
            log.warn("--- [KioskController] 메뉴 탭 접근 차단: 대시보드 상태가 OCCUPIED 아님 (tableId: {}, status: {}) ---",
                    tableId, tableStatus);
            session.removeAttribute("partySize");
            session.removeAttribute("packageId");
            session.removeAttribute("sessionStartTime");
            session.removeAttribute("durationMinutes");
            return "redirect:/kiosk/session/start";
        }

        // 대시보드 기준(OCCUPIED)이면 진입 허용하되, 활성 세션 누락 시 자동 복구 시도
        if (tableSessionAdminService.getActiveSession(tableId) == null) {
            if (recoverSessionId != null) {
                cafeTableService.syncTableWithSession(tableId, recoverSessionId);
                log.warn("--- [KioskController] OCCUPIED 상태-세션 불일치 복구 완료 (tableId: {}, sessionId: {}) ---",
                        tableId, recoverSessionId);
            } else {
                log.warn("--- [KioskController] OCCUPIED 상태지만 활성 세션 없음 (tableId: {}) - 대시보드 기준으로 진입 허용 ---",
                        tableId);
            }
        }

        return null;
    }
}
