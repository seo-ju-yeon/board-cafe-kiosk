package org.example.board_cafe_kiosk_2603.domain.kiosk.tableMessage;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TableMessage {
    private long          id;
    private int           tableId;
    private Integer       macroId;     // nullable — 자유 입력 시 null
    private String        direction;   // STAFF_TO_TABLE | TABLE_TO_STAFF
    private String        content;
    private boolean       isRead;
    private LocalDateTime createdAt;
}
