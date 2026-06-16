package org.example.board_cafe_kiosk_2603.domain.admin.macro;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MacroMessage {
    private Integer id;
    private String direction;    // ENUM: 'STAFF_TO_TABLE', 'TABLE_TO_STAFF'
    private String messageText;
    private boolean isActive;
}
