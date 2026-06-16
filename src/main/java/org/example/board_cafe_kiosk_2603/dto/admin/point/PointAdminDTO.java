package org.example.board_cafe_kiosk_2603.dto.admin.point;

import lombok.*;
import org.example.board_cafe_kiosk_2603.domain.admin.point.Point;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointAdminDTO {
    private int id;
    private String phone;
    private int balance;
    private LocalDateTime updatedAt;

    // === 정적 팩토리 ===
    public static PointAdminDTO from(Point point) {
        return PointAdminDTO.builder()
                .id(point.getId())
                .phone(point.getPhone())
                .balance(point.getBalance())
                .updatedAt(point.getUpdatedAt())
                .build();
    }
}
