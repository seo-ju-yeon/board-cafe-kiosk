package org.example.board_cafe_kiosk_2603.mapper.admin;

import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.point.Point;
import org.example.board_cafe_kiosk_2603.domain.admin.point.PointHistory;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;
import org.example.board_cafe_kiosk_2603.mapper.admin.point.PointMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Log4j2
@SpringBootTest
class PointMapperTest {

    @Autowired
    private PointMapper pointMapper;

    @Test
    @DisplayName("포인트 계좌 생성 후 전화번호로 조회 성공")
    void insert_and_findByPhone() {
        Point point = Point.builder().phone("010-9999-0001").balance(0).build();
        pointMapper.insert(point);
        assertThat(point.getId()).isPositive();

        Point found = pointMapper.findByPhone("010-9999-0001");
        assertThat(found).isNotNull();
        assertThat(found.getPhone()).isEqualTo("010-9999-0001");
        assertThat(found.getBalance()).isEqualTo(0);
    }

    @Test
    @DisplayName("존재하지 않는 전화번호 조회 시 null 반환")
    void findByPhone_notFound() {
        Point found = pointMapper.findByPhone("000-0000-0000");
        assertThat(found).isNull();
    }

    @Test
    @DisplayName("잔액 업데이트 성공")
    void updateBalance() {
        Point point = Point.builder().phone("010-9999-0002").balance(0).build();
        pointMapper.insert(point);

        Point updated = Point.builder()
                .id(point.getId())
                .phone(point.getPhone())
                .balance(5000)
                .build();
        pointMapper.updateBalance(updated);

        Point found = pointMapper.findByPhone("010-9999-0002");
        assertThat(found.getBalance()).isEqualTo(5000);
    }

    @Test
    @DisplayName("전체 고객 수 및 총 잔액 집계 성공")
    void countAll_and_sumTotalBalance() {
        int before = pointMapper.countAll();
        pointMapper.insert(Point.builder().phone("010-9999-0005").balance(3000).build());

        assertThat(pointMapper.countAll()).isEqualTo(before + 1);
        assertThat(pointMapper.sumTotalBalance()).isGreaterThanOrEqualTo(3000);
    }

    @Test
    @DisplayName("포인트 이력 추가 후 조회 성공")
    void insertHistory_and_findHistory() {
        Point point = Point.builder().phone("010-9999-0006").balance(1000).build();
        pointMapper.insert(point);

        PointHistory history = PointHistory.builder()
                .pointId(point.getId())
                .type("EARN")
                .amount(1000)
                .balanceAfter(1000)
                .build();
        pointMapper.insertHistory(history);
        assertThat(history.getId()).isPositive();

        List<PointHistory> list = pointMapper.findHistoryByPointId(point.getId());
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getType()).isEqualTo("EARN");
        assertThat(list.get(0).getAmount()).isEqualTo(1000);
        assertThat(list.get(0).getBalanceAfter()).isEqualTo(1000);
    }

    /*=============페이징==============*/
    @Test
    @DisplayName("페이징 포인트 목록 조회 성공")
    void selectList() {
        pointMapper.insert(Point.builder().phone("010-9999-0003").balance(0).build());
        pointMapper.insert(Point.builder().phone("010-9999-0004").balance(0).build());

        PageRequestDTO pageRequestDTO = PageRequestDTO.builder()
                .page(1)
                .size(10)
                .build();

        List<Point> list = pointMapper.selectList(pageRequestDTO);
        assertThat(list).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("keyword로 전화번호 검색 조회 성공")
    void selectList_withKeyword() {
        pointMapper.insert(Point.builder().phone("010-9999-0007").balance(0).build());

        PageRequestDTO pageRequestDTO = PageRequestDTO.builder()
                .page(1)
                .size(10)
                .keyword("010-9999-0007")
                .build();

        List<Point> list = pointMapper.selectList(pageRequestDTO);
        assertThat(list).hasSizeGreaterThanOrEqualTo(1);
        assertThat(list.get(0).getPhone()).isEqualTo("010-9999-0007");
    }

    @Test
    @DisplayName("전체 개수 조회 성공")
    void selectCount() {
        int before = pointMapper.selectCount(PageRequestDTO.builder().page(1).size(10).build());
        pointMapper.insert(Point.builder().phone("010-9999-0008").balance(0).build());

        int after = pointMapper.selectCount(PageRequestDTO.builder().page(1).size(10).build());
        assertThat(after).isEqualTo(before + 1);
    }
}
