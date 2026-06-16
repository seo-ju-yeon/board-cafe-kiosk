package org.example.board_cafe_kiosk_2603.mapper.common.cafeTableSession;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.board_cafe_kiosk_2603.domain.common.cafeTableSession.CafeTableSession;

/**
 * table_session 테이블 전용 MyBatis Mapper.
 *
 * ※ 통합 경위
 *   - TableSessionKioskMapper  : 키오스크 세션 생성(insertSession)만 담당 → insert()로 통합
 *   - TableSessionAdminMapper  : 어드민 전용 조회/종료/금액 업데이트 담당 → 동일 메서드로 통합
 *   - TableSessionMapper(구)   : 위 두 Mapper를 하나로 묶으려다 resultType 오류 등으로 실패한 버전
 *   세 파일의 기능을 모두 이 인터페이스 하나로 합쳤습니다.
 *
 * ※ 패키지 변경
 *   기존 mapper.admin.table / mapper.kiosk 두 곳에 분산되어 있던 것을
 *   mapper.table 로 일원화하였습니다.
 *   → XML namespace, Spring 컴포넌트 스캔 경로도 함께 수정하세요.
 */
@Mapper
public interface CafeTableSessionMapper {
    // =========================================================
    // [공통] 세션 조회
    // =========================================================

    /**
     * 테이블 ID로 현재 활성(is_active=TRUE) 세션을 조회합니다.
     * 체크인 시간 기준 최신 1건을 반환합니다.
     *
     * @param tableId 테이블 번호
     * @return 활성 세션, 없으면 null
     */
    CafeTableSession findActiveByTableId(int tableId);

    /**
     * 세션 PK로 단건 조회합니다.
     *
     * @param id 세션 고유 번호
     * @return CafeTableSession, 없으면 null
     */
    CafeTableSession findById(long id);

    // =========================================================
    // [키오스크] 체크인
    // =========================================================

    /**
     * 새 세션을 생성합니다 (체크인 / 패키지 선택 완료 시 호출).
     * insert 후 자동 생성된 PK 가 session.id 에 채워집니다 (useGeneratedKeys).
     *
     * @param session tableId, packageId, initialGuestCnt 를 세팅한 객체
     */
    void insert(CafeTableSession session);

    // =========================================================
    // [어드민 / 키오스크 공통] 체크아웃 & 금액 업데이트
    // =========================================================

    /**
     * 체크아웃 처리합니다.
     * is_active=FALSE, check_out_time=현재 시각, total_amount 확정을 한 번에 수행합니다.
     *
     * @param session id, totalAmount 를 세팅한 객체
     */
    void checkOut(CafeTableSession session);

    /**
     * 주문 추가 등으로 total_amount 만 갱신합니다.
     * 체크아웃 전 중간 금액 업데이트에 사용합니다.
     *
     * @param id          세션 PK
     * @param totalAmount 새 합계 금액
     */
    void updateTotalAmount(@Param("id") long id, @Param("totalAmount") int totalAmount);
}
