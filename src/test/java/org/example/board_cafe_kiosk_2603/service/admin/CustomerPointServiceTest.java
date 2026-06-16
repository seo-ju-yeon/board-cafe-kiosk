//package org.example.board_cafe_kiosk_2603.service.admin;
//
//import lombok.extern.log4j.Log4j2;
//import org.example.board_cafe_kiosk_2603.dto.admin.point.CustomerResponseDTO;
//import org.example.board_cafe_kiosk_2603.dto.admin.point.PointHistoryResponseDTO;
//import org.example.board_cafe_kiosk_2603.dto.admin.point.PointTransactionDTO;
//import org.example.board_cafe_kiosk_2603.service.admin.point.CustomerPointService;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.List;
//
//@Log4j2
//@SpringBootTest
//class CustomerPointServiceTest {
//    @Autowired
//    private CustomerPointService customerPointService;
//
//    @Test
//    void processPointTransactionEarnTest() { // 포인트 사용 없이 1만원 결제
//        PointTransactionDTO pointTransactionDTO = PointTransactionDTO.builder()
//                .phone("010-9999-8888")
//                .amount(10000) // 결제액
//                .usePoint(0) // 포인트 사용 안함
//                .isEarning(true)
//                .build();
//
//        CustomerResponseDTO result =
//                customerPointService.processPointTransaction(pointTransactionDTO);
//
//        log.info("결과 - phone: {}, 포인트: {}",
//                result.getPhone(),
//                result.getTotalPoint());
//    }
//
//    @Test
//    void processPointTransactionUseTest() { // 500포인트 사용 + 1만원 결제
//        PointTransactionDTO pointTransactionDTO = PointTransactionDTO.builder()
//                .phone("010-9999-8888")
//                .amount(10000)   // 결제액
//                .usePoint(500)   // 500포인트 사용
//                .isEarning(true)
//                .build();
//
//        CustomerResponseDTO result =
//                customerPointService.processPointTransaction(pointTransactionDTO);
//
//        log.info("결과 - phone: {}, 포인트: {}",
//                result.getPhone(),
//                result.getTotalPoint());
//    }
//
//    @Test
//    void findCustomerByPhoneTest() { // 기존에 등록된 번호로 조회
//        CustomerResponseDTO result =
//                customerPointService.findCustomerByPhone("010-9999-8888");
//
//        if (result == null) {
//            log.info("존재하지 않는 고객");
//        } else {
//            log.info("조회 결과 - phone: {}, 포인트: {}", result.getPhone(), result.getTotalPoint());
//        }
//    }
//
//    @Test
//    void getCustomerHistoryTest() {
//        // point 계좌 ID로 이력 조회 (DB에서 확인한 point.id 값 사용)
//        List<PointHistoryResponseDTO> historyList = customerPointService.getCustomerHistory(1);
//
//        historyList.forEach(history -> log.info("이력 - 날짜: {}, 변동: {}, 사유: {}, 잔여: {}",
//                history.getTransactionDate(),
//                history.getChangeAmount(),
//                history.getDescription(),
//                history.getRemainingPoint()));
//    }
//}