package org.example.board_cafe_kiosk_2603.service.kiosk;

import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.service.kiosk.tableSession.TableSessionKioskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Log4j2
@SpringBootTest
class CafeTableSessionMapperKioskServiceTest {

    @Autowired
    private TableSessionKioskService tableSessionKioskService;

    @Test
    void createSessionTest() {
        tableSessionKioskService.createSession(1, 1, 2);
        log.info("sessionTest...");
    }

}