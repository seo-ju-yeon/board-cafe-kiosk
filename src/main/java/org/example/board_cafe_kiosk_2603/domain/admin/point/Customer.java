package org.example.board_cafe_kiosk_2603.domain.admin.point;

import lombok.*;

import java.sql.Timestamp;

@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Customer {
    private int id;             // 고객 고유 번호 (PK, AI)
    private String phone;        // 전화번호 (유일 식별자)
    private Boolean isActive;    // 활성 상태 (FALSE=비활성)
    private Timestamp createdAt; // 등록 일시
}
