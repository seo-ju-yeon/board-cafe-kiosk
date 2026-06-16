package org.example.board_cafe_kiosk_2603.service.admin.statistics;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
class StatServiceTest {
    @Autowired
    private StatService statService;

    @Test
    void generateMarchFullStats() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        statService.createStatisticsForPeriod(start, end);
    }

}