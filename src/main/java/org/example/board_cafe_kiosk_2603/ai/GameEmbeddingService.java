package org.example.board_cafe_kiosk_2603.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Log4j2
@Service
public class GameEmbeddingService {
    /* 지식 베이스 임베딩 서비스 */
    // MariaDB(정형 데이터)를 읽어 VectorStore로 변환/저장하는 역할

    private final JdbcTemplate mariaJdbcTemplate;  // 소스 데이터 (MariaDB)
    private final VectorStore vectorStore;  // 목적지 (PGVector)

    // 멀티 DB 환경에서 생성자 주입으로 명시적으로 처리
    public GameEmbeddingService(
            @Qualifier("mariaJdbcTemplate") JdbcTemplate mariaJdbcTemplate,
            VectorStore vectorStore) {
        this.mariaJdbcTemplate = mariaJdbcTemplate;
        this.vectorStore = vectorStore;
    }

    /* 임베딩 대상 게임 조회 */
    // 키오스크에서 실제로 '판매 중'이고 '재고가 있는' '게임'만 선별
    private static final String GAME_QUERY = """
            SELECT
                m.id          AS menu_id,
                m.name        AS name,
                m.description AS description,
                g.id          AS game_id,
                g.min_players,
                g.max_players,
                g.play_time
            FROM menu m
            JOIN category c
                ON m.category_id = c.id
               AND c.type = 'GAME'
            JOIN game g
                ON g.name = m.name
               AND g.is_active = TRUE
            WHERE m.is_available = TRUE
              AND m.is_deleted   = FALSE
              AND EXISTS (
                  SELECT 1
                  FROM game_item gi
                  WHERE gi.game_id = g.id
                    AND gi.status = 'NORMAL'
              )
            """;

    /* 전체 동기화 */
    // 모든 게임 정보를 벡터 저장소에 다시 넣음
    // 최초 1회 반드시 실행해야 함
    // 이후 대량 게임 데이터 변경 시에도 사용
    public int embedAllGames() {
        log.info("[임베딩] 전체 게임 임베딩 시작");

        List<Map<String, Object>> games = mariaJdbcTemplate.queryForList(GAME_QUERY);
        log.info("[임베딩] 조회된 행 수 (중복 포함): {}", games.size());

        if (games.isEmpty()) {
            log.warn("[임베딩] 임베딩할 게임이 없습니다. (MariaDB 데이터를 확인)");
            return 0;
        }
        // 쿼리 구조상 동일 게임이 여러 번 조회될 수 있으므로 menu_id를 키로 사용하여 1건씩만 필터링함. (중복 제거)
        // ★ 버그 수정: menu_id 기준으로 중복 제거 후 deduped.values()로 Document 생성
        // 이전 코드에서는 deduped를 만들고도 원본 games로 Document를 만드는 버그가 있었음
        // game_item이 여러 개면 같은 게임이 여러 행으로 조회되므로 반드시 중복 제거 필요
        Map<String, Map<String, Object>> deduped = new LinkedHashMap<>();
        for (Map<String, Object> row : games) {
            String menuId = str(row.get("menu_id"));
            deduped.putIfAbsent(menuId, row);  // 첫 번째 행만 유지
        }
        log.info("[임베딩] 중복 제거 후 게임 수: {}", deduped.size());

        // 각 행을 AI 문서(Document) 객체로 변환
//        List<Document> documents = games.stream()
//                .map(this::toDocument)
//                .toList();
        // ★ deduped.values() 사용 (버그 수정 핵심)
        List<Document> documents = deduped.values().stream()
                .map(this::toDocument)
                .toList();

        // 동일한 ID가 있으면 기존 내용을 업데이트(Upsert)함
        vectorStore.add(documents);

        log.info("[임베딩] {}개 게임 PGVector 저장 완료", documents.size());
        return documents.size();
    }

    /* 단일 동기화 */
    // 특정 게임의 정보가 바뀌었을 때 실시간으로 AI 지식을 업데이트함
    public void upsertGameByMenuId(Integer menuId) {
        String singleQuery = GAME_QUERY + " AND m.id = ?";
        List<Map<String, Object>> result = mariaJdbcTemplate.queryForList(singleQuery, menuId);

        if (result.isEmpty()) {
            // 조건 미충족 (비활성화, 재고 없음 등) → 기존 벡터 삭제
            log.info("[임베딩] menuId={} 조건 미충족 → 벡터에서 제외", menuId);
            deleteByMenuId(menuId);
            return;
        }

        // 기존 데이터를 지우고 새 정보를 저장하여 데이터 정합성 유지
        deleteByMenuId(menuId);
        vectorStore.add(List.of(toDocument(result.get(0))));

        log.info("[임베딩] menuId={} 임베딩 완료", menuId);
    }

    /* 단일 데이터 삭제 */
    // 특정 게임을 AI 지식 베이스에서 제거
    public void deleteByMenuId(Integer menuId) {
        // menu_id 기반의 고정 UUID를 생성하여 정확한 문서를 타겟팅해 삭제
        String documentId = menuIdToUuid(menuId);
        vectorStore.delete(List.of(documentId));
        log.info("[임베딩] menuId={} 벡터 삭제 (uuid={})", menuId, documentId);
    }

    /* 변환 로직 */
    // DB 행 데이터를 AI가 읽을 수 있는 자연어 문장으로 변환합니다.
    private Document toDocument(Map<String, Object> row) {
        String name = str(row.get("name"));
        String description = str(row.get("description"));
        Integer minPlayers = toInt(row.get("min_players"));
        Integer maxPlayers = toInt(row.get("max_players"));
        Integer playTime = toInt(row.get("play_time"));
        String menuId = str(row.get("menu_id"));
        String gameId = str(row.get("game_id"));

        // LLM 컨텍스트로 읽을 문장 구성 (자연어 형태)
        String content = String.format(
                "게임명: %s\n설명: %s\n플레이 인원: %s~%s명\n평균 플레이 시간: %s분",
                name,
                (description != null && !description.isBlank()) ? description : "설명 없음",
                minPlayers != null ? minPlayers : "?",
                maxPlayers != null ? maxPlayers : "?",
                playTime != null ? playTime : "?"
        );

        // 메타데이터
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "game");
        metadata.put("menuId", menuId);
        metadata.put("gameId", gameId);
        metadata.put("gameName", name);

        // menu_id를 기반으로 한 고정 UUID를 ID로 사용
        String documentId = menuIdToUuid(Integer.parseInt(menuId));
        log.info("[문서 생성]: {}", menuId);

        return new Document(documentId, content, metadata);
    }

    /* UUID 생성 */
    // 동일한 menu_id에 대해 항상 같은 UUID를 생성하여 벡터 저장소 내에서 데이터가 중복되지 않고 덮어쓰기 되도록 함.
    private String menuIdToUuid(Integer menuId) {
        return UUID.nameUUIDFromBytes(("game-menu-" + menuId).getBytes()).toString();
    }

    // 안전한 문자열 변환
    private String str(Object o) {
        return o != null ? o.toString() : null;
    }

    // 안전한 숫자 변환
    private Integer toInt(Object o) {
        if (o == null) return null;  // 데이터가 없으면 null 변환
        if (o instanceof Integer i) return i;  // 이미 숫자형이면 그대로 반환
        try {
            return Integer.parseInt(o.toString());  // 문자열이라면 숫자로 변환 시도
        } catch (Exception e) {
            return null;  // 변환 실패 시 에러 대신 Null 반환
        }
    }
}
