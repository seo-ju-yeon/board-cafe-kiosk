package org.example.board_cafe_kiosk_2603.domain.admin.macro;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AdminTableMessage {
    private Long id;
    private Integer tableId;
    private Integer macroId;
    private String direction;
    private String content;
    private boolean isRead;
    private LocalDateTime createdAt;
}
