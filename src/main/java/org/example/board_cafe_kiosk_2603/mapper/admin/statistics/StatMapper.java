package org.example.board_cafe_kiosk_2603.mapper.admin.statistics;

import org.apache.ibatis.annotations.Mapper;
import org.example.board_cafe_kiosk_2603.dto.admin.statistics.DailySalesDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.statistics.GameStatsDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.statistics.ItemSalesDTO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface StatMapper {
    /** * [Batch] 특정 날짜의 기존 일별 요약 데이터 삭제
     * 통계 재집계 시 데이터 중복 방지를 위해 수행합니다.
     **/
    void deleteDailySummary(LocalDate targetDate);

    /** * [Batch] 특정 날짜의 세션/주문을 집계하여 요약 테이블에 삽입
      - INNER JOIN을 통해 '인원 추가 (1명)' 수량을 초기 인원(initial_guest_cnt)과 합산하여 저장합니다.
      - 결제 완료된 세션(is_active = FALSE)의 매출과 이용 시간을 집계합니다.
     **/
    void insertDailySummaryFromSessions(LocalDate targetDate);

    /** [Batch] 특정 날짜의 기존 상품 판매 이력 삭제 **/
    void deleteItemSalesHistory(LocalDate targetDate);

    /**
      [Batch] 특정 날짜의 메뉴별 판매 수량/금액을 집계하여 히스토리 테이블에 삽입
      - 순수 상품 매출 통계를 위해 '인원 추가 (1명)' 항목은 제외하고 저장합니다.
      - 정상 결제 완료된(PENDING, CANCELLED 제외) 주문 건만 포함합니다.
     **/
    void insertItemSalesHistory(LocalDate targetDate);

    /**
      [조회] 특정 날짜의 카테고리별 매출 합계 조회
      - item_sales_history 테이블 기반으로 집계하므로 '인원 추가' 금액이 제외된 순수 카테고리 실적을 반환합니다.
     **/
    List<Map<String, Object>> getCategoryStatsByDate(@Param("targetDate") LocalDate targetDate);

    /**
      [조회] 기준 날짜로부터 최근 7일간의 요약 통계 리스트 조회
      - 대시보드의 주간 매출 차트 및 일평균 방문객 수 계산에 사용됩니다.
     **/
    List<DailySalesDTO> getWeeklyStats(@Param("endDate") LocalDate endDate);

    /**
      [조회] 기준 날짜의 인기 메뉴 TOP N 조회
      - '인원 추가' 항목이 제외된 상태로 가장 많이 팔린 메뉴 순으로 정렬하여 반환합니다.
     **/
    List<ItemSalesDTO> getTopSellingMenuByDate(@Param("targetDate") LocalDate targetDate, @Param("limit") int limit);

    /**
      [조회] 기준 날짜가 속한 '월(Month)'의 인기 보드게임 TOP N 조회
      - game_history 테이블을 기반으로 대여 횟수가 많은 순으로 정렬합니다.
     **/
    List<GameStatsDTO> getTopGamesByMonth(@Param("targetDate") LocalDate targetDate, @Param("limit") int limit);
}
