package org.example.board_cafe_kiosk_2603.domain.admin.product;

import lombok.*;

@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameItem {
    // 보드게임의 실물 재고 단위를 관리하며, game 테이블을 FK로 참조

    private int id;
    private int gameId;
    private String serialNumber;  // 실물 시리얼 번호 (UNIQUE)
    private GameItemStatus status;  // Enum 타입 사용 (NORMAL, RENTED, DAMAGED, LOST)

}
