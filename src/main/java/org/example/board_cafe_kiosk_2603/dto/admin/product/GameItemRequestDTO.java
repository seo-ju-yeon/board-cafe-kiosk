package org.example.board_cafe_kiosk_2603.dto.admin.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.board_cafe_kiosk_2603.domain.admin.product.GameItemStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameItemRequestDTO {
    /* 게임 아이템(재고) 등록·수정 요청 시 사용하는 DTO */

    /* FK → game.id */
    private int gameId;

    /* 실물 시리얼 번호 */
    private String serialNumber;

    /* NORMAL / RENTED / DAMAGED / LOST => Enum */
    private GameItemStatus status;

    // GameItemRequestDTO.java에 추가
    // product_game html 매칭용 추가
    private int id;  // 기존 아이템 수정 시 식별용
}
