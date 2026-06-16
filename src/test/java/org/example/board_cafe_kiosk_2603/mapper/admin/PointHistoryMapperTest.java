//package org.example.board_cafe_kiosk_2603.mapper.admin;
//
//import lombok.extern.log4j.Log4j2;
//import org.example.board_cafe_kiosk_2603.mapper.admin.point.PointHistoryMapper;
//import org.example.board_cafe_kiosk_2603.mapper.admin.point.PointMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//@Log4j2
//@SpringBootTest
//class PointHistoryMapperTest {
//    @Autowired
//    private PointMapper pointMapper;
//
//    @Autowired
//    private PointHistoryMapper pointHistoryMapper;
//
////    @Test
////    void insertHistoryTest() {
////        // point 계좌 조회 후 이력 저장
////        Point point = pointMapper.selectByPhone("010-1111-2222");
////
////        PointHistory history = PointHistory.builder()
////                .pointId(point.getId())
////                .orderId(null)
////                .type("EARN")
////                .amount(500)
////                .balanceAfter(500)
////                .build();
////
////        pointHistoryMapper.insertHistory(history);
////        log.info("저장된 이력: {}", history);
////    }
//
////    @Test
////    void selectByPointIdTest() {
////        // point 계좌 조회 후 이력 조회
////        Point point = pointMapper.selectByPhone("010-1111-2222");
////
////        List<PointHistory> list = pointHistoryMapper.selectByPointId(point.getId());
////        list.forEach(h -> log.info("이력: {}", h));
////    }
//}