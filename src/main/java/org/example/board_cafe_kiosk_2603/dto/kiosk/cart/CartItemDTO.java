package org.example.board_cafe_kiosk_2603.dto.kiosk.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {

    private int    id;
    private int    cartId;
    private int    menuId;
    private int    quantity;
    private String menuName;   // cart_item JOIN menu 결과 (DB 저장 컬럼 아님)
    private int    menuPrice;  // cart_item JOIN menu 결과 (DB 저장 컬럼 아님)
}
