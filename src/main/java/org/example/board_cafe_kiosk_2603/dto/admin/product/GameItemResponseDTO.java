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
public class GameItemResponseDTO {
    /*
    게임 아이템(재고) 조회 응답 시 사용하는 DTO
    -> gameName 을 JOIN 하여 함께 반환
     */

    private int id;

    /* FK → game.id */
    private int gameId;

    /* JOIN으로 가져온 게임명 */
    private String gameName;

    /* 실물 시리얼 번호 */
    private String serialNumber;

    /* NORMAL / RENTED / DAMAGED / LOST => Enum */
    private GameItemStatus status;
}
