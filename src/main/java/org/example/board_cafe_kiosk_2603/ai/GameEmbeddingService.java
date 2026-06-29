package org.example.board_cafe_kiosk_2603.ai;

import lombok.extern.log4j.Log4j2;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * MariaDB의 보드게임 데이터를 PGVector 기반 VectorStore에 동기화하는 서비스입니다.
 *
 * <p>키오스크에서 실제 추천 가능한 게임만 임베딩 대상으로 삼기 위해
 * 판매 가능 여부, 삭제 여부, 게임 활성 상태, 정상 재고 존재 여부를 함께 확인합니다.</p>
 *
 * <p>관리자가 게임/메뉴/재고 정보를 변경했을 때 벡터 저장소도 함께 갱신하여
 * 실제 판매 상태와 AI 추천 답변의 데이터 정합성을 맞추는 역할을 합니다.</p>
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

    /**
     * 추천 가능한 전체 게임 데이터를 조회하여 벡터 저장소에 일괄 저장합니다.
     *
     * <p>게임 재고 테이블과의 관계 때문에 동일한 게임이 여러 행으로 조회될 수 있으므로,
     * {@code menu_id} 기준으로 중복을 제거한 뒤 문서를 생성합니다.</p>
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
        // game_item이 여러 개면 같은 게임이 여러 행으로 조회되므로 menu_id 기준으로 1건만 유지합니다.
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
     * 특정 메뉴 ID에 해당하는 게임 정보를 벡터 저장소에 갱신합니다.
     *
     * <p>게임이 비활성화되었거나 정상 재고가 없어 추천 대상에서 제외되는 경우,
     * 기존 벡터 문서를 삭제하여 AI 답변에 노출되지 않도록 처리합니다.</p>
     *
     * @param menuId 갱신할 게임 메뉴 ID
     */
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
