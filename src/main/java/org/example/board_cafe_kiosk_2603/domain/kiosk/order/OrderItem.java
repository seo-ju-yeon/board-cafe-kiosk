package org.example.board_cafe_kiosk_2603.domain.kiosk.order;

import lombok.*;

@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem {
    private int id;
    private int orderId;
    private Integer menuId;
    private String menuName;
    private int price;
    private int quantity;
    private String  status;
}
