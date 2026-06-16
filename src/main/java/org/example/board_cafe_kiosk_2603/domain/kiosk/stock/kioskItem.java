package org.example.board_cafe_kiosk_2603.domain.kiosk.stock;

import lombok.*;

@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class kioskItem {
    private String name;
    private int price;       // 게임은 0
    private String imageUrl;
    private int stock;       // 메뉴는 -1(무제한), 게임은 gameItemCount
}
