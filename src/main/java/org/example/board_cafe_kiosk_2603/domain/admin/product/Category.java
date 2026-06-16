package org.example.board_cafe_kiosk_2603.domain.admin.product;

import lombok.*;

@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    private int id;
    private String name;
    private CategoryType type;  // Enum 타입 사용 (DRINK, FOOD, GAME, GUEST)
}
