package org.example.board_cafe_kiosk_2603.dto.admin.statistics;

import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GameStatsDTO {
    private String gameName;
    private int rentCount;
}
