package org.example.board_cafe_kiosk_2603.ai;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@ConditionalOnProperty(name = "app.rag.reindex-on-startup", havingValue = "true", matchIfMissing = true)
public class GameEmbeddingRunner implements ApplicationRunner {

    // 시작 시 재인덱싱 설정이 활성화되어 있으면 검색 대상 게임 문서를 일괄 저장함

    private final GameEmbeddingService gameEmbeddingService;

    public GameEmbeddingRunner(GameEmbeddingService gameEmbeddingService) {
        this.gameEmbeddingService = gameEmbeddingService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("========================================");
        log.info("[시작] 게임 임베딩 자동 초기화");
        log.info("========================================");

        try {
            int count = gameEmbeddingService.embedAllGames();
            if (count == 0) {
                log.warn("[초기화] 저장된 게임이 없습니다. MariaDB에 게임 데이터를 확인하세요.");
            } else {
                log.info("[초기화] 완료 - {}개 게임이 AI 검색에 등록되었습니다.", count);
            }
        } catch (Exception e) {
            // 임베딩 실패해도 앱은 정상 기동되도록 예외 처리
            log.error("[초기화] 초기화 임베딩 실패, 앱은 계속 실행됩니다.");
            log.error("[초기화] 원인: {}", e.getMessage());
        }
    }

}
