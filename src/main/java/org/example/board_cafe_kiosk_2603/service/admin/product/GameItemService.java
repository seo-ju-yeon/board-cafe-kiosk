package org.example.board_cafe_kiosk_2603.service.admin.product;

import org.example.board_cafe_kiosk_2603.domain.admin.product.GameItemStatus;
import org.example.board_cafe_kiosk_2603.dto.admin.product.GameItemRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.product.GameItemResponseDTO;

import java.util.List;
import java.util.Map;

/**
 * GameItem 비즈니스 로직 인터페이스
 */
public interface GameItemService {
    /** 전체 게임 아이템 목록 반환 */
    List<GameItemResponseDTO> getAll();

    /** game_id 기준 게임 아이템 목록 반환 */
    List<GameItemResponseDTO> getByGameId(int gameId);

    /** status 기준 게임 아이템 목록 반환 */
    List<GameItemResponseDTO> getByStatus(GameItemStatus gameItemStatus);

    /** PK로 게임 아이템 단건 반환 */
    GameItemResponseDTO getById(int id);

    /** 게임 아이템 등록 */
    void register(GameItemRequestDTO gameItemRequestDTO);

    /** 게임 아이템 수정 */
    void modify(int id, GameItemRequestDTO gameItemRequestDTO);

    /** 게임 아이템 삭제 */
    void remove(int id);

    /** 게임 아이템 상태 변경 */
    void changeStatus(int id, GameItemStatus gameItemStatus);

    /** 게임명 기준 대여 가능 시리얼 조회 (NORMAL) */
    List<GameItemResponseDTO> getAvailableByGameName(String gameName);

    /** 주문에 대해 선택한 시리얼을 대여 처리하고 game_history에 기록 */
    void assignGameItemsToOrder(int tableId, int orderId, String gameName, List<Integer> gameItemIds);

    /** 현재 테이블의 활성 게임 대여 목록 조회 */
    List<Map<String, Object>> getActiveGameRentalsByTable(int tableId);

    /** 현재 테이블의 전체 게임 대여 이력 조회 */
    List<Map<String, Object>> getGameRentalHistoryByTable(int tableId);

    /** 결제 전/후 반납 상태 처리 (NORMAL/DAMAGED/LOST) */
    void settleGameRentals(int tableId, List<Map<String, Object>> updates);
}
