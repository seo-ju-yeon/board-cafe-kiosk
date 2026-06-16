package org.example.board_cafe_kiosk_2603.dto.kiosk.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 주문 생성 요청 DTO
 *
 * cart.html의 confirmOrder() 함수에서 사용:
 *
 * fetch('/kiosk/order/create', {
 *   method: 'POST',
 *   headers: { 'Content-Type': 'application/json' },
 *   body: JSON.stringify({
 *     tableNumber: 5,
 *     totalAmount: 15000,
 *     customerPhone: "010-1234-5678" (optional)
 *   })
 * })
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {
    private int tableNumber;
    private int totalAmount;
    private String customerPhone;  // optional
}