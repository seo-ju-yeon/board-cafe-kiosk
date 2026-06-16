package org.example.board_cafe_kiosk_2603.scheduler;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
class StatSchedulerTest {
    @Autowired
    private StatScheduler statScheduler;

    @Test
    void runManualStatJobTest() {
        // 1. 테스트하고 싶은 날짜 설정 (예: 3일 전)
        LocalDate targetDate = LocalDate.now();

        log.info(">>>>>> 통합 테스트 시작: 대상 날짜 = {}", targetDate);

        // 2. 실제 메서드 호출
        // 이 메서드 안에서 jobLauncher.run()이 실제로 돌아갑니다.
        statScheduler.runManualStatJob(targetDate);

        log.info(">>>>>> 통합 테스트 종료. DB의 BATCH_JOB_EXECUTION 테이블을 확인하세요.");
    }
}