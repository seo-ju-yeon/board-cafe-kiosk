package org.example.board_cafe_kiosk_2603.service.admin.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.ai.GameEmbeddingService;
import org.example.board_cafe_kiosk_2603.domain.admin.product.Game;
import org.example.board_cafe_kiosk_2603.dto.admin.product.GameRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.product.GameResponseDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageResponseDTO;
import org.example.board_cafe_kiosk_2603.mapper.admin.product.GameMapper;
import org.example.board_cafe_kiosk_2603.mapper.admin.product.MenuMapper;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Log4j2
@Service
@RequiredArgsConstructor
class GameServiceImpl implements GameService {

    private final GameMapper gameMapper;
    private final MenuMapper menuMapper;
    private final GameEmbeddingService gameEmbeddingService;

    /* 전체 게임 목록 조회 */
    @Override
    public List<GameResponseDTO> getAll() {
        log.debug("GameServiceImpl.getAll() 실행");
        List<GameResponseDTO> list = gameMapper.findAll();
        log.debug("조회된 게임 수: {}", list.size());
        return list;
    }

    /* category_id 기준 게임 목록 조회 */
    @Override
    public List<GameResponseDTO> getByCategoryId(int categoryId) {
        log.debug("GameServiceImpl.getByCategoryId() 실행 - categoryId: {}", categoryId);
        List<GameResponseDTO> list = gameMapper.findByCategoryId(categoryId);
        log.debug("조회된 게임 수 (categoryId={}): {}", categoryId, list.size());
        return list;
    }

    /* 활성 여부 기준 게임 목록 조회 */
    @Override
    public List<GameResponseDTO> getByIsActive(boolean isActive) {
        log.debug("GameServiceImpl.getByIsActive() 실행 - isActive: {}", isActive);
        List<GameResponseDTO> list = gameMapper.findByIsActive(isActive);
        log.debug("조회된 게임 수 (isActive={}): {}", isActive, list.size());
        return list;
    }

    /* PK로 게임 단건 조회 */
    @Override
    public GameResponseDTO getById(int id) {
        log.debug("GameServiceImpl.getById() 실행 - id: {}", id);
        return gameMapper.findById(id)
                .orElseThrow(() -> {
                    log.warn("게임 없음 - id: {}", id);
                    return new NoSuchElementException("게임을 찾을 수 없습니다. id=" + id);
                });
    }

