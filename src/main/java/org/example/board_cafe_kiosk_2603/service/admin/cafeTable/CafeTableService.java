package org.example.board_cafe_kiosk_2603.service.admin.cafeTable;

import org.example.board_cafe_kiosk_2603.domain.admin.table.CafeTable;
import org.example.board_cafe_kiosk_2603.dto.admin.table.CafeTableDTO;
import org.example.board_cafe_kiosk_2603.dto.kiosk.order.OrderItemDTO;

import java.util.List;
import java.util.Optional;

public interface CafeTableService {
    /* 전체 테이블 목록 및 현재 세션 정보 조회 */
    List<CafeTableDTO> getAllTableStatus();

    /* 테이블 상태 변경 (입실/퇴실/청소) 및 세션 동기화 */
    void changeTableStatus(Integer id, String status);

    /* 새 액세스 토큰 발급 및 할당 */
    String generateNewToken(Integer id);

    /* 자정 전체 초기화 실행 (세션 마감 및 테이블 공석 처리) */
    void resetAllTablesForNewDay();

    /* 실시간 주문 상세 내역 조회(대시보드 모달창에 표시할 특정 테이블의 현재 주문 항목 리스트 출력) */
    List<OrderItemDTO> getActiveOrders(Integer tableId);

    /* 읽지 않은 테이블 요청 목록 */
    List<String> getUnreadMessages(Integer tableId);

    /* 알림 확인 처리 */
    void markMessagesAsRead(Integer tableId);

    // 키오스크 로그인
    // 키오스크 로그인: 테이블 번호 + 비밀번호 검증 후 테이블 반환
    Optional<CafeTable> login(int tableNumber, String password);

    void updateAccessToken(int tableId, String accessToken);
    String getTableStatus(int tableId);
    Long findCurrentSessionId(int tableId);

    // table_session 직접 조회
    Long findActiveSessionByTableId(int tableId);
    // 불일치 복구
    void syncTableWithSession(int tableId, Long sessionId);

    // access_token 조회 메서드
    String getTableAccessToken(int tableId);

}
