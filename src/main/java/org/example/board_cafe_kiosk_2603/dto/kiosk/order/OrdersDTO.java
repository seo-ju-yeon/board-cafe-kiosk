package org.example.board_cafe_kiosk_2603.dto.kiosk.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdersDTO {

    private boolean            success;
    private String             message;
    private int                id;
    private int                tableId;
    private String             customerPhone;
    private String             status;
    private int                totalAmount;
    private LocalDateTime      orderedAt;
    private List<OrderItemDTO> items;
}