    @Override
    public List<GameResponseDTO> getByNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        return gameMapper.findByNames(names);
    }

    /* 게임 등록 */
    @Override
    public int register(GameRequestDTO gameRequestDTO) {
        log.debug("GameServiceImpl.register() 실행 - dto: {}", gameRequestDTO);
        Game game = Game.builder()
                .categoryId(gameRequestDTO.getCategoryId())
                .name(gameRequestDTO.getName())
                .minPlayers(gameRequestDTO.getMinPlayers())
                .maxPlayers(gameRequestDTO.getMaxPlayers())
                .playTime(gameRequestDTO.getPlayTime())
                .isActive(gameRequestDTO.isActive())
                .imageUrl(gameRequestDTO.getImageUrl())
                .build();
        gameMapper.insert(game);

        // 키오스크의 0원 게임 메뉴가 없으면 생성함
        menuMapper.insertGameMenuIfNotExists(
                gameRequestDTO.getCategoryId(),
                gameRequestDTO.getName(),
                gameRequestDTO.getDescription()
        );

        menuMapper.updateGameMenuDescriptionByName(
                gameRequestDTO.getName(),
                gameRequestDTO.getDescription()
        );
        // 게임과 메뉴 정보를 저장한 뒤 검색 대상 조건을 다시 확인함
        tryUpsertEmbeddingByGameName(gameRequestDTO.getName());

        log.info("게임 등록 완료 - generated id: {}", game.getId());
        return game.getId();  // insert 후 game.getId() 반환
    }

    /* 게임 수정 (존재 여부 선확인) */
    @Override
    public void modify(int id, GameRequestDTO gameRequestDTO) {
        log.debug("GameServiceImpl.modify() 실행 - id: {}, dto: {}", id, gameRequestDTO);

        // 수정 전 원본 데이터 확보
        GameResponseDTO origin = gameMapper.findById(id)
                .orElseThrow(() -> {
                    log.warn("수정 대상 게임 없음 - id: {}", id);
                    return new NoSuchElementException("게임을 찾을 수 없습니다. id=" + id);
                });
        Game game = Game.builder()
                .id(id)
                .categoryId(gameRequestDTO.getCategoryId())
                .name(gameRequestDTO.getName())
                .minPlayers(gameRequestDTO.getMinPlayers())
                .maxPlayers(gameRequestDTO.getMaxPlayers())
                .playTime(gameRequestDTO.getPlayTime())
                .isActive(gameRequestDTO.isActive())
                .imageUrl(gameRequestDTO.getImageUrl())
                .build();
        int result = gameMapper.update(game);

        // 이름 변경 시 메뉴의 이름도 함께 수정함
        if (origin.getName() != null && gameRequestDTO.getName() != null
                && !origin.getName().equals(gameRequestDTO.getName())) {
            menuMapper.renameGameMenuName(origin.getName(), gameRequestDTO.getName());
        }
        menuMapper.insertGameMenuIfNotExists(
                gameRequestDTO.getCategoryId(),
                gameRequestDTO.getName(),
                gameRequestDTO.getDescription()
        );
        menuMapper.updateGameMenuDescriptionByName(
                gameRequestDTO.getName(),
                gameRequestDTO.getDescription()
        );
        // 게임과 메뉴 정보를 수정한 뒤 검색 대상 조건을 다시 확인함
        tryUpsertEmbeddingByGameName(gameRequestDTO.getName());
        log.debug("게임 수정 결과 - affected rows: {}", result);
    }

    /* 게임 삭제 (game_item ON DELETE CASCADE 로 자동 삭제, 존재 여부 선확인) */
    @Override
    public void remove(int id) {

        log.info("GameServiceImpl.remove() 실행 - id: {}", id);

        GameResponseDTO game = gameMapper.findById(id)
                .orElseThrow(() -> {
                    log.warn("삭제 대상 게임 없음 - id: {}", id);
                    return new NoSuchElementException("게임을 찾을 수 없습니다. id=" + id);
                });

        // 게임 삭제 후 검색 문서를 삭제할 수 있도록 연결된 메뉴 ID를 먼저 확보함
        Integer menuId = menuMapper.findMenuIdByGameName(game.getName());
        gameMapper.delete(id);  // DB 삭제 (Cascade 설정에 의해 game_item도 함께 삭제)

        if (menuId != null) {
            tryDeleteEmbeddingByMenuId(menuId);
        }
        log.debug("게임 삭제 결과 - affected rows: {}", id);
    }

    /* 게임 활성 상태 토글 (is_active 반전, 존재 여부 선확인) */
    @Override
    public void toggleActive(int id) {
        log.info("GameServiceImpl.toggleActive() 실행 - id: {}", id);
        GameResponseDTO game = gameMapper.findById(id)
                .orElseThrow(() -> {
                    log.warn("토글 대상 게임 없음 - id: {}", id);
                    return new NoSuchElementException("게임을 찾을 수 없습니다. id=" + id);
                });

        int result = gameMapper.toggleActive(id);

        // 게임 활성 상태를 변경한 뒤 검색 대상 조건을 다시 확인함
        tryUpsertEmbeddingByGameName(game.getName());
        log.info("게임 활성 상태 토글 결과 - affected rows: {}", result);
    }

    /* 헬퍼: game.name → menu_id → 임베딩 upsert */
    // 예외 발생 시 로그만 남기고 비즈니스 로직 중단하지 않음

    /* 게임명을 통한 AI 지식 업서트(추가/수정) 트리거 */
    private void tryUpsertEmbeddingByGameName(String gameName) {
        try {
            Integer menuId = menuMapper.findMenuIdByGameName(gameName);
            if (menuId != null) {
                gameEmbeddingService.upsertGameByMenuId(menuId);
            } else {
                log.warn("[임베딩] menu_id 없음 - gameName={}", gameName);
            }
        } catch (Exception e) {
            log.error("[임베딩] upsert 실패 - gameName={}, 원인={}", gameName, e.getMessage());
        }
    }

    /* 메뉴 ID를 통한 AI 지식 삭제 트리거 */
    private void tryDeleteEmbeddingByMenuId(Integer menuId) {
        try {
            gameEmbeddingService.deleteByMenuId(menuId);
        } catch (Exception e) {
            log.error("[임베딩] delete 실패 - menuId={}, 원인={}", menuId, e.getMessage());
        }
    }

    /*===========페이지=========== */
    /* 전체 게임 목록 조회 - 페이징 */
    @Override
    public PageResponseDTO<GameResponseDTO> getAll(PageRequestDTO pageRequestDTO) {
        log.debug("GameServiceImpl.getAll(paged) 실행");
        List<GameResponseDTO> list = gameMapper.findAllPaged(pageRequestDTO);
        int total = gameMapper.countAll();
        log.debug("조회된 게임 수: {}, 전체: {}", list.size(), total);
        return new PageResponseDTO<>(pageRequestDTO, total, list);
    }

    /* category_id 기준 게임 목록 조회 - 페이징 */
    @Override
    public PageResponseDTO<GameResponseDTO> getByCategoryId(int categoryId, PageRequestDTO pageRequestDTO) {
        log.debug("GameServiceImpl.getByCategoryId(paged) 실행 - categoryId: {}", categoryId);
        List<GameResponseDTO> list = gameMapper.findByCategoryIdPaged(categoryId, pageRequestDTO);
        int total = gameMapper.countByCategoryId(categoryId);
        log.debug("조회된 게임 수 (categoryId={}): {}, 전체: {}", categoryId, list.size(), total);
        return PageResponseDTO.<GameResponseDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(list)
                .total(total)
                .build();
    }

    /* 활성 여부 기준 게임 목록 조회 - 페이징 */
    @Override
    public PageResponseDTO<GameResponseDTO> getByIsActive(boolean isActive, Integer categoryId, PageRequestDTO pageRequestDTO) {
        List<GameResponseDTO> list = gameMapper.findByIsActivePaged(isActive, categoryId, pageRequestDTO);
        int total = gameMapper.countByIsActive(isActive, categoryId);
        return PageResponseDTO.<GameResponseDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(list)
                .total(total)
                .build();
    }
}
