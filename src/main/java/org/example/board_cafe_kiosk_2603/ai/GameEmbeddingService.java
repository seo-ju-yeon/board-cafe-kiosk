package org.example.board_cafe_kiosk_2603.ai;

import lombok.extern.log4j.Log4j2;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * MariaDB의 게임 정보를 pgvector에 검색 문서로 저장하고 메뉴 ID로 갱신·삭제합니다.
 *
 * <p>인덱싱 시점에 판매 가능하고 삭제되지 않은 {@code GAME} 메뉴에 연결된 활성 게임 중
 * {@code NORMAL} 재고가 있는 게임을 저장 대상으로 삼습니다.</p>
 */
@Log4j2
@Service
public class GameEmbeddingService {
    private final JdbcTemplate mariaJdbcTemplate;  // 소스 데이터 (MariaDB)
    private final VectorStore vectorStore;  // 목적지 (PGVector)

    // 멀티 DB 환경에서 생성자 주입으로 명시적으로 처리
    public GameEmbeddingService(
            @Qualifier("mariaJdbcTemplate") JdbcTemplate mariaJdbcTemplate,
            VectorStore vectorStore) {
        this.mariaJdbcTemplate = mariaJdbcTemplate;
        this.vectorStore = vectorStore;
    }

    // 게임과 메뉴는 이름으로 연결하고, NORMAL 재고는 EXISTS로 존재 여부를 확인함
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

    /**
     * 검색 조건을 충족한 조회 결과를 메뉴 ID당 문서 한 건으로 만들어 일괄 저장합니다.
     *
     * <p>일괄 저장은 이번 조회 결과에 한정되며, 기존 문서 삭제는 개별 갱신·삭제 경로에서 처리합니다.</p>
     *
     * @return 벡터 저장소에 저장한 문서 수
     */
    public int embedAllGames() {
        log.info("[임베딩] 전체 게임 임베딩 시작");

        List<Map<String, Object>> games = mariaJdbcTemplate.queryForList(GAME_QUERY);
        log.info("[임베딩] 조회된 행 수 (중복 포함): {}", games.size());

        if (games.isEmpty()) {
            log.warn("[임베딩] 임베딩할 게임이 없습니다. (MariaDB 데이터를 확인)");
            return 0;
        }
        Map<String, Map<String, Object>> deduped = new LinkedHashMap<>();
        for (Map<String, Object> row : games) {
            String menuId = str(row.get("menu_id"));
            deduped.putIfAbsent(menuId, row);  // 첫 번째 행만 유지
        }
        log.info("[임베딩] 중복 제거 후 게임 수: {}", deduped.size());

        // 각 행을 AI 문서(Document) 객체로 변환
        List<Document> documents = deduped.values().stream()
                .map(this::toDocument)
                .toList();

        // 동일한 ID가 있으면 기존 내용을 업데이트(Upsert)함
        vectorStore.add(documents);

        log.info("[임베딩] {}개 게임 PGVector 저장 완료", documents.size());
        return documents.size();
    }

    /**
     * 메뉴 ID로 검색 대상 조건을 다시 확인해 해당 문서를 갱신합니다.
     *
     * <p>조회 결과가 있으면 기존 문서를 삭제한 뒤 새 문서를 저장합니다. 조회 결과가 비면 기존 문서를 삭제합니다.
     * 삭제 후 새 문서 저장에 실패하면 해당 메뉴의 검색 문서가 누락될 수 있습니다.</p>
     *
     * @param menuId 갱신할 게임 메뉴 ID
     */
    public void upsertGameByMenuId(Integer menuId) {
        String singleQuery = GAME_QUERY + " AND m.id = ?";
        List<Map<String, Object>> result = mariaJdbcTemplate.queryForList(singleQuery, menuId);

        if (result.isEmpty()) {
            log.info("[임베딩] menuId={} 조건 미충족 → 벡터에서 제외", menuId);
            deleteByMenuId(menuId);
            return;
        }

        deleteByMenuId(menuId);
        vectorStore.add(List.of(toDocument(result.get(0))));

        log.info("[임베딩] menuId={} 임베딩 완료", menuId);
    }

    /**
     * 특정 메뉴 ID에 해당하는 게임 문서를 벡터 저장소에서 삭제합니다.
     *
     * @param menuId 삭제할 게임 메뉴 ID
     */
    public void deleteByMenuId(Integer menuId) {
        // menu_id 기반의 고정 UUID를 생성하여 정확한 문서를 타겟팅해 삭제
        String documentId = menuIdToUuid(menuId);
        vectorStore.delete(List.of(documentId));
        log.info("[임베딩] menuId={} 벡터 삭제 (uuid={})", menuId, documentId);
    }

    /**
     * DB 조회 결과를 AI 검색에 사용할 자연어 문서로 변환합니다.
     *
     * @param row MariaDB에서 조회한 게임 데이터 행
     * @return VectorStore에 저장할 Document
     */
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
