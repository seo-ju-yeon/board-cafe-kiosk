package org.example.board_cafe_kiosk_2603.domain.admin.statistics;

import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ItemSalesHistory {
    private LocalDate statDate;    // DB: stat_date
    private Integer productId;     // DB: product_id
    private String category;       // DB: category (Enum 문자열)
    private Integer salesQty;      // DB: sales_qty
    private Long salesAmount;      // DB: sales_amount
}
