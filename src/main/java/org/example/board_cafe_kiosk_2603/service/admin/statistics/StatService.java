package org.example.board_cafe_kiosk_2603.service.admin.statistics;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.dto.admin.statistics.DailySalesDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.statistics.GameStatsDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.statistics.ItemSalesDTO;
import org.example.board_cafe_kiosk_2603.mapper.admin.statistics.StatMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class StatService {
    private final StatMapper statMapper;

    /**
     * 단일 날짜 동기화 - 특정 날짜의 모든 통계 데이터를 갱신 (일일 요약 + 상품별 히스토리)
     * Mapper의 INNER JOIN 로직을 통해 '인원 추가' 수량을 실제 방문자 수에 합산
     * 상품 히스토리 저장 시 '인원 추가' 매출은 자동으로 제외
     */
    public void createDailyStatistics(LocalDate targetDate) {
        log.info("--- StatService createDailyStatistics ---");
        log.info("{} 날짜 통계 데이터 재집계", targetDate);

        // 1. 일일 매출 요약 (기존 데이터 삭제 후 JOIN 기반 재삽입)
        // SQL 내부: ts.initial_guest_cnt + 추가인원 수량 합산 처리됨
        statMapper.deleteDailySummary(targetDate);
        statMapper.insertDailySummaryFromSessions(targetDate);

        // 2. 상품별 판매 히스토리 (기존 데이터 삭제 후 재삽입)
        // SQL 내부: menu.name != '인원 추가 (1명)' 조건으로 순수 메뉴 실적만 저장됨
        statMapper.deleteItemSalesHistory(targetDate);
        statMapper.insertItemSalesHistory(targetDate);

        log.info("{} 날짜 통계 갱신 완료", targetDate);
    }

    /**
     * 기간 동기화 특정 기간(예: 최근 한 달)의 통계를 한 번에 초기화할 때 사용
     * 대규모 데이터 수정이나 로직 변경 후 과거 데이터를 일괄 갱신할 때 유용
     */
    public void createStatisticsForPeriod(LocalDate startDate, LocalDate endDate) {
        log.info("--- StatService createStatisticsForPeriod ---");
        log.info("{} 부터 {} 까지 기간 통계 생성", startDate, endDate);
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            createDailyStatistics(current);
            current = current.plusDays(1);
        }
    }

    /**
     * 조회 기준 날짜 포함 최근 7일간의 요약 통계 데이터 조회 (차트 및 요약용)
     */
    @Transactional(readOnly = true)
    public List<DailySalesDTO> getWeeklyStats(LocalDate endDate) {
        log.info("--- StatService getWeeklyStats ---");

        return statMapper.getWeeklyStats(endDate);
    }

    /**
     * 조회 특정 날짜의 인기 메뉴 TOP N 조회
     * '인원 추가' 항목이 이미 히스토리 저장 단계에서 배제되었으므로 순수 인기 상품만 반환
     */
    @Transactional(readOnly = true)
    public List<ItemSalesDTO> getTopSellingMenuByDate(LocalDate targetDate, int limit) {
        log.info("--- StatService getTopSellingMenuByDate ---");

        return statMapper.getTopSellingMenuByDate(targetDate, limit);
    }

    /**
     * 조회 - 특정 날짜의 카테고리별 매출 통계 가공
     * 프론트엔드 차트 라이브러리(Chart.js 등) 형식에 맞게 라벨과 데이터 값을 분리하여 반환
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCategoryStats(LocalDate targetDate) {
        log.info("--- StatService getCategoryStats ---");

        // 이미 '인원 추가' 금액이 제외된 item_sales_history 기반으로 조회됨
        List<Map<String, Object>> stats = statMapper.getCategoryStatsByDate(targetDate);

        List<String> labels = stats.stream()
                .map(s -> String.valueOf(s.get("categoryName")))
                .collect(Collectors.toList());

        List<Long> values = stats.stream()
                .map(s -> Long.parseLong(String.valueOf(s.get("totalAmount"))))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("values", values);
        return result;
    }

    /**
     * 조회 - 기준 날짜가 속한 월의 인기 보드게임 TOP N 조회 (대여 횟수 기준)
     */
    @Transactional(readOnly = true)
    public List<GameStatsDTO> getTopGamesByMonth(LocalDate targetDate, int limit) {
        log.info("--- StatService getTopGamesByMonth ---");
        return statMapper.getTopGamesByMonth(targetDate, limit);
    }
}
