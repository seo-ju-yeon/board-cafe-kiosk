package org.example.board_cafe_kiosk_2603.controller.kiosk.order;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.dto.admin.product.GameResponseDTO;
import org.example.board_cafe_kiosk_2603.dto.kiosk.order.OrderCreateRequest;
import org.example.board_cafe_kiosk_2603.dto.kiosk.order.OrderItemDTO;
import org.example.board_cafe_kiosk_2603.dto.kiosk.order.OrdersDTO;
import org.example.board_cafe_kiosk_2603.service.admin.product.GameService;
import org.example.board_cafe_kiosk_2603.service.kiosk.order.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 키오스크 주문 관리 컨트롤러.
 *
 * [페이지]
 * GET    /kiosk/order/{orderId}            → 주문 상세 페이지
 *
 * [REST API]
 * POST   /kiosk/order/create               → 주문 생성 (카트 기반)
 * GET    /kiosk/order/api/{orderId}        → 주문 단건 조회 (JSON)
 * GET    /kiosk/order/latest               → 테이블 최근 주문 조회 (세션 기반)
 * GET    /kiosk/order/session/{sessionId}  → 세션 전체 주문 목록 조회
 * PATCH  /kiosk/order/{orderId}/status     → 주문 상태 변경
 * DELETE /kiosk/order/{orderId}            → 주문 취소
 */
