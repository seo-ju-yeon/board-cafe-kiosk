package org.example.board_cafe_kiosk_2603.domain.common.cafeTableSession;

import lombok.*;

import java.time.LocalDateTime;

/**
 * table_session 테이블의 도메인 클래스.
 * 테이블 이용 세션 및 방문 히스토리를 나타냅니다.
 * 입장~퇴장 한 번의 방문을 나타내며 orders, payment, rental_log 의 기준이 됩니다.
 */

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CafeTableSession {
    private Long id;                // 세션 고유 번호 (PK, BIGINT)
    private Integer tableId;        // 이용 중인 테이블 번호 (FK)
    private Integer packageId;      // 선택한 요금제 패키지 ID (FK)
    private Integer initialGuestCnt;// 최초 입장 인원
    private LocalDateTime checkInTime;  // 입장 시간 (DEFAULT CURRENT_TIMESTAMP)
    private LocalDateTime checkOutTime; // 퇴장 시간 (NULL 가능)
    private Boolean isActive;       // 현재 세션 활성화 여부 (T/F)
    private Integer totalAmount;    // 최종 정산 금액 (퇴실 시 합산)
}
