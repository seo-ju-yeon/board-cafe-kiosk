package org.example.board_cafe_kiosk_2603.service.admin.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.ai.GameEmbeddingService;
import org.example.board_cafe_kiosk_2603.domain.admin.product.GameItem;
import org.example.board_cafe_kiosk_2603.domain.admin.product.GameItemStatus;
import org.example.board_cafe_kiosk_2603.dto.admin.product.GameItemRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.product.GameItemResponseDTO;
import org.example.board_cafe_kiosk_2603.mapper.admin.product.MenuMapper;
import org.example.board_cafe_kiosk_2603.mapper.admin.table.CafeTableMapper;
import org.example.board_cafe_kiosk_2603.mapper.admin.product.GameItemMapper;
import org.example.board_cafe_kiosk_2603.service.kiosk.order.OrderService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class GameItemServiceImpl implements GameItemService {

    private final GameItemMapper gameItemMapper;
    private final CafeTableMapper cafeTableMapper;
    private final ModelMapper modelMapper;
    private final OrderService orderService;
    private final MenuMapper menuMapper; // menu_id 조회용
    private final GameEmbeddingService gameEmbeddingService;

    /* 전체 게임 아이템 목록 조회 */
    @Override
    public List<GameItemResponseDTO> getAll() {
        log.info("전체 게임 아이템 목록 조회 실행");
        List<GameItemResponseDTO> list = gameItemMapper.findAll();
        log.info("조회된 게임 아이템 수: {}", list.size());
        return list;
    }

    /* game_id 기준 게임 아이템 목록 조회 */
    @Override
    public List<GameItemResponseDTO> getByGameId(int gameId) {
        log.debug("GameItemServiceImpl.getByGameId() 실행 - gameId: {}", gameId);
        List<GameItemResponseDTO> list = gameItemMapper.findByGameId(gameId);
        log.debug("조회된 게임 아이템 수 (gameId={}): {}", gameId, list.size());
        return list;
    }

    /* status 기준 게임 아이템 목록 조회 */
    @Override
    public List<GameItemResponseDTO> getByStatus(GameItemStatus gameItemStatus) {
        log.info("상태 기준 게임 아이템 목록 조회 실행: {}", gameItemStatus);
        List<GameItemResponseDTO> list = gameItemMapper.findByStatus(gameItemStatus);
        log.info("조회된 게임 아이템 수 (status={}): {}", gameItemStatus, list.size());
        return list;
    }

    /* PK로 게임 아이템 단건 조회 */
    @Override
    public GameItemResponseDTO getById(int id) {
        log.info("게임 아이템 단건 조회 실행 - id: {}", id);
        return gameItemMapper.findById(id)
                .orElseThrow(() -> {
                    log.warn("게임 아이템 없음 - id: {}", id);
                    return new NoSuchElementException("게임 아이템을 찾을 수 없습니다. id=" + id);
                });
    }

    /* 게임 아이템 등록 */
    @Override
    public void register(GameItemRequestDTO gameItemRequestDTO) {
        log.info("게임 아이템 등록 실행 - gameItemRequestDTO: {}", gameItemRequestDTO);

        GameItem gameItem = modelMapper.map(gameItemRequestDTO, GameItem.class);
        int result = gameItemMapper.insert(gameItem);

        // 아이템 등록 후 해당 게임의 검색 대상 조건을 다시 확인함
        tryUpsertEmbeddingByGameId(gameItemRequestDTO.getGameId());
        log.debug("게임 아이템 등록 결과 - affected rows: {}, generated id: {}", result, gameItem.getId());
    }

    /* 게임 아이템 수정 (존재 여부 선확인) */
    @Override
    public void modify(int id, GameItemRequestDTO gameItemRequestDTO) {
        log.info("게임 아이템 수정 실행 - id: {}, dto: {}", id, gameItemRequestDTO);

        // 임베딩 갱신 여부를 판단할 수 있도록 수정 전 상태를 조회함
        GameItemResponseDTO existing = gameItemMapper.findById(id)
                .orElseThrow(() -> {
                    log.warn("수정 대상 게임 아이템 없음 - id: {}", id);
                    return new NoSuchElementException("게임 아이템을 찾을 수 없습니다. id=" + id);
                });

        GameItem gameItem = GameItem.builder()
                .id(id)
                .gameId(gameItemRequestDTO.getGameId())
                .serialNumber(gameItemRequestDTO.getSerialNumber())
                .status(gameItemRequestDTO.getStatus())
                .build();

        int result = gameItemMapper.update(gameItem);
        log.debug("게임 아이템 수정 결과 - affected rows: {}", result);

        GameItemStatus newStatus = gameItemRequestDTO.getStatus();
        GameItemStatus oldStatus = existing.getStatus();

        // changeStatus()와 같은 기준으로 임베딩 갱신 여부를 판단함
        boolean affectsStock = (newStatus == GameItemStatus.DAMAGED)
                || (newStatus == GameItemStatus.LOST)
                || (newStatus == GameItemStatus.NORMAL
                && (oldStatus == GameItemStatus.DAMAGED
                || oldStatus == GameItemStatus.LOST));

        if (affectsStock) {
            log.debug("modify() 상태 변경으로 임베딩 갱신 - id: {}, {} → {}", id, oldStatus, newStatus);
            tryUpsertEmbeddingByGameId(gameItemRequestDTO.getGameId());
        }
    }

    /* 게임 아이템 삭제 (존재 여부 선확인) */
    // 대여 가능(NORMAL) 혹은 대여 중(RENTED)인 아이템은 삭제할 수 없음
    @Override
    public void remove(int id) {
        log.debug("GameItemServiceImpl.remove() 실행 - id: {}", id);

        GameItemResponseDTO gameItem = gameItemMapper.findById(id)
                .orElseThrow(() -> {
                    log.warn("삭제 대상 게임 아이템 없음 - id: {}", id);
                    return new NoSuchElementException("게임 아이템을 찾을 수 없습니다. id=" + id);
                });

        if (gameItem.getStatus() == GameItemStatus.NORMAL || gameItem.getStatus() == GameItemStatus.RENTED) {
            String message = (gameItem.getStatus() == GameItemStatus.NORMAL)
                    ? "대여 가능 상태(NORMAL) 아이템은 삭제할 수 없습니다."
                    : "대여 중 상태(RENTED) 아이템은 삭제할 수 없습니다.";
            log.warn("게임 아이템 삭제 정책 위반 - id: {}, status: {}", id, gameItem.getStatus());
            throw new IllegalStateException(message);
        }

        // 삭제 전에 gameId 확보
        int gameId = gameItem.getGameId();
        int result = gameItemMapper.delete(id);

        // 아이템 삭제 후 해당 게임의 검색 대상 조건을 다시 확인함
        tryUpsertEmbeddingByGameId(gameId);
        log.debug("게임 아이템 삭제 결과 - affected rows: {}", result);
    }

    /* 게임 아이템 상태 변경 (존재 여부 선확인) */
    @Override
    public void changeStatus(int id, GameItemStatus status) {
        log.debug("GameItemServiceImpl.changeStatus() 실행 - id: {}, status: {}", id, status);
        GameItemResponseDTO gameItem = gameItemMapper.findById(id)
                .orElseThrow(() -> new NoSuchElementException("게임 아이템을 찾을 수 없습니다. id=" + id));

        gameItemMapper.updateStatus(id, status);

        // 저장할 상태가 DAMAGED 또는 LOST이거나, 해당 상태에서 NORMAL로 복구할 때 임베딩 갱신을 시도함
        boolean affectsStock = (status == GameItemStatus.DAMAGED)
                || (status == GameItemStatus.LOST)
                || (status == GameItemStatus.NORMAL
                && (gameItem.getStatus() == GameItemStatus.DAMAGED
                || gameItem.getStatus() == GameItemStatus.LOST));

        if (affectsStock) {
            tryUpsertEmbeddingByGameId(gameItem.getGameId());
        }

        log.debug("게임 아이템 상태 변경 완료 - id: {}, status: {}", id, status);
    }

    // 게임 ID로 연결된 메뉴를 찾아 검색 문서 갱신을 시도함
    private void tryUpsertEmbeddingByGameId(int gameId) {
        try {
            Integer menuId = menuMapper.findMenuIdByGameId(gameId);
            if (menuId != null) {
                gameEmbeddingService.upsertGameByMenuId(menuId);
            } else {
                log.warn("[임베딩] menu_id 없음 - gameId={}", gameId);
            }
        } catch (Exception e) {
            log.error("[임베딩] upsert 실패 - gameId={}, 원인={}", gameId, e.getMessage());
        }
    }

    @Override
    public List<GameItemResponseDTO> getAvailableByGameName(String gameName) {
        String normalized = (gameName == null) ? "" : gameName.trim();
        if (normalized.isEmpty()) {
            return List.of();
        }

        return gameItemMapper.findAll().stream()
                .filter(item -> normalized.equals(item.getGameName()))
                .filter(item -> item.getStatus() == GameItemStatus.NORMAL)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignGameItemsToOrder(int tableId, int orderId, String gameName, List<Integer> gameItemIds) {
        String normalized = (gameName == null) ? "" : gameName.trim();
        if (tableId <= 0 || orderId <= 0 || normalized.isEmpty() || gameItemIds == null || gameItemIds.isEmpty()) {
            throw new IllegalArgumentException("필수 파라미터가 누락되었습니다.");
        }

        Long sessionId = resolveSessionId(tableId);

        int orderMatch = gameItemMapper.countOrderItemInSession(orderId, sessionId, normalized);
        if (orderMatch <= 0) {
            throw new IllegalArgumentException("현재 테이블 세션의 주문 항목과 일치하지 않습니다.");
        }

        for (Integer itemId : gameItemIds) {
            if (itemId == null || itemId <= 0) continue;

            GameItemResponseDTO item = getById(itemId);
            if (!normalized.equals(item.getGameName())) {
                throw new IllegalArgumentException("선택한 시리얼이 주문 게임과 일치하지 않습니다. id=" + itemId);
            }
            if (item.getStatus() != GameItemStatus.NORMAL) {
                throw new IllegalStateException("대여 가능한 상태가 아닙니다. id=" + itemId);
            }

            changeStatus(itemId, GameItemStatus.RENTED);
            gameItemMapper.insertGameHistory(sessionId, itemId);
        }

        // 게임 주문은 일련번호가 배정되면 주문 확인 단계로 전환한다.
        var updateResult = orderService.updateStatus(orderId, "CONFIRMED");
        if (!updateResult.isSuccess()) {
            log.warn("게임 일련번호 배정 후 주문 상태 변경 실패 - orderId: {}, reason: {}",
                    orderId, updateResult.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getActiveGameRentalsByTable(int tableId) {
        Long sessionId = resolveSessionId(tableId);
        return gameItemMapper.findActiveGameRentalsBySessionId(sessionId);
    }

    @Override
    public List<Map<String, Object>> getGameRentalHistoryByTable(int tableId) {
        Long sessionId = resolveSessionId(tableId);
        return gameItemMapper.findGameRentalHistoryBySessionId(sessionId);
    }

    @Override
    @Transactional
    public void settleGameRentals(int tableId, List<Map<String, Object>> updates) {
        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("반납 처리할 데이터가 없습니다.");
        }
        Long sessionId = resolveSessionId(tableId);

        for (Map<String, Object> update : updates) {
            long historyId = Long.parseLong(String.valueOf(update.get("historyId")));
            int gameItemId = Integer.parseInt(String.valueOf(update.get("gameItemId")));
            String statusText = String.valueOf(update.get("status")).trim().toUpperCase();

            if (!List.of("NORMAL", "DAMAGED", "LOST").contains(statusText)) {
                throw new IllegalArgumentException("지원하지 않는 반납 상태입니다: " + statusText);
            }

            Map<String, Object> historyRow = gameItemMapper.findGameHistoryById(historyId);

            if (historyRow == null) {
                throw new NoSuchElementException("game_history가 존재하지 않습니다. id=" + historyId);
            }

            long rowSessionId = ((Number) historyRow.get("sessionId")).longValue();
            int rowGameItemId = ((Number) historyRow.get("gameItemId")).intValue();
            String rowStatus = String.valueOf(historyRow.get("status"));

            if (rowSessionId != sessionId || rowGameItemId != gameItemId) {
                throw new IllegalArgumentException("현재 테이블 세션과 일치하지 않는 대여 이력입니다. historyId=" + historyId);
            }
            if (!"RENTED".equalsIgnoreCase(rowStatus)) {
                throw new IllegalStateException("이미 반납 완료된 이력입니다. historyId=" + historyId);
            }

            GameItemStatus itemStatus = switch (statusText) {
                case "NORMAL" -> GameItemStatus.NORMAL;
                case "DAMAGED" -> GameItemStatus.DAMAGED;
                case "LOST" -> GameItemStatus.LOST;
                default -> throw new IllegalArgumentException("지원하지 않는 상태: " + statusText);
            };

            changeStatus(gameItemId, itemStatus);
            gameItemMapper.updateGameHistoryStatus(historyId, statusText);
        }
    }

    private Long resolveSessionId(int tableId) {
        Long sessionId = cafeTableMapper.selectCurrentSessionId(tableId);
        if (sessionId == null) {
            sessionId = cafeTableMapper.selectActiveSessionByTableId(tableId);
        }
        if (sessionId == null) {
            sessionId = cafeTableMapper.selectLatestSessionByTableId(tableId);
        }
        if (sessionId == null) {
            throw new IllegalStateException("활성 세션을 찾을 수 없습니다. tableId=" + tableId);
        }
        return sessionId;
    }
}
