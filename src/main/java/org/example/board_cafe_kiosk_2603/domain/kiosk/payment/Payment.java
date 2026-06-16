package org.example.board_cafe_kiosk_2603.domain.kiosk.payment;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Payment {
    private int           id;
    private long          sessionId;    // table_session.id FK (세션당 최종 1회 결제)
    private Integer       tableNumber;  // 테이블 번호 (조회 편의용)
    private String        status;       // READY | DONE
    private int           finalAmount;  // 최종 결제 금액
    private String        paymentKey;   // 토스 결제 키 (중복 결제 방지)
    private String        orderIdToss;  // 토스용 주문번호
    private String        method;       // 결제 수단 (카드, 간편결제, 계좌이체 등)
    private String        rawResponse;  // 토스 API 응답 원문
    private LocalDateTime approvedAt;   // 토스 승인 시각
    private LocalDateTime paidAt;       // 결제 완료 시각
}
