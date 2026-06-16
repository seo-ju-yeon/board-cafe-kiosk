package org.example.board_cafe_kiosk_2603.mapper.admin.table;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.board_cafe_kiosk_2603.domain.common.cafeTableSession.CafeTableSession;
import org.example.board_cafe_kiosk_2603.domain.admin.table.CafeTable;
import org.example.board_cafe_kiosk_2603.dto.kiosk.order.OrderItemDTO;

import java.util.List;
import java.util.Optional;

/**
 * 대시보드 테이블 현황 전용 DAO 인터페이스
 * select와 update 기능에 집중하여 설계
 */
@Mapper
public interface CafeTableMapper {
    /**
     전체 테이블 현황 조회
     모든 테이블의 번호, 상태, 현재 세션 ID 포인터를 가져옴
     */
    List<CafeTable> selectAllTables();

    /**
     현재 이용 중인(OCCUPIED) 테이블의 ID 리스트만 조회
     */
    List<Integer> selectOccupiedTableIds();

    /**
     신규 세션 생성 (입장 시)
     table_session 테이블에 신규 행을 추가하고, 생성된 PK(id)를 session 객체에 채워줌
     */
    int insertNewSession(CafeTableSession session);

    /**
     테이블 상태 및 세션 포인터 갱신
     */
    int updateTableStatusAndSession(@Param("id") Integer id,
                                    @Param("status") String status,
                                    @Param("sessionId") Long sessionId);

    /**
     세션 종료 처리 (퇴실 시)
     세션의 isActive를 false로 바꾸고 check_out_time을 현재 시각으로 기록
     */
    int closeSession(@Param("sessionId") Long sessionId);

    /**
     특정 테이블의 현재 세션 ID 포인터 조회
     */
    Long selectCurrentSessionId(@Param("tableId") Integer tableId);

    /**
     특정 테이블의 현재 상태 조회 (EMPTY/OCCUPIED/CLEANING)
     */
    String selectStatusById(@Param("tableId") Integer tableId);

    /**
     액세스 토큰(UUID) 개별 갱신
     */
    int updateAccessToken(@Param("tableId") Integer tableId, @Param("accessToken") String accessToken);

    /**
     자정 시스템 초기화
     모든 테이블의 상태를 EMPTY로, 세션 포인터를 NULL로 일괄 초기화
     */
    int resetAllTablesAtMidnight();

    /**
     자정 기준 모든 활성 세션 종료 처리
     아직 퇴실 처리되지 않은(is_active=TRUE) 세션들의 check_out_time을 현재 시각으로 기록
     */
    int updateAllActiveSessions();

    /**
     특정 세션의 실시간 유효 주문 항목 리스트 조회 (PAID, CANCELLED 상태 제외)
     */
    List<OrderItemDTO> selectActiveOrderItems(@Param("sessionId") Long sessionId);

    /**
     테이블의 요청 매세지 읽음 여부 변경
     */
    int updateMessagesReadStatus(@Param("tableId") Integer tableId);

    /**
     특정 테이블의 읽지 않은 메시지들만 가져오기
     */
    List<String> selectUnreadMessageContents(@Param("tableId") Integer tableId);

    /**
     특정 테이블의 현재 액세스 토큰만 조회
     */
    String selectAccessTokenById(@Param("id") Integer id);

    // === 키오스크 로그인(세큐리티 관련 메서드) ===
    // 테이블 번호로 단건 조회 (로그인용), 주연
    Optional<CafeTable> findByTableNumber(int tableNumber);

    // table_session에서 직접 활성 세션 ID 조회
    Long selectActiveSessionByTableId(@Param("tableId") int tableId);

    // table_session에서 최신 세션 ID 조회 (활성/비활성 포함)
    Long selectLatestSessionByTableId(@Param("tableId") int tableId);

    /**
     * 특정 세션 ID에 해당하는 모든 메시지를 읽음 처리
     */
    int updateMessagesReadStatusBySessionId(@Param("sessionId") Long sessionId);

    /**
     * [Batch Update] 모든 미확인 메시지 일괄 읽음 처리
     */
    int updateAllMessagesReadStatus();
}
