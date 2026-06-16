package org.example.board_cafe_kiosk_2603.mapper.admin.point;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.board_cafe_kiosk_2603.domain.admin.point.Point;
import org.example.board_cafe_kiosk_2603.domain.admin.point.PointHistory;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;

import java.util.List;

@Mapper
public interface PointMapper {

    /* ===== point 테이블 ===== */

    // 전체 포인트 계좌 목록 조회 (관리자 화면)
    List<Point> findAll();

    // 전화번호로 포인트 계좌 조회
    Point findByPhone(String phone);

    // 포인트 계좌 생성 (키오스크 전화번호 입력 시 자동 생성)
    void insert(Point point);

    // 잔액 업데이트 (적립/사용 시)
    void updateBalance(Point point);

    // 전체 포인트 합계 (통계)
    int sumTotalBalance();

    // 전체 고객 수 (통계)
    int countAll();

    /* ===== point_history 테이블 ===== */

    // 특정 포인트 계좌의 이력 조회
    List<PointHistory> findHistoryByPointId(int pointId);

    // 포인트 이력 추가
    void insertHistory(PointHistory history);

    // 주문 단위 포인트 사용 이력 존재 여부
    int countUseHistoryByOrderId(@Param("orderId") Long orderId);

    /*=============== 페이징 ==============*/
    // [페이징된 포인트 목록 조회]
    List<Point> selectList(PageRequestDTO pageRequestDTO);

    // [조건에 맞는 전체 데이터 개수 조회]
    int selectCount(PageRequestDTO pageRequestDTO);
}