@Log4j2
@Controller
@RequestMapping("/kiosk/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final GameService gameService;

    // ===========================================================
    // 페이지
    // ===========================================================

    /**
     * 주문 상세 페이지
     * GET /kiosk/order/{orderId}
     *
     * 주문 상태 확인 페이지
     * - 주문 내용 표시
     * - 상태 실시간 표시
     * - 결제로 진행하기 버튼
     */
    @GetMapping("/{orderId}")
    public String orderDetailPage(@PathVariable int orderId, Model model, HttpSession session) {
        Integer tableNumber = sessionTableNumber(session);
        if (tableNumber == null) return "redirect:/kiosk";

        if (!orderService.isOrderOwnedByTableNumber(orderId, tableNumber)) {
            log.warn("주문 상세 접근 차단 - orderId: {}, tableNumber: {}", orderId, tableNumber);
            return "redirect:/kiosk/cart";
        }

        // 주문 정보 조회
        OrdersDTO order = orderService.getOrder(orderId);
        if (!order.isSuccess()) {
            log.warn("주문 조회 실패 - orderId: {}", orderId);
            return "redirect:/kiosk/cart";
        }

        // 모델에 데이터 추가
        boolean isGameOnlyOrder = isGameOnlyOrder(order);
        model.addAttribute("order", order);
        model.addAttribute("orderId", orderId);
        model.addAttribute("tableNumber", tableNumber);
        model.addAttribute("isGameOnlyOrder", isGameOnlyOrder);
        model.addAttribute("statusDisplay", getStatusDisplay(order.getStatus(), isGameOnlyOrder));

        return "kiosk/order_detail";
    }

    /**
     * 게임 주문 상세 페이지
     * GET /kiosk/game/{orderId}
     */
    @GetMapping("/game/{orderId}")
    public String gameDetailPage(@PathVariable int orderId, Model model, HttpSession session) {
        Integer tableNumber = sessionTableNumber(session);
        if (tableNumber == null) return "redirect:/kiosk";

        if (!orderService.isOrderOwnedByTableNumber(orderId, tableNumber)) {
            return "redirect:/kiosk/cart";
        }

        OrdersDTO order = orderService.getOrder(orderId);
        if (!order.isSuccess()) {
            return "redirect:/kiosk/cart";
        }

        List<OrderItemDTO> orderItems = order.getItems() == null ? List.of() : order.getItems();
        List<OrderItemDTO> gameItems = orderItems.stream()
                .filter(item -> item != null && item.getPrice() == 0)
                .toList();

        if (gameItems.isEmpty()) {
            return "redirect:/kiosk/order/" + orderId;
        }

        List<String> gameNames = gameItems.stream()
                .map(OrderItemDTO::getMenuName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .toList();

        Map<String, GameResponseDTO> gameInfoMap = gameService.getByNames(gameNames).stream()
                .collect(Collectors.toMap(GameResponseDTO::getName, Function.identity(), (a, b) -> a));

        List<Map<String, Object>> requestedGames = new ArrayList<>();
        for (OrderItemDTO gameItem : gameItems) {
            String gameName = gameItem.getMenuName();
            GameResponseDTO info = gameInfoMap.get(gameName);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", gameName);
            row.put("quantity", gameItem.getQuantity());
            row.put("imageUrl", info != null ? info.getImageUrl() : null);
            row.put("minPlayers", info != null ? info.getMinPlayers() : null);
            row.put("maxPlayers", info != null ? info.getMaxPlayers() : null);
            row.put("playTime", info != null ? info.getPlayTime() : null);
            row.put("description", info != null ? info.getDescription() : null);
            requestedGames.add(row);
        }

        model.addAttribute("order", order);
        model.addAttribute("orderId", orderId);
        model.addAttribute("tableNumber", tableNumber);
        model.addAttribute("requestedGames", requestedGames);
        return "kiosk/game_detail";
    }

    // ===========================================================
    // REST API - 주문 생성
    // ===========================================================

    /**
     * 카트에서 주문 생성
     * POST /kiosk/order/create
     *
     * 요청: {
     *   "tableNumber": 5,
     *   "totalAmount": 15000,
     *   "customerPhone": "010-1234-5678" (optional)
     * }
     *
     * 응답: {
     *   "success": true,
     *   "id": 123,
     *   "status": "ORDERED",
     * }
     */
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<OrdersDTO> createOrder(
            @RequestBody OrderCreateRequest request,
            HttpSession session) {
        Integer tableNumber = sessionTableNumber(session);

        if (tableNumber == null) {
            log.warn("테이블 번호 없음 - 주문 생성 실패");
            return ResponseEntity.badRequest()
                    .body(OrdersDTO.builder()
                            .success(false)
                            .message("테이블 정보가 없습니다.")
                            .build());
        }

        try {
            OrdersDTO result = orderService.createOrderFromCart(
                    tableNumber,
                    request.getCustomerPhone(),
                    request.getTotalAmount()
            );

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                log.warn("주문 생성 실패 - message: {}", result.getMessage());
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            log.error("주문 생성 중 예외 발생", e);
            return ResponseEntity.internalServerError()
                    .body(OrdersDTO.builder()
                            .success(false)
                            .message("주문 생성 중 오류가 발생했습니다: " + e.getMessage())
                            .build());
        }
    }

    // ===========================================================
    // REST API - 조회
    // ===========================================================

    /**
     * 신규 주문 목록 조회 (PENDING 상태)
     * GET /kiosk/order/pending
     *
     * 관리자 대시보드에서 신규 주문 알림 조회용
     */
    @GetMapping("/pending")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPendingOrders() {
        try {
            List<OrdersDTO> orders = orderService.getNewOrders();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orders", orders,
                    "count", orders.size()
            ));
        } catch (Exception e) {
            log.error("신규 주문 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", "신규 주문 조회 실패"
                    ));
        }
    }

    /**
     * 주문 단건 조회 (JSON)
     * GET /kiosk/order/api/{orderId}
     */
    @GetMapping("/api/{orderId}")
    @ResponseBody
    public ResponseEntity<OrdersDTO> getOrderApi(@PathVariable int orderId, HttpSession session) {
        Integer tableNumber = sessionTableNumber(session);
        if (tableNumber == null) return ResponseEntity.badRequest().build();
        if (!orderService.isOrderOwnedByTableNumber(orderId, tableNumber)) {
            log.warn("주문 API 접근 차단 - orderId: {}, tableNumber: {}", orderId, tableNumber);
            return ResponseEntity.status(403).build();
        }
        OrdersDTO result = orderService.getOrder(orderId);
        return result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
    }

    /**
     * 테이블 최근 주문 조회 (JSON)
     * GET /kiosk/order/latest
     */
    @GetMapping("/latest")
    @ResponseBody
    public ResponseEntity<OrdersDTO> getLatestOrder(HttpSession session) {
        Integer tableNumber = sessionTableNumber(session);
        if (tableNumber == null) return ResponseEntity.badRequest().build();
        OrdersDTO result = orderService.getLatestOrderByTableNumber(tableNumber);
        return result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
    }

    /**
     * 세션 주문 목록 조회 (JSON)
     * GET /kiosk/order/session/{sessionId}
     */
    @GetMapping("/session/{sessionId}")
    @ResponseBody
    public ResponseEntity<List<OrdersDTO>> getOrdersBySession(@PathVariable long sessionId, HttpSession session) {
        Integer tableNumber = sessionTableNumber(session);
        if (tableNumber == null) return ResponseEntity.badRequest().build();
        if (!orderService.isSessionOwnedByTableNumber(sessionId, tableNumber)) {
            log.warn("세션 주문 목록 접근 차단 - sessionId: {}, tableNumber: {}", sessionId, tableNumber);
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(orderService.getOrdersBySessionId(sessionId));
    }

    /**
     * 현재 활성 세션의 주문 목록 조회 (장바구니 진입 버튼용)
     * GET /kiosk/order/active
     */
    @GetMapping("/active")
    @ResponseBody
    public ResponseEntity<List<OrdersDTO>> getActiveOrders(HttpSession session) {
        Integer tableNumber = sessionTableNumber(session);
        if (tableNumber == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(orderService.getActiveSessionOrders(tableNumber));
    }

    // ===========================================================
    // REST API - 상태 변경 / 취소
    // ===========================================================

    /**
     * 주문 상태 변경
     * PATCH /kiosk/order/{orderId}/status
     * body: { "status": "ORDERED" }
     * 상태 전이 규칙:
     * - ORDERED   → CONFIRMED (주문확인)
     * - CONFIRMED → COOKING   (조리시작)
     * - COOKING   → DELIVERING(서빙시작)
     * - DELIVERING→ COMPLETED (서빙완료)
     * - (모든 상태) → CANCELLED (취소)
     */
    @PatchMapping("/{orderId}/status")
    @ResponseBody
    public ResponseEntity<OrdersDTO> updateStatus(@PathVariable int orderId,
                                                  @RequestBody Map<String, String> body,
                                                  HttpSession session) {
        Integer tableNumber = sessionTableNumber(session);
        if (tableNumber == null) return ResponseEntity.badRequest().build();
        if (!orderService.isOrderOwnedByTableNumber(orderId, tableNumber)) {
            log.warn("주문 상태 변경 차단 - orderId: {}, tableNumber: {}", orderId, tableNumber);
            return ResponseEntity.status(403).build();
        }
        String status = body.get("status");
        if (status == null || status.isBlank()) return ResponseEntity.badRequest().build();
        OrdersDTO result = orderService.updateStatus(orderId, status);
        return result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    /**
     * 주문 취소
     * DELETE /kiosk/order/{orderId}
     */
    @DeleteMapping("/{orderId}")
    @ResponseBody
    public ResponseEntity<OrdersDTO> cancelOrder(@PathVariable int orderId, HttpSession session) {
        Integer tableNumber = sessionTableNumber(session);
        if (tableNumber == null) return ResponseEntity.badRequest().build();
        if (!orderService.isOrderOwnedByTableNumber(orderId, tableNumber)) {
            log.warn("주문 취소 차단 - orderId: {}, tableNumber: {}", orderId, tableNumber);
            return ResponseEntity.status(403).build();
        }
        OrdersDTO result = orderService.cancelOrder(orderId);
        return result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    // ===========================================================
    // 헬퍼
    // ===========================================================

    /**
     * 주문 상태를 한글 메시지로 변환
     */
    private String getStatusDisplay(String status, boolean isGameOnlyOrder) {
        if (isGameOnlyOrder) {
            return switch (status) {
                case "ORDERED" -> "게임 요청 완료! 일련번호 배정 대기 중...";
                case "CONFIRMED" -> "일련번호 배정 완료! 대여 준비가 끝났어요.";
                case "COOKING", "DELIVERING" -> "게임 대여 처리 중...";
                case "COMPLETED" -> "게임 처리 완료!";
                case "CANCELLED" -> "요청 취소됨";
                default -> "상태 확인 중...";
            };
        }
        return switch (status) {
            case "ORDERED"   -> "주문 완료! 관리자 확인 중...";
            case "CONFIRMED" -> "주문 확인! 조리 시작...";
            case "COOKING"   -> "조리 중입니다...";
            case "DELIVERING"-> "조리 완료! 서빙 중...";
            case "COMPLETED" -> "서빙 완료!";
            case "CANCELLED" -> "주문 취소됨";
            default -> "상태 확인 중...";
        };
    }

    private boolean isGameOnlyOrder(OrdersDTO order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            return false;
        }
        return order.getItems().stream()
                .allMatch(item -> item != null && item.getPrice() == 0);
    }

    private Integer sessionTableNumber(HttpSession session) {
        Object raw = session.getAttribute("tableNumber");
        if (raw == null) return null;
        if (raw instanceof Integer n) return n;
        try {
            int parsed = Integer.parseInt(raw.toString());
            session.setAttribute("tableNumber", parsed);
            return parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
