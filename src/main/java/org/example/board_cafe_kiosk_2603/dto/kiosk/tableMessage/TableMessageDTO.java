package org.example.board_cafe_kiosk_2603.dto.kiosk.tableMessage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class TableMessageDTO {
    private int id;
    private int tableId;
    private int macroId;
    private String content;
    private boolean isRead;
    private LocalDateTime createdAt;
}
