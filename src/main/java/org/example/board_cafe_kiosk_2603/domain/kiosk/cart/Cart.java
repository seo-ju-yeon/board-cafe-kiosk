package org.example.board_cafe_kiosk_2603.domain.kiosk.cart;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Cart {
    private int id;
    private int tableId;
    private LocalDateTime updatedAt;
}
