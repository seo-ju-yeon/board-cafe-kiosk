package org.example.board_cafe_kiosk_2603.dto.admin.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameRequestDTO {

    // GameController에서 '게임 등록(register)' 후 생성된 PK 수신용
    private Integer id;

    /* FK → category.id */
    private Integer categoryId;

    private String name;
    private Integer minPlayers;
    private Integer maxPlayers;

    /* 평균 플레이 시간 (분) */
    private Integer playTime;

    /** 게임 설명 (menu.description과 연동 저장) */
    private String description;

    /* 활성 여부 */
    private boolean isActive;

    private String imageUrl;

    // product_game html 매칭용 추가
    private List<GameItemRequestDTO> items;      // 기존 game_item 수정용
    private List<GameItemRequestDTO> newItems;   // 신규 game_item 등록용
}
