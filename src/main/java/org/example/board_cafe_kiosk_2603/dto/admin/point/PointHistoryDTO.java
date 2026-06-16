package org.example.board_cafe_kiosk_2603.dto.admin.point;

import lombok.*;
import org.example.board_cafe_kiosk_2603.domain.admin.point.PointHistory;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointHistoryDTO {
    private long id;
    private int pointId;
    private Long orderId;
    private String type;         // EARN | USE
    private int amount;
    private int balanceAfter;
    private LocalDateTime createdAt;

    // === 정적 팩토리 ===
    public static PointHistoryDTO from(PointHistory history) {
        return PointHistoryDTO.builder()
                .id(history.getId())
                .pointId(history.getPointId())
                .orderId(history.getOrderId())
                .type(history.getType())
                .amount(history.getAmount())
                .balanceAfter(history.getBalanceAfter())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
