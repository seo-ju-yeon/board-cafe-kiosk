package org.example.board_cafe_kiosk_2603.ai;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class GameEmbeddingRunner implements ApplicationRunner {

    /* 애플리케이션이 구동되는 시점에 자동으로 실행되어 RAG용 벡터 데이터를 최신화함 */

    private final GameEmbeddingService gameEmbeddingService;
    private final JdbcTemplate pgVectorJdbcTemplate;

    // 멀티 DB 환경에서 의존성 주입
    public GameEmbeddingRunner(
            GameEmbeddingService gameEmbeddingService,
            @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate) {
        this.gameEmbeddingService = gameEmbeddingService;
        this.pgVectorJdbcTemplate = pgVectorJdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("========================================");
        log.info("[시작] 게임 임베딩 자동 초기화");
        log.info("========================================");

        // 1. PostgreSQL 인프라 확인하고 작동 설치 시도
        ensureVectorExtension();

        // 2. 데이터 동기화 (MariaDB → PGVector)
        // MariaDB에 저장된 보드게임 상세 정보를 읽어와서 벡터화(Embedding)한 후 Vector Store에 저장
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
//            log.error("[초기화] 수동 재실행: POST /api/ai/admin/reindex-games");
        }
    }

    /* PostgresSQL 벡터 확장 기능 활성화 */
    // SQL 문법이 MariaDB와 다르므로 반드시 pgVectorJdbcTemplate을 통해 실행
    private void ensureVectorExtension() {
        try {
            pgVectorJdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            log.info("[PGVector] vector 확장 설치 확인 완료");
        } catch (Exception e) {
            log.warn("[PGVector] vector 확장 설치 실패: {}", e.getMessage());
        }
    }
}
