package org.example.board_cafe_kiosk_2603.domain.admin.statistics;

import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DailySalesSummary {
    private LocalDate statDate;
    private Long totalRevenue;
    private Integer orderCount;
    private Integer visitCount;
    private Integer avgUsageTime;
}
