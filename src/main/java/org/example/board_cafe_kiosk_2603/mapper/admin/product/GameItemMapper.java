package org.example.board_cafe_kiosk_2603.mapper.admin.product;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.board_cafe_kiosk_2603.domain.admin.product.GameItem;
import org.example.board_cafe_kiosk_2603.domain.admin.product.GameItemStatus;
import org.example.board_cafe_kiosk_2603.dto.admin.product.GameItemResponseDTO;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * game_item 테이블 CRUD MyBatis Mapper 인터페이스
 */
@Mapper
public interface GameItemMapper {

    /** 전체 게임 아이템 목록 조회 (game JOIN 포함) */
    List<GameItemResponseDTO> findAll();

    /** game_id 기준 게임 아이템 목록 조회 */
    List<GameItemResponseDTO> findByGameId(int gameId);

    /** status 기준 게임 아이템 목록 조회 */
    List<GameItemResponseDTO> findByStatus(@Param("status") GameItemStatus gameItemStatus);

    /** PK로 게임 아이템 단건 조회 (game JOIN 포함) */
    Optional<GameItemResponseDTO> findById(int id);

    /** 게임 아이템 등록 */
    int insert(GameItem gameItem);

    /** 게임 아이템 수정 (시리얼 번호·상태 변경) */
    int update(GameItem gameItem);

    /** 게임 아이템 삭제 */
    int delete(int id);

    /** 게임 아이템 상태 변경 */
    int updateStatus(@Param("id") int id, @Param("status") GameItemStatus gameItemStatus);

    /** 주문-세션-게임명 매칭 검증 */
    int countOrderItemInSession(@Param("orderId") int orderId,
                                @Param("sessionId") long sessionId,
                                @Param("menuName") String menuName);

    /** 게임 주문(order)과 매칭되는 RENTED 이력 수량 조회 */
    int countMatchedRentedGameHistoriesForOrder(@Param("orderId") int orderId,
                                                @Param("sessionId") long sessionId);

    /** 게임 대여 이력 생성 */
    int insertGameHistory(@Param("sessionId") long sessionId,
                          @Param("gameItemId") int gameItemId);

    /** 테이블의 활성 게임 대여 목록 */
    List<Map<String, Object>> findActiveGameRentalsBySessionId(@Param("sessionId") long sessionId);

    /** 테이블의 전체 게임 대여 이력 */
    List<Map<String, Object>> findGameRentalHistoryBySessionId(@Param("sessionId") long sessionId);

    /** game_history 단건 조회 */
    Map<String, Object> findGameHistoryById(@Param("historyId") long historyId);

    /** game_history 반납 상태 업데이트 */
    int updateGameHistoryStatus(@Param("historyId") long historyId,
                                @Param("status") String status);

    /** 세션의 RENTED 이력을 일괄 NORMAL 처리 */
    int returnActiveRentalsBySessionId(@Param("sessionId") long sessionId);

    /** 세션의 RENTED->NORMAL 반영 대상 game_item을 NORMAL로 복구 */
    int normalizeNormalItemsBySessionId(@Param("sessionId") long sessionId);
}
