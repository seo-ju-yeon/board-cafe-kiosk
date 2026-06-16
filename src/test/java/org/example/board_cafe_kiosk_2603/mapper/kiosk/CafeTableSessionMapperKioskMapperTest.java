//package org.example.board_cafe_kiosk_2603.mapper.kiosk;
//
//import lombok.extern.log4j.Log4j2;
//import org.example.board_cafe_kiosk_2603.domain.TableSession;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//@Log4j2
//@SpringBootTest
//class CafeTableSessionMapperKioskMapperTest {
//    @Autowired
//    private TableSessionKioskMapper tableSessionMapper;
//
//    @Test
//    void insertSessionTest() {
//        TableSession tableSession = TableSession.builder()
//                .tableId(1) // 테스트용 테이블 ID
//                .packageId(1) // 테스트용 패키지 ID
//                .initialGuestCnt(2) // 입장 인원 2명
//                .build();
//        tableSessionMapper.insertSession(tableSession);
//        log.info("생성된 세션: {}", tableSession);
//    }
//}