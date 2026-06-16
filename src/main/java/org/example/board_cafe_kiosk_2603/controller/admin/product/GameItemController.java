package org.example.board_cafe_kiosk_2603.controller.admin.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.product.GameItemStatus;
import org.example.board_cafe_kiosk_2603.dto.admin.product.GameItemRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.product.GameItemResponseDTO;
import org.example.board_cafe_kiosk_2603.service.admin.product.GameItemService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Controller
@RequestMapping("/admin/product/game-items")
@RequiredArgsConstructor
public class GameItemController {
    /* 게임 아이템(재고) CRUD 컨트롤러 */
    // 재고 목록 조회
    // 개별 아이템 등록/수정/삭제
    // 아이템 상태(대여가능/수리중 등) 관리

    private final GameItemService gameItemService;

    /**
     * 게임 아이템 전체 목록 조회 → 뷰 반환
     * GET /admin/product/game-items
     */
    /* 게임 아이템 전체 목록 조회 */
    @GetMapping
    public String getAll(Model model) {
        log.info("--- 게임 아이템 전체 목록 조회 요청 ---");
        try {
            List<GameItemResponseDTO> gameItemList = gameItemService.getAll();
            model.addAttribute("gameItemList", gameItemList);
            model.addAttribute("activePage", "productReg");
            log.debug("--- 게임 아이템 목록 조회 완료 - 건수: {} ---", gameItemList.size());
        } catch (Exception e) {
            log.error("--- 게임 아이템 목록 로드 중 오류 발생: {}", e.getMessage());
        }
        return "admin/product_game";
    }


    /**
     * 게임 아이템 수정 처리
     * POST /admin/product/game-items/edit/{id}
     */
    /* 게임 아이템 정보 수정 처리 */
    @PostMapping("/edit/{id}")
    public String modify(@PathVariable int id, @ModelAttribute GameItemRequestDTO gameItemRequestDTO) {
        log.info("--- 게임 아이템 수정 시작 (ItemID: {}) ---", id);
        log.debug("--- 수정 요청 데이터: {} ---", gameItemRequestDTO);

        gameItemService.modify(id, gameItemRequestDTO);

        log.info("게임 아이템 수정 성공 - id: {}", id);
        return "redirect:/admin/product/game";
    }

    /**
     * 게임 아이템 삭제 처리
     * POST /admin/product/game-items/delete/{id}
     */
    /* 삭제 아이템(재고) 삭제 처리 */
    // 삭제 가능 상태: DAMAGED, LOST
    // 삭제 불가 상태: NORMAL, RENTED → 백엔드에서 IllegalStateException 발생
    @PostMapping("/delete/{id}")
    public String remove(@PathVariable int id,
                         RedirectAttributes redirectAttributes) {
        log.info("--- 게임 아이템 삭제 요청 (ItemID: {}) ---", id);
//        gameItemService.remove(id);
        try {
            gameItemService.remove(id);
            log.info("--- 게임 아이템 삭제 완료 (ItemID: {}) ---", id);
            redirectAttributes.addFlashAttribute("successMessage", "아이템이 삭제되었습니다.");

        } catch (IllegalStateException e) {
            // NORMAL / RENTED 상태 → 삭제 불가
            log.warn("--- 게임 아이템 삭제 거부 (ItemID: {}) - 사유: {} ---", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        log.debug("게임 아이템 삭제 완료 - id: {}", id);
        return "redirect:/admin/product/game";
    }

    /* 게임 아이템 상태 변경 (단독 변경) */
    // ex) 대여가능 -> 수리중/분실 변경 시 사용
    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable int id,
                               @RequestParam GameItemStatus status) {
        log.info("--- 아이템 상태 변경 요청 (id: {}, status: {}) ---", id, status);

        gameItemService.changeStatus(id, status);

        log.info("--- 상태 변경 완료 (id: {}, status: {}) ---", id, status);
        return "redirect:/admin/product/game";
    }

    /**
     * 대시보드/관리자용: 게임명 기준 대여 가능한 시리얼 조회
     * GET /admin/product/game-items/available?tableId=1&gameName=...
     */
    @GetMapping("/available")
    @ResponseBody
    public List<GameItemResponseDTO> getAvailableGameItems(
            @RequestParam int tableId,
            @RequestParam String gameName) {
        log.info("대여 가능 시리얼 조회 - tableId: {}, gameName: {}", tableId, gameName);
        return gameItemService.getAvailableByGameName(gameName);
    }

    /**
     * 대시보드/관리자용: 주문에 시리얼 할당 후 RENTED 전환 + game_history 생성
     * POST /admin/product/game-items/assign
     * body: { tableId, orderId, gameName, gameItemIds: [] }
     */
    @PostMapping("/assign")
    @ResponseBody
    public Map<String, Object> assignGameItems(@RequestBody Map<String, Object> body) {
        int tableId = Integer.parseInt(String.valueOf(body.get("tableId")));
        int orderId = Integer.parseInt(String.valueOf(body.get("orderId")));
        String gameName = String.valueOf(body.get("gameName"));
        List<Integer> gameItemIds = ((List<?>) body.getOrDefault("gameItemIds", List.of()))
                .stream().map(v -> Integer.parseInt(String.valueOf(v))).collect(Collectors.toList());

        gameItemService.assignGameItemsToOrder(tableId, orderId, gameName, gameItemIds);
        return Map.of("success", true, "message", "일련번호가 대여 처리되었습니다.");
    }

    /**
     * 현재 테이블의 활성 대여 목록 조회 (RENTED)
     * GET /admin/product/game-items/rentals/active?tableId=1
     */
    @GetMapping("/rentals/active")
    @ResponseBody
    public List<Map<String, Object>> getActiveRentals(@RequestParam int tableId) {
        return gameItemService.getActiveGameRentalsByTable(tableId);
    }

    /**
     * 현재 테이블의 전체 대여 이력 조회
     * GET /admin/product/game-items/rentals/history?tableId=1
     */
    @GetMapping("/rentals/history")
    @ResponseBody
    public List<Map<String, Object>> getRentalHistory(@RequestParam int tableId) {
        return gameItemService.getGameRentalHistoryByTable(tableId);
    }

    /**
     * 결제 전/후 반납 상태 처리 (NORMAL/DAMAGED/LOST) + game_history 업데이트
     * PATCH /admin/product/game-items/rentals/settle
     * body: { tableId, updates:[{historyId, gameItemId, status}] }
     */
    @PatchMapping("/rentals/settle")
    @ResponseBody
    public Map<String, Object> settleRentals(@RequestBody Map<String, Object> body) {
        int tableId = Integer.parseInt(String.valueOf(body.get("tableId")));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updates = (List<Map<String, Object>>) body.getOrDefault("updates", List.of());

        gameItemService.settleGameRentals(tableId, updates);
        return Map.of("success", true, "message", "반납 상태가 반영되었습니다.");
    }
}
