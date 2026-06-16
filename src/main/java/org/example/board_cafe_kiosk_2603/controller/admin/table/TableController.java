package org.example.board_cafe_kiosk_2603.controller.admin.table;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import jakarta.servlet.http.HttpSession;
import org.example.board_cafe_kiosk_2603.dto.admin.table.CafeTableDTO;
import org.example.board_cafe_kiosk_2603.dto.kiosk.order.OrderItemDTO;
import org.example.board_cafe_kiosk_2603.service.admin.cafeTable.CafeTableService;
import org.example.board_cafe_kiosk_2603.service.kiosk.tableSession.TableSessionKioskService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Log4j2
@Controller
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class TableController {
    private final CafeTableService cafeTableService;
    private final TableSessionKioskService tableSessionKioskService;

    /**
     * 대시보드 메인 페이지
       현재 12개 테이블의 실시간 상태(이용중/공석/청소중)와 입실 시간을 조회하여 뷰에 전달
     */
    @GetMapping
    public String dashboard(Model model) {
        log.info("--- TableController dashboard ---");
        List<CafeTableDTO> tables = cafeTableService.getAllTableStatus();
        model.addAttribute("tables", tables);

        log.info("대시보드 조회: 총 {}개의 테이블 상태 로드 완료", tables.size());
        return "admin/dashboard";
    }

    /**
     대시보드 테이블 상태 폴링용 API
     프론트에서 주기적으로 조회하여 상태가 바뀌면 화면을 새로고침
     */
    @ResponseBody
    @GetMapping("/tables")
    public ResponseEntity<List<CafeTableDTO>> getTablesSnapshot() {
        return ResponseEntity.ok(cafeTableService.getAllTableStatus());
    }

    /**
     특정 테이블의 실시간 주문 항목 조회 (AJAX 호출용)
     */
    @ResponseBody
    @GetMapping("/{id}/orders")
    public ResponseEntity<List<OrderItemDTO>> getTableOrders(@PathVariable("id") Integer id) {
        log.info("--- TableController getTableOrders ---");
        /*
         * 서비스에서 해당 테이블의 current_session_id를 추적함.
         * 해당 세션에 묶인 '미결제' 주문 아이템들만 DTO 리스트로 가져옴.
         */
        log.info("API 호출: 테이블 {}번 실시간 주문 내역 요청", id);

        List<OrderItemDTO> activeOrders = cafeTableService.getActiveOrders(id);

        // 데이터가 없더라도 빈 리스트([])와 함께 200 OK를 반환하여 프론트 처리를 원활하게 함
        return ResponseEntity.ok(activeOrders);
    }

    /**
     대시보드에서 정산 페이지 진입
     kiosk 권한 체인을 타지 않고 관리자 권한으로 checkout.html을 렌더링
     */
    @GetMapping("/{id}/checkout")
    public String moveToCheckout(@PathVariable("id") Integer id, HttpSession session, Model model) {
        List<CafeTableDTO> tables = cafeTableService.getAllTableStatus();
        CafeTableDTO table = tables.stream()
                .filter(t -> id.equals(t.getId()))
                .findFirst()
                .orElse(null);

        if (table == null || table.getTableNumber() == null) {
            return "redirect:/admin/dashboard";
        }

        int tableNumber = table.getTableNumber();
        session.setAttribute("tableId", id);
        session.setAttribute("tableNumber", tableNumber);
        session.setAttribute("adminCheckoutMode", true);

        tableSessionKioskService.buildCheckoutModel(model, tableNumber, session);
        model.addAttribute("tableNumber", tableNumber);
        model.addAttribute("checkoutSource", "admin");
        return "kiosk/checkout";
    }

    /**
     대시보드 실시간 정산 금액 계산용 메타 조회
     정산 페이지 진입과 달리 관리자 세션의 결제 대상 테이블을 변경하지 않는다.
     */
    @ResponseBody
    @GetMapping("/{id}/checkout-meta")
    public ResponseEntity<Map<String, Object>> getCheckoutMeta(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(tableSessionKioskService.buildCheckoutMeta(id));
    }

    /**
     테이블 상태 변경 및 세션 연동 (입실/퇴실/청소)
     */
    @ResponseBody
    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> updateStatus(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, String> request) {
        log.info("--- TableController updateStatus ---");
        String status = request.get("status");

        try {
            // 1. 핵심 로직 실행 (토큰 체크 및 세션 생성/종료 포함)
            cafeTableService.changeTableStatus(id, status);

            log.info("상태 변경 성공: 테이블 {}번 -> {}", id, status);
            return ResponseEntity.ok(Map.of("message", "상태가 " + status + "(으)로 변경되었습니다."));

        } catch (IllegalStateException e) {
            // 2. 서비스에서 던진 "토큰 없음" 등의 비즈니스 로직 예외 처리
            log.warn("상태 변경 거부: 테이블 {}번, 사유: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            // 3. 예상치 못한 기타 서버 오류
            log.error("상태 변경 실패: 테이블 {}번, 사유: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "서버 오류가 발생했습니다."));
        }
    }

    /**
     특정 테이블의 액세스 토큰(UUID) 갱신
     태블릿의 로그인이 풀렸거나 보안 갱신이 필요할 때 새로운 8자리 토큰 발급
     */
    @ResponseBody
    @PostMapping("/{id}/token")
    public ResponseEntity<Map<String, String>> refreshToken(@PathVariable("id") Integer id) {
        log.info("--- TableController refreshToken ---");
        try {
            String newToken = cafeTableService.generateNewToken(id);
            log.info("토큰 갱신 완료: 테이블 {}번 -> {}", id, newToken);

            return ResponseEntity.ok(Map.of(
                    "accessToken", newToken,
                    "message", "새로운 인증 토큰이 발급되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     자정 데이터 리셋 강제 실행 (운영/테스트용)
     모든 활성 세션을 강제 종료하고 모든 테이블을 공석(EMPTY)으로 초기화
     */
    @ResponseBody
    @DeleteMapping("/reset")
    public ResponseEntity<Map<String, String>> forceReset() {
        log.info("--- TableController forceReset ---");
        try {
            cafeTableService.resetAllTablesForNewDay();
            log.warn("경고: 관리자에 의한 시스템 전체 강제 리셋이 수행되었습니다.");

            return ResponseEntity.ok(Map.of("message", "모든 테이블과 세션이 초기화되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "리셋 중 오류 발생"));
        }
    }

    /**
     특정 테이블의 미확인 메시지 목록 조회 (모달용)
     */
    @GetMapping("/messages/{tableId}")
    @ResponseBody
    public ResponseEntity<List<String>> getUnreadMessages(@PathVariable("tableId") Integer tableId) {
        log.info("--- TableController getUnreadMessages ---");

        List<String> messages = cafeTableService.getUnreadMessages(tableId);
        return ResponseEntity.ok(messages);
    }

    /**
     특정 테이블의 알림 '읽음' 처리
     */
    @PatchMapping("/messages/{tableId}/read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable("tableId") Integer tableId) {
        log.info("--- TableController markAsRead ---");

        try {
            cafeTableService.markMessagesAsRead(tableId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("알림 읽음 처리 중 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("success", false));
        }
    }
}
