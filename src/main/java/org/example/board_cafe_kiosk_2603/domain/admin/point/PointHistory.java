package org.example.board_cafe_kiosk_2603.domain.admin.point;

import lombok.*;

import java.time.LocalDateTime;


@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PointHistory {
    private int id;             // 포인트 이력 고유 번호 (PK)
    private int pointId;        // 소속 포인트 계좌 ID (FK)
    private Long orderId;        // 관련 주문 ID (FK)
    private String type;         // 이력 유형 ('EARN', 'USE')
    private int amount;          // 변동 포인트
    private int balanceAfter;    // 처리 직후 잔액 스냅샷
    private LocalDateTime createdAt; // 이력 생성 일시
}
