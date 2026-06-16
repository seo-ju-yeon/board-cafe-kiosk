package org.example.board_cafe_kiosk_2603.service.admin.cafeTable;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.table.CafeTable;
import org.example.board_cafe_kiosk_2603.domain.common.cafeTableSession.CafeTableSession;
import org.example.board_cafe_kiosk_2603.domain.kiosk.payment.Payment;
import org.example.board_cafe_kiosk_2603.dto.admin.table.CafeTableDTO;
import org.example.board_cafe_kiosk_2603.dto.kiosk.order.OrderItemDTO;
import org.example.board_cafe_kiosk_2603.mapper.admin.product.GameItemMapper;
import org.example.board_cafe_kiosk_2603.mapper.admin.table.CafeTableMapper;
import org.example.board_cafe_kiosk_2603.mapper.kiosk.payment.PaymentMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class CafeTableServiceImpl implements CafeTableService {
    private final CafeTableMapper cafeTableMapper;
    private final GameItemMapper gameItemMapper;
    private final PaymentMapper paymentMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true) // 상세 설명: 단순 조회 메서드이므로 성능 최적화를 위해 readOnly 적용
    public List<CafeTableDTO> getAllTableStatus() {
        /* 주 설명: DB 도메인(VO)을 DTO 리스트로 변환하여 컨트롤러로 반환 */
        List<CafeTable> cafeTableList = cafeTableMapper.selectAllTables();

        // 상세 설명: Stream API를 활용하여 DTO 변환 코드를 간결화
        return cafeTableList.stream().map(cafeTable -> CafeTableDTO.builder()
                .id(cafeTable.getId())
                .tableNumber(cafeTable.getTableNumber())
                .status(cafeTable.getStatus())
                .accessToken(cafeTable.getAccessToken())
                .checkInTime(cafeTable.getCheckInTime())
                .guestCount(cafeTable.getGuestCount())
                .hasUnreadMessage(cafeTable.isHasUnreadMessage())
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    public void changeTableStatus(Integer id, String status) {
        /**
         * [핵심] 테이블 상태 전이에 따른 세션(방문 이력) 생명주기 관리
         * 상세 설명: 단순 텍스트 변경이 아닌, OCCUPIED 시 세션을 생성(매핑)하고 CLEANING 시 세션을 마감(해제)함
         */
        log.info("테이블 ID: {} 상태 변경 프로세스 시작 -> {}", id, status);

        // 토큰 체크 로직: 입실(OCCUPIED) 시도 시 토큰이 없으면 예외 던짐
        if ("OCCUPIED".equals(status)) {
            String currentToken = cafeTableMapper.selectAccessTokenById(id);

            if (currentToken == null || currentToken.trim().isEmpty()) {
                log.error("실패: 테이블 ID {}번에 토큰이 없어 OCCUPIED 전환이 불가능합니다.", id);
                throw new IllegalStateException("인증 토큰이 없는 테이블은 입실 처리가 불가능합니다.");
            }
        }

        switch (status) {
            case "OCCUPIED":
                /* 주 설명: [입실] 신규 방문 세션 생성 및 테이블 포인터 연결 */
                CafeTableSession newSession = CafeTableSession.builder()
                        .tableId(id)
                        .packageId(1) // 임시: 향후 프론트에서 전달받은 패키지 ID 적용
                        .initialGuestCnt(1) // 임시: 향후 프론트에서 전달받은 인원 수 적용
                        .build();

                cafeTableMapper.insertNewSession(newSession);
                Long newSessionId = newSession.getId(); // DB에서 생성된 PK 획득

                //
                cafeTableMapper.updateTableStatusAndSession(id, "OCCUPIED", newSessionId);
                log.info("성공: 세션 {}번 생성 및 테이블 매핑 완료", newSessionId);
                break;

            case "CLEANING":
                /* 주 설명: [퇴실/결제] 현재 이용 중인 세션 마감 및 테이블 포인터 해제 */
                Long currentSessionId = cafeTableMapper.selectCurrentSessionId(id);
                if (currentSessionId == null) {
                    currentSessionId = cafeTableMapper.selectActiveSessionByTableId(id);
                }

                if (currentSessionId != null) {
                    ensurePaymentCompletedBeforeCleaning(id, currentSessionId);
                }

                if (currentSessionId != null) {
                    // 퇴실하는 세션의 모든 메시지 읽음 처리
                    cafeTableMapper.updateMessagesReadStatusBySessionId(currentSessionId);
                    log.info("성공: 세션 {}번 종료로 인한 미확인 메시지 자동 읽음 처리", currentSessionId);

                    cafeTableMapper.closeSession(currentSessionId);
                    log.info("성공: 세션 {}번 이용 이력 마감 완료", currentSessionId);
                }

                // 매핑 해제 시 sessionId에 null을 명시적으로 전달
                cafeTableMapper.updateTableStatusAndSession(id, "CLEANING", null);
                log.info("성공: 테이블 매핑 해제 및 CLEANING 상태 전환 완료");
                break;

            case "EMPTY":
                Long latestSessionId = cafeTableMapper.selectCurrentSessionId(id);
                if (latestSessionId == null) {
                    latestSessionId = cafeTableMapper.selectLatestSessionByTableId(id);
                }
                if (latestSessionId != null) {
                    int historyUpdated = gameItemMapper.returnActiveRentalsBySessionId(latestSessionId);
                    int itemUpdated = gameItemMapper.normalizeNormalItemsBySessionId(latestSessionId);
                    log.info("청소완료(EMPTY) 전환 시 게임 대여 자동 복구 | tableId: {}, sessionId: {}, historyUpdated: {}, itemUpdated: {}",
                            id, latestSessionId, historyUpdated, itemUpdated);
                }

                // EMPTY 전환 시에도 혹시 남아있을지 모를 테이블 기준 알림 청소
                cafeTableMapper.updateMessagesReadStatus(id);
                /* 주 설명: [청소 완료] 다음 손님 대기 상태로 전환 */
                cafeTableMapper.updateTableStatusAndSession(id, "EMPTY", null);
                log.info("성공: 테이블 EMPTY 상태 전환 완료");
                break;

            default:
                log.warn("알 수 없는 상태값 요청: {}", status);
        }
    }

    private void ensurePaymentCompletedBeforeCleaning(Integer tableId, Long sessionId) {
        Payment payment = paymentMapper.findBySessionId(sessionId);
        if (payment == null || !"DONE".equals(payment.getStatus())) {
            log.warn("결제 미완료 상태에서 CLEANING 전환 차단 | tableId: {}, sessionId: {}, paymentStatus: {}",
                    tableId, sessionId, payment == null ? "NOT_FOUND" : payment.getStatus());
            throw new IllegalStateException("결제가 완료되지 않아 청소중으로 변경할 수 없습니다.");
        }
    }

    @Override
    public String generateNewToken(Integer id) {
        /* 주 설명: UUID를 생성하여 특정 테이블의 access_token 단독 갱신 */
        String newToken = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        // 상세 설명: 주석 내용(8자리 짧은 토큰)과 일치하도록 substring(0, 8)로 유지함

        // 토큰 새로 뽑을 때 해당 테이블 알림 전부 읽음 처리
        cafeTableMapper.updateMessagesReadStatus(id);

        cafeTableMapper.updateAccessToken(id, newToken);
        log.info("테이블 ID: {} 새 토큰 발급 완료: {}", id, newToken);

        return newToken;
    }

    @Override
    public void resetAllTablesForNewDay() {
        /** * [핵심] 자정 전체 초기화 로직 (트랜잭션 보장)
         * 1. table_session의 모든 활성 세션을 일괄 마감 (is_active = FALSE)
         * 2. cafe_table의 모든 상태를 'EMPTY'로 변경하고 세션 포인터(current_session_id)를 NULL로 해제
         */
        log.info("--- 자정 데이터 리셋 프로세스 시작 ---");

        // 1. 활성 세션 일괄 종료
        int closedSessions = cafeTableMapper.updateAllActiveSessions();
        log.info("자정 데이터 리셋 - 활성 세션 {}개 강제 종료 완료", closedSessions);

        // 2. 모든 미확인 메시지 일괄 읽음 처리
        int updatedMessages = cafeTableMapper.updateAllMessagesReadStatus();
        log.info("미확인 메시지 {}건 읽음 처리 완료", updatedMessages);

        // 3. 전체 테이블 공석 처리 및 연결 해제
        // 상세 설명: for문으로 건건이 쿼리하는 대신, Mapper에 등록된 일괄 업데이트 쿼리를 호출하여 통신 횟수 및 부하 최소화
        int resetTables = cafeTableMapper.resetAllTablesAtMidnight();
        log.info("자정 데이터 리셋 - 전체 테이블 공석(EMPTY) 및 매핑 해제 완료 (적용 건수: {})", resetTables);

        log.info("--- 자정 데이터 리셋 프로세스 종료 ---");
    }

    /**
     * [실시간 주문 상세 내역 조회]
     * 대시보드 모달창에 표시할 특정 테이블의 현재 주문 항목 리스트를 가져옵니다.
     * * @param tableId 조회 대상 테이블의 고유 식별 번호 (PK)
     * @return OrderItemDTO 리스트 (진행 중인 주문이 없거나 빈 테이블일 경우 빈 리스트 반환)
     */
    @Override
    @Transactional(readOnly = true) // 데이터 정합성을 유지하면서도 성능 최적화를 위해 읽기 전용 트랜잭션 적용
    public List<OrderItemDTO> getActiveOrders(Integer tableId) {

        log.info("주문 내역 조회 요청 - 테이블 ID: {}", tableId);

        // 1. [세션 확인] 해당 물리 테이블이 현재 가리키고 있는 활성 방문 세션(current_session_id)을 조회합니다.
        // 상세 설명: 테이블 상태가 'OCCUPIED'인 경우에만 유효한 ID가 존재하며, 'EMPTY'나 'CLEANING'일 경우 NULL이 반환됩니다.
        Long sessionId = cafeTableMapper.selectCurrentSessionId(tableId);
        if (sessionId == null) {
            // current_session_id가 비정상일 때를 대비해 table_session에서 활성 세션을 한번 더 조회
            sessionId = cafeTableMapper.selectActiveSessionByTableId(tableId);
        }

        // 2. [방어 로드] 세션 ID가 존재하지 않는 경우 (손님이 없는 테이블 등)
        // 상세 설명: 불필요하게 orders 테이블을 Join 하지 않도록 즉시 빈 리스트(ArrayList)를 생성하여 반환합니다.
        if (sessionId == null) {
            log.debug("테이블 {}번: 연결된 활성 세션이 없어 주문 조회를 중단합니다.", tableId);
            return new ArrayList<>();
        }

        // 3. [데이터 추출] MyBatis Mapper를 통해 실제 DB에서 주문 항목들을 가져옵니다.
        /**
         * SQL 필터링 기준 (Mapper.xml 내부 로직):
         * - session_id가 일치해야 함
         * - 주문 상태(status)가 'PAID'(결제완료) 또는 'CANCELLED'(주소)가 아닌 것만 포함
         * - 최신 주문이 위로 오도록 ordered_at 기준 정렬
         */
        List<OrderItemDTO> activeItems = cafeTableMapper.selectActiveOrderItems(sessionId);

        log.info("조회 완료 - 테이블 {}번(세션 {}), 진행 중인 주문 항목: {}건",
                tableId, sessionId, activeItems.size());

        return activeItems;
    }

    /**
     * 읽지 않은 메시지 내용들만 추출
     */
    @Override
    public List<String> getUnreadMessages(Integer tableId) {
        log.info("손님 요청 메시지 조회 - 테이블 ID: {}", tableId);
        return cafeTableMapper.selectUnreadMessageContents(tableId);
    }

    /**
     * 알림 상태 업데이트 (is_read = true)
     */
    @Override
    @Transactional
    public void markMessagesAsRead(Integer tableId) {
        log.info("손님 요청 읽음 처리 - 테이블 ID: {}", tableId);
        cafeTableMapper.updateMessagesReadStatus(tableId);
    }

    // 키오스크 로그인: 테이블 번호 + 비밀번호 검증 후 테이블 반환
    @Override
    public Optional<CafeTable> login(int tableNumber, String password) {
        CafeTable cafeTable = cafeTableMapper.findByTableNumber(tableNumber)
                .orElse(null);

        if (cafeTable == null) {
            log.warn("존재하지 않는 테이블 번호: {}", tableNumber);
            return Optional.empty();
        }

        // 현재 비밀번호가 평문이므로 equals 비교
        // 추후 BCrypt 적용 시 -> if문을 아래 코드로 교체
        // !passwordEncoder.matches(password, cafeTable.getPassword())
//        if (!cafeTable.getPassword().equals(password)) {
        if (!passwordEncoder.matches(password, cafeTable.getPassword())) {
//            log.warn("테이블 {} 비밀번호 불일치", tableNumber);
            log.warn("--- [CafeTableService] 비밀번호 불일치 | tableNumber: {} ---", tableNumber);
            return Optional.empty();
        }

        return Optional.of(cafeTable);
    }

    @Override
    public void updateAccessToken(int tableId, String accessToken) {
        log.info("--- [CafeTableService] access_token 업데이트 | tableId: {} ---", tableId);
        cafeTableMapper.updateAccessToken(tableId, accessToken);
    }

    @Override
    @Transactional(readOnly = true)
    public String getTableStatus(int tableId) {
        return cafeTableMapper.selectStatusById(tableId);
    }

    @Override
    public Long findCurrentSessionId(int tableId) {
        log.info("--- [CafeTableService] current_session_id 조회 | tableId: {} ---", tableId);
        Long sessionId = cafeTableMapper.selectCurrentSessionId(tableId);
        log.info("--- [CafeTableService] 조회 결과 | sessionId: {} ---", sessionId);
        return sessionId;
    }

    @Override
    @Transactional(readOnly = true)
    public Long findActiveSessionByTableId(int tableId) {
        log.info("--- [CafeTableService] table_session 활성 세션 조회 | tableId: {} ---", tableId);
        return cafeTableMapper.selectActiveSessionByTableId(tableId);
    }

    @Override
    public void syncTableWithSession(int tableId, Long sessionId) {
        log.info("--- [CafeTableService] cafe_table 동기화 | tableId: {}, sessionId: {} ---",
                tableId, sessionId);
        // tableId를 그냥 넘기면 됨 - Mapper @Param("id")가 받음.
        cafeTableMapper.updateTableStatusAndSession(tableId, "OCCUPIED", sessionId);
    }

    // access_token 조회 메서드
    @Override
    @Transactional(readOnly = true)
    public String getTableAccessToken(int tableId) {
        return cafeTableMapper.selectAccessTokenById(tableId);
        // selectAccessTokenById는 changeTableStatus()에서 이미 사용 중 → Mapper에 존재함
    }


}
