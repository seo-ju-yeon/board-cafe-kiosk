package org.example.board_cafe_kiosk_2603.service.admin.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.ai.GameEmbeddingService;
import org.example.board_cafe_kiosk_2603.domain.admin.product.Menu;
import org.example.board_cafe_kiosk_2603.dto.admin.product.MenuRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.product.MenuResponseDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageResponseDTO;
import org.example.board_cafe_kiosk_2603.mapper.admin.product.MenuMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Log4j2
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuMapper menuMapper;
    private final GameEmbeddingService gameEmbeddingService;

    /* 전체 메뉴 목록 조회 (소프트 삭제 제외) */
    @Override
    public List<MenuResponseDTO> getAll() {
        log.debug("MenuServiceImpl.getAll() 실행");
        List<MenuResponseDTO> list = menuMapper.findAll();
        log.debug("조회된 메뉴 수: {}", list.size());
        return list;
    }

    /* category_id 기준 메뉴 목록 조회 */
    @Override
    public List<MenuResponseDTO> getByCategoryId(int categoryId) {
        log.debug("MenuServiceImpl.getByCategoryId() 실행 - categoryId: {}", categoryId);
        List<MenuResponseDTO> list = menuMapper.findByCategoryId(categoryId);
        log.debug("조회된 메뉴 수 (categoryId={}): {}", categoryId, list.size());
        return list;
    }

    /* category type 기준 메뉴 목록 조회 (FOOD / DRINK / GUEST) */
    @Override
    public List<MenuResponseDTO> getByType(String type) {
        log.debug("MenuServiceImpl.getByType() 실행 - type: {}", type);
        List<MenuResponseDTO> list = menuMapper.findByType(type);
        log.debug("조회된 메뉴 수 (type={}): {}", type, list.size());
        return list;
    }


    /* 소프트 삭제 여부 기준 메뉴 목록 조회 (숨김 탭용) */
    @Override
    public List<MenuResponseDTO> getByIsDeleted(boolean isDeleted) {
        log.debug("MenuServiceImpl.getByIsDeleted() 실행 - isDeleted: {}", isDeleted);
        List<MenuResponseDTO> list = menuMapper.findByIsDeleted(isDeleted);
        log.debug("조회된 메뉴 수 (isDeleted={}): {}", isDeleted, list.size());
        return list;
    }

    /* 판매 가능 여부 기준 메뉴 목록 조회 */
    @Override
    public List<MenuResponseDTO> getByIsAvailable(boolean isAvailable) {
        log.debug("MenuServiceImpl.getByIsAvailable() 실행 - isAvailable: {}", isAvailable);
        List<MenuResponseDTO> list = menuMapper.findByIsAvailable(isAvailable);
        log.debug("조회된 메뉴 수 (isAvailable={}): {}", isAvailable, list.size());
        return list;
    }

    /* PK로 메뉴 단건 조회 */
    @Override
    public MenuResponseDTO getById(int id) {
        log.debug("MenuServiceImpl.getById() 실행 - id: {}", id);
        return menuMapper.findByIdIncludeDeleted(id)
                .orElseThrow(() -> {
                    log.warn("메뉴 없음 - id: {}", id);
                    return new NoSuchElementException("메뉴를 찾을 수 없습니다. id=" + id);
                });
    }

    /* 메뉴 등록 */
    // GAME 타입은 GameServiceImpl.register()에서 처리하므로 여기선 임베딩 불필요
    @Override
    public void register(MenuRequestDTO dto) {
        log.debug("MenuServiceImpl.register() 실행 - dto: {}", dto);
        Menu menu = Menu.builder()
                .categoryId(dto.getCategoryId())
                .name(dto.getName())
                .price(dto.getPrice())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .isAvailable(dto.isAvailable())
                .build();
        int result = menuMapper.insert(menu);
        log.debug("메뉴 등록 결과 - affected rows: {}, generated id: {}", result, menu.getId());
    }

    /* 메뉴 수정 */
    // GAME 타입 메뉴(categoryName으로 판단)의 description 변경 시 임베딩 갱신
    @Override
    public void modify(int id, MenuRequestDTO dto) {
        log.debug("MenuServiceImpl.modify() 실행 - id: {}, dto: {}", id, dto);

        // 1. 수정 전 기존 메뉴 조회 (GAME 타입 여부 확인용)
        MenuResponseDTO origin = menuMapper.findByIdIncludeDeleted(id)
                .orElseThrow(() -> new NoSuchElementException("메뉴를 찾을 수 없습니다. id=" + id));
        Menu menu = Menu.builder()
                .id(id)
                .categoryId(dto.getCategoryId())
                .name(dto.getName())
                .price(dto.getPrice())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .isAvailable(dto.isAvailable())
                .build();
        int result = menuMapper.update(menu);

        // GAME 타입 메뉴인 경우에만 임베딩 갱신
        if (isGameCategory(origin.getCategoryType())) {
            tryUpsertEmbeddingByMenuId(id);
        }
        log.info("메뉴 수정 결과 - affected rows: {}", result);
    }

    /* 메뉴 소프트 삭제 (is_deleted = true) */
    // GAME 타입 메뉴 삭제 시 임베딩도 삭제
    // AI가 삭제된 게임을 안내하지 않음
    @Override
    public void remove(int id) {
        log.debug("MenuServiceImpl.remove() 실행 - id: {}", id);

        MenuResponseDTO menu = menuMapper.findByIdIncludeDeleted(id)
                .orElseThrow(() -> new NoSuchElementException("메뉴를 찾을 수 없습니다. id=" + id));

        menuMapper.softDelete(id);

        // GAME 타입 메뉴인 경우 임베딩 삭제
        if (isGameCategory(menu.getCategoryType())) {
            tryDeleteEmbeddingByMenuId(id);
        }
        log.debug("메뉴 소프트 삭제 결과 - affected rows: {}", id);
    }

    /* 메뉴 복원 (is_deleted = false) */
    // GAME 타입 메뉴 복원 시 임베딩 재등록 시도
    // 다른 조건(재고, is_active) 충족 시에만 실제 등록됨
    @Override
    public void restore(int id) {
        log.debug("MenuServiceImpl.restore() 실행 - id: {}", id);
        MenuResponseDTO menu = menuMapper.findByIdIncludeDeleted(id)
                .orElseThrow(() -> new NoSuchElementException("메뉴를 찾을 수 없습니다. id=" + id));

        menuMapper.restore(id);

        // GAME 타입 메뉴인 경우 임베딩 재등록 시도
        if (isGameCategory(menu.getCategoryType())) {
            tryUpsertEmbeddingByMenuId(id);
        }
        log.debug("메뉴 복원 결과 - affected rows: {}", id);
    }

    /* 메뉴 판매 상태 토글 */
    // GAME 타입 메뉴 판매 중지 시 임베딩 삭제 & 판매 재개 시 임베딩 등록
    // 판매 중지 상태가 되면 AI 안내에서도 자동으로 제외
    @Override
    public void toggleAvailable(int id) {
        log.debug("MenuServiceImpl.toggleAvailable() 실행 - id: {}", id);

        MenuResponseDTO menu = menuMapper.findByIdIncludeDeleted(id)
                .orElseThrow(() -> new NoSuchElementException("메뉴를 찾을 수 없습니다. id=" + id));

        menuMapper.toggleAvailable(id);

        // GAME 타입 메뉴인 경우 임베딩 상태 재확인
        if (isGameCategory(menu.getCategoryType())) {
            tryUpsertEmbeddingByMenuId(id);
        }
        log.debug("메뉴 판매 상태 토글 결과 - affected rows: {}", id);
    }

    /* 카테고리 타입 판별 (GAME 여부) */
    // MenuResponseDTO에 포함된 categoryType 필드를 기준으로 판단합니다.
    private boolean isGameCategory(String categoryType) {
        return "GAME".equalsIgnoreCase(categoryType);
    }

    /* AI 임베딩 갱신 트리거 (예외 격리) */
    private void tryUpsertEmbeddingByMenuId(int menuId) {
        try {
            gameEmbeddingService.upsertGameByMenuId(menuId);
        } catch (Exception e) {
            log.error("[임베딩] upsert 실패 - menuId={}, 원인={}", menuId, e.getMessage());
        }
    }

    /* AI 임베딩 삭제 트리거 (예외 격리) */
    private void tryDeleteEmbeddingByMenuId(int menuId) {
        try {
            gameEmbeddingService.deleteByMenuId(menuId);
        } catch (Exception e) {
            log.error("[임베딩] delete 실패 - menuId={}, 원인={}", menuId, e.getMessage());
        }
    }

    /*=====페이지=======*/
    /* category type 기준 메뉴 목록 조회 - 페이징 */
    @Override
    public PageResponseDTO<MenuResponseDTO> getByType(String type, PageRequestDTO pageRequestDTO) {
        log.debug("MenuServiceImpl.getByType(paged) 실행 - type: {}", type);
        List<MenuResponseDTO> list = menuMapper.findByTypePaged(type, pageRequestDTO);
        int total = menuMapper.countByType(type);
        log.debug("조회된 메뉴 수 (type={}): {}, 전체: {}", type, list.size(), total);
        return new PageResponseDTO<>(pageRequestDTO, total, list);
    }

    /* category type + category_id 기준 메뉴 목록 조회 - 페이징 */
    @Override
    public PageResponseDTO<MenuResponseDTO> getByTypeAndCategoryId(String type, int categoryId, PageRequestDTO pageRequestDTO) {
        log.debug("MenuServiceImpl.getByTypeAndCategoryId(paged) 실행 - type: {}, categoryId: {}", type, categoryId);
        List<MenuResponseDTO> list = menuMapper.findByTypeAndCategoryIdPaged(type, categoryId, pageRequestDTO);
        int total = menuMapper.countByTypeAndCategoryId(type, categoryId);
        log.debug("조회된 메뉴 수 (type={}, categoryId={}): {}, 전체: {}", type, categoryId, list.size(), total);
        return new PageResponseDTO<>(pageRequestDTO, total, list);
    }

    /* 소프트 삭제 여부 기준 메뉴 목록 조회 - 페이징 (숨김 탭용) */
    @Override
    public PageResponseDTO<MenuResponseDTO> getByIsDeleted(boolean isDeleted, PageRequestDTO pageRequestDTO) {
        log.debug("MenuServiceImpl.getByIsDeleted(paged) 실행 - isDeleted: {}", isDeleted);
        List<MenuResponseDTO> list = menuMapper.findByIsDeletedPaged(isDeleted, pageRequestDTO);
        int total = menuMapper.countByIsDeleted(isDeleted);
        log.debug("조회된 메뉴 수 (isDeleted={}): {}, 전체: {}", isDeleted, list.size(), total);
        return new PageResponseDTO<>(pageRequestDTO, total, list);
    }

    @Override
    public PageResponseDTO<MenuResponseDTO> getByIsDeletedAndCategoryId(boolean isDeleted, int categoryId, PageRequestDTO pageRequestDTO) {
        List<MenuResponseDTO> list = menuMapper.findByIsDeletedAndCategoryIdPaged(isDeleted, categoryId, pageRequestDTO);
        int total = menuMapper.countByIsDeletedAndCategoryId(isDeleted, categoryId);
        return PageResponseDTO.<MenuResponseDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .total(total)
                .dtoList(list)
                .build();
    }
}
