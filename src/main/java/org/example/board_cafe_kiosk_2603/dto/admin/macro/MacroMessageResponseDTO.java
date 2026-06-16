package org.example.board_cafe_kiosk_2603.dto.admin.macro;

import lombok.*;
import org.example.board_cafe_kiosk_2603.domain.admin.macro.MacroMessage;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MacroMessageResponseDTO {
    private Integer id;
    private String direction;
    private String messageText;

    // Entity를 DTO로 변환하는 정적 팩토리 메서드
    public static MacroMessageResponseDTO from(MacroMessage entity) {
        return MacroMessageResponseDTO.builder()
                .id(entity.getId())
                .direction(entity.getDirection())
                .messageText(entity.getMessageText())
                .build();
    }
}