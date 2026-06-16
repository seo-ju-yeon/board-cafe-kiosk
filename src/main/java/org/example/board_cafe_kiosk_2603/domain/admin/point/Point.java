package org.example.board_cafe_kiosk_2603.domain.admin.point;

import lombok.*;

import java.time.LocalDateTime;


@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Point {
    private int id;             // 포인트 계좌 고유 번호 (PK)
    private String phone;        // 고객 전화번호 (유일 식별자)
    private int balance;         // 현재 포인트 잔액
    private LocalDateTime updatedAt;  // 마지막 변경 일시
}
