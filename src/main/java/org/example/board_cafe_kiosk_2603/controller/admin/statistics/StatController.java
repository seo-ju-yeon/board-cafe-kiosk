package org.example.board_cafe_kiosk_2603.controller.admin.statistics;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.dto.admin.statistics.DailySalesDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.statistics.GameStatsDTO;
import org.example.board_cafe_kiosk_2603.service.admin.statistics.StatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER')")
public class StatController {
    private final StatService statService;

    /**
     관리자 통계 페이지 이동
     */
    @GetMapping("/status")
    public String adminStatusPage(Model model) {
        log.info("--- StatController adminStatusPage ---");
        model.addAttribute("activePage", "salesStats");
        // 초기 진입 시 오늘 날짜 기준으로 설정
        model.addAttribute("selectedDate", LocalDate.now());
        return "admin/status";
    }

    /**
     통계 데이터 API (차트 및 요약 정보)
     */
    @GetMapping("/api/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatData(
            @RequestParam(value = "targetDate", required = false) String targetDateStr) {
        log.info("--- StatController ResponseEntity ---");
        log.info("targetDateStr: " + targetDateStr);

        LocalDate targetDate = (targetDateStr == null || targetDateStr.isEmpty())
                ? LocalDate.now() : LocalDate.parse(targetDateStr);

        // 실시간 반영 - 조회 시점에 해당 날짜 통계를 재집계하여 데이터 누락 방지
        statService.createDailyStatistics(targetDate);

        Map<String, Object> response = new HashMap<>();

        // 1. 주간 매출 (차트용)
        List<DailySalesDTO> weeklySales = statService.getWeeklyStats(targetDate);
        log.info("weeklySales : {}...", weeklySales);
        response.put("weeklySales", weeklySales);

        // 주간 일평균 매출 직접 계산
        long weeklyTotal = weeklySales.stream().mapToLong(DailySalesDTO::getTotalRevenue).sum();
        long weeklyAvg = weeklySales.isEmpty() ? 0 : weeklyTotal / weeklySales.size();

        // 2. 선택일 요약 데이터
        DailySalesDTO summary = weeklySales.stream()
                .filter(s -> s.getStatDate().equals(targetDate.toString()))
                .findFirst()
                .orElse(DailySalesDTO.builder().statDate(targetDate.toString())
                        .totalRevenue(0L).orderCount(0).visitCount(0).build());
        log.info("summary : {}...", summary.toString());
        response.put("summary", summary);

        // 3. 베스트 셀러
        log.info("topMenus : {}", statService.getTopSellingMenuByDate(targetDate, 5));
        response.put("topMenus", statService.getTopSellingMenuByDate(targetDate, 5));


        // 4. 카테고리 통계
        log.info("categoryStats : {}", statService.getCategoryStats(targetDate));
        response.put("categoryStats", statService.getCategoryStats(targetDate));

        // 5. 최근 7일간의 일별 매출 추이
        log.info("weeklySales : {}", weeklySales);
        response.put("weeklySales", weeklySales);

        // 6. 해당 기간(7일) 동안의 일일 평균 매출액
        log.info("weeklyAvgRevenue : {}", weeklyAvg);
        response.put("weeklyAvgRevenue", weeklyAvg);

        // 7. 월간 인기 보드게임 TOP 5
        List<GameStatsDTO> topGames = statService.getTopGamesByMonth(targetDate, 5);
        log.info("topGames : {}", topGames);
        response.put("topGames", topGames);

        return ResponseEntity.ok(response);
    }
}
