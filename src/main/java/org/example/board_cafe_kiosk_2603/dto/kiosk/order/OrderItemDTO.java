package org.example.board_cafe_kiosk_2603.dto.kiosk.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {

    private int     id;
    private Integer orderId;
    private Integer menuId;
    private String  menuName;
    private int     price;
    private int     quantity;
    private String  status;
}

