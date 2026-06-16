package org.example.board_cafe_kiosk_2603.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Log4j2
@Component
@EnableScheduling
@RequiredArgsConstructor
public class StatScheduler {
    private final JobLauncher jobLauncher;
    private final Job dailyRevenueJob; // BatchConfig의 @Bean 메서드 명과 동일해야 함

    /**
     * 매일 새벽 03:00:00에 Spring Batch Job 실행
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void runDailyStatJob() {
        try {
            // 1. 어제 날짜를 문자열로 준비 (Tasklet의 @Value와 매칭)
            String yesterday = LocalDate.now().minusDays(1).toString();

            // 2. 배치 실행을 위한 파라미터 생성
            // (time 파라미터는 동일한 파라미터로 재실행이 안 되는 Batch 특성상 매번 다르게 주기 위함입니다)
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("targetDate", yesterday)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            log.info("[Batch Scheduler] 통계 배치 작업 시작 (대상일자: {})", yesterday);

            // 3. Job 실행
            jobLauncher.run(dailyRevenueJob, jobParameters);

            log.info("Batch Scheduler Success...");

        } catch (Exception e) {
            log.error("Batch Scheduler Failed : {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 수동 실행을 위한 메서드 - 테스트 코드에서 호출
     */
    public void runManualStatJob(LocalDate targetDate) {
        log.info("[Batch Scheduler] 수동 통계 배치 작업 요청 확인 (대상일자: {})", targetDate);

        // 파라미터 생성 (수동 실행 시에도 매번 새로운 JobInstance로 인식되도록 time 추가)
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", targetDate.toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        try {
            log.info("[Batch Scheduler] 수동 통계 배치 작업 시작...");

            // Job 실행
            jobLauncher.run(dailyRevenueJob, jobParameters);

            log.info("[Batch Scheduler] 수동 통계 배치 작업 완료 성공! (대상일자: {})", targetDate);

        } catch (Exception e) {
            log.error("[Batch Scheduler] 수동 통계 배치 작업 중 오류 발생: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
