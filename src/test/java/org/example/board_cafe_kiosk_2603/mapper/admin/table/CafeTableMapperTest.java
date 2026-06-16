package org.example.board_cafe_kiosk_2603.mapper.admin.table;

import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.table.CafeTable;
import org.example.board_cafe_kiosk_2603.dto.kiosk.order.OrderItemDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Log4j2
@SpringBootTest
class CafeTableMapperTest {
    @Autowired
    private CafeTableMapper cafeTableMapper;

    @Test
    public void selectUnreadMessageContentsTest() {
        Integer tableId = 1;
        List<String> messages = cafeTableMapper.selectUnreadMessageContents(tableId);

        log.info("=== 미확인 손님 메시지 목록 (총 {}건) ===", messages.size());
        messages.forEach(content -> log.info("요청 내용: {}", content));

        // 데이터가 있다면, direction이 TABLE_TO_STAFF인 것만 가져왔는지 로그로 확인 가능합니다.
    }

    @Test
    public void selectAllTablesTest() {
        List<CafeTable> tables = cafeTableMapper.selectAllTables();

        log.info("=== 전체 테이블 현황 ===");
        for (CafeTable table : tables) {
            log.info("테이블 {}번 | 상태: {} | 손님요청 존재여부: {}",
                    table.getTableNumber(), table.getStatus(), table.isHasUnreadMessage());
        }
    }

    @Test
    public void selectActiveOrderItemsTest() {
        // 실제 DB에 존재하는 유효한 세션 ID 사용
        Long activeSessionId = 1L;
        List<OrderItemDTO> orderItems = cafeTableMapper.selectActiveOrderItems(activeSessionId);

        log.info("=== 세션 {}번의 진행 중인 주문 목록 ===", activeSessionId);
        orderItems.forEach(item ->
                log.info("메뉴: {} | 수량: {} | 가격: {}원", item.getMenuName(), item.getQuantity(), item.getPrice())
        );
    }

    @Test
    public void updateMessagesReadStatusTest() {
        Integer tableId = 1;
        int updatedCount = cafeTableMapper.updateMessagesReadStatus(tableId);

        log.info("읽음 처리 완료된 손님 메시지 수: {}건", updatedCount);
    }

    @Test
    public void resetAllTablesAtMidnightTest() {
        int resetCount = cafeTableMapper.resetAllTablesAtMidnight();
        log.info("리셋 처리된 테이블 수: {}건", resetCount);

        List<CafeTable> tables = cafeTableMapper.selectAllTables();
        boolean allEmpty = tables.stream().allMatch(t -> "EMPTY".equals(t.getStatus()));

        log.info("모든 테이블이 EMPTY 상태인가? : {}", allEmpty);
    }

}