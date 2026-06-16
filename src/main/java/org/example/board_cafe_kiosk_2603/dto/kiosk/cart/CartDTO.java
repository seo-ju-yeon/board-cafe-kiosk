package org.example.board_cafe_kiosk_2603.dto.kiosk.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartDTO {

    private boolean           success;
    private String            message;
    private List<CartItemDTO> cartItems;
    private int               totalPrice;
    private int               cartCount;
}

