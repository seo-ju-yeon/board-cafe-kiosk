package org.example.board_cafe_kiosk_2603.dto.admin.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySalesSummary {
    private Long id;              // PK
    private LocalDate statDate;   // 통계 날짜 (2026-04-01)
    private Long totalRevenue;    // 총 매출액
    private Integer orderCount;   // 총 주문 건수
    private Integer visitCount;   // 총 방문자 수 (세션 수)
    private Integer avgUsageTime; // 평균 이용 시간 (분 단위)
}
