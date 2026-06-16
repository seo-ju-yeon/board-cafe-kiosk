package org.example.board_cafe_kiosk_2603.dto.admin.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameResponseDTO {
    private int id;
    private Integer categoryId;

    /** JOIN으로 가져온 카테고리명 */
    private String categoryName;

    private String name;
    private Integer minPlayers;
    private Integer maxPlayers;

    /* 평균 플레이 시간 (분) */
    private Integer playTime;

    /** 게임 설명 (menu.description JOIN) */
    private String description;

    private boolean isActive;

    private String imageUrl;

    /** game_item 테이블 COUNT로 가져온 보유 재고 수 */
    private int gameItemCount;
}
