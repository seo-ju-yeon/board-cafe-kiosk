package org.example.board_cafe_kiosk_2603.domain.kiosk.order;

/**
 * 주문 상태 전이 규칙
 * 
 * ┌─────────────────────────────────────────────────────┐
 * │  주문 상태 흐름 (Order Status)                        │
 * ├─────────────────────────────────────────────────────┤
 * │ ORDERED → CONFIRMED → COOKING → DELIVERING → COMPLETED
 * │    ↓         ↓          ↓          ↓                 │
 * │ CANCELLED (모든 상태에서 취소 가능, COMPLETED 제외)   │
 * │                                                      │
 * │ 결제 상태는 별도로 payment 테이블에서 관리             │
 * │ (READY, DONE)                                        │
 * └─────────────────────────────────────────────────────┘
 */
public enum OrderStatus {

    ORDERED,    // 주문완료 (고객이 주문, 결제 전)
    CONFIRMED,  // 주문확인 (관리자가 확인)
    COOKING,    // 조리중
    DELIVERING, // 배달/서빙 대기 (조리완료 → 서빙 단계)
    COMPLETED,  // 서빙완료 (최종 상태)
    CANCELLED;  // 취소됨 (최종 상태)

    /**
     * 현재 상태에서 목표 상태로의 전이가 허용되는지 검증합니다.
     * 
     * 전이 규칙:
     * - ORDERED: CONFIRMED, CANCELLED 가능
     * - CONFIRMED: COOKING, CANCELLED 가능
     * - COOKING: DELIVERING, CANCELLED 가능
     * - DELIVERING: COMPLETED, CANCELLED 가능
     * - COMPLETED: 변경 불가 (최종 상태)
     * - CANCELLED: 변경 불가 (최종 상태)
     *
     * @param next 전이하려는 목표 상태
     * @throws IllegalStateException 허용되지 않는 상태 전이일 때
     */
    public void validateTransitionTo(OrderStatus next) {
        boolean allowed = switch (this) {
            case ORDERED    -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED  -> next == COOKING   || next == CANCELLED;
            case COOKING    -> next == DELIVERING || next == CANCELLED;
            case DELIVERING -> next == COMPLETED || next == CANCELLED;
            case COMPLETED  -> false; // 최종 상태 — 변경 불가
            case CANCELLED  -> false; // 최종 상태 — 변경 불가
        };

        if (!allowed) {
            throw new IllegalStateException(
                    String.format("허용되지 않는 상태 전이입니다: %s → %s", this.name(), next.name()));
        }
    }
}
