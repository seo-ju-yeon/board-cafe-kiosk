package org.example.board_cafe_kiosk_2603.domain.kiosk.cart;

import lombok.*;

@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartItem {
    private int id;
    private int cartId;
    private int menuId;
    private int quantity;
    // cart_item JOIN menu 결과 수신용 (DB 저장 컬럼 아님)
    private String menuName;
    private int menuPrice;
}
