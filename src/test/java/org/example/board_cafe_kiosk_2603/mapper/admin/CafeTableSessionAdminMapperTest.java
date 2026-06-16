//package org.example.board_cafe_kiosk_2603.mapper.admin;
//
//import lombok.extern.log4j.Log4j2;
//import org.example.board_cafe_kiosk_2603.domain.TableSession;
//import org.example.board_cafe_kiosk_2603.mapper.admin.table.TableSessionAdminMapper;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//@Log4j2
//@SpringBootTest
//class CafeTableSessionAdminMapperTest {
//    @Autowired
//    private TableSessionAdminMapper tableSessionMapper;
//
//    @Test
//    void selectActiveByTableIdTest() {
//        TableSession tableSession = tableSessionMapper.selectActiveByTableId(1);
//        log.info("활성 세션 조회: {}", tableSession);
//    }
//
//    @Test
//    void updateTotalAmountTest() {
//        // 활성 세션 조회 후 정산 금액 업데이트
//        TableSession tableSession = tableSessionMapper.selectActiveByTableId(1);
//
//        TableSession updated = TableSession.builder()
//                .id(tableSession.getId())
//                .totalAmount(25000)
//                .build();
//        tableSessionMapper.updateTotalAmount(updated);
//        log.info("정산 금액 업데이트 완료: {}", updated);
//    }
//
//    @Test
//    void closeSessionTest() {
//        tableSessionMapper.closeSession(1);
//        log.info("세션 종료 완료 - tableId: 1");
//
//        // 종료 후 조회 시 null 이어야 정상
//        TableSession tableSession = tableSessionMapper.selectActiveByTableId(1);
//        log.info("종료 후 활성 세션 조회: {}", tableSession);
//    }
//
//}