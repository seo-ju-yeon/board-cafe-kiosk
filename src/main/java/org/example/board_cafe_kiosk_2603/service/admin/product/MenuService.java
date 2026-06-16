package org.example.board_cafe_kiosk_2603.service.admin.product;

import org.example.board_cafe_kiosk_2603.dto.admin.product.MenuRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.product.MenuResponseDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageResponseDTO;

import java.util.List;

/**
 * Menu 비즈니스 로직 인터페이스
 */
public interface MenuService {
    /** 전체 메뉴 목록 반환 (소프트 삭제 제외) */
    List<MenuResponseDTO> getAll();

    /** category_id 기준 메뉴 목록 반환 */
    List<MenuResponseDTO> getByCategoryId(int categoryId);

    /** 판매 가능 여부 기준 메뉴 목록 반환 */
    List<MenuResponseDTO> getByIsAvailable(boolean isAvailable);

    /** PK로 메뉴 단건 반환 */
    MenuResponseDTO getById(int id);

    /** 메뉴 등록 */
    void register(MenuRequestDTO menuRequestDTO);

    /** 메뉴 수정 */
    void modify(int id, MenuRequestDTO menuRequestDTO);

    /** 메뉴 소프트 삭제 (숨김)*/
    void remove(int id);

    /** 메뉴 복원 (소프트 삭제 해제) */
    void restore(int id);

    /** 메뉴 판매 상태 토글 */
    void toggleAvailable(int id);

    /** category type 기준 메뉴 목록 반환 (FOOD / DRINK / GUEST) */
    List<MenuResponseDTO> getByType(String type);

    /** 소프트 삭제 여부 기준 메뉴 목록 반환 (숨김 탭용) */
    List<MenuResponseDTO> getByIsDeleted(boolean isDeleted);

    /*========페이지=======*/
    /** category type 기준 메뉴 목록 반환 - 페이징 */
    PageResponseDTO<MenuResponseDTO> getByType(String type, PageRequestDTO pageRequestDTO);

    /** category type + category_id 기준 메뉴 목록 반환 - 페이징 */
    PageResponseDTO<MenuResponseDTO> getByTypeAndCategoryId(String type, int categoryId, PageRequestDTO pageRequestDTO);

    /** 소프트 삭제 여부 기준 메뉴 목록 반환 - 페이징 (숨김 탭용) */
    PageResponseDTO<MenuResponseDTO> getByIsDeleted(boolean isDeleted, PageRequestDTO pageRequestDTO);

    /** 소프트 삭제 여부 + category_id 기준 메뉴 목록 반환 - 페이징 */
    PageResponseDTO<MenuResponseDTO> getByIsDeletedAndCategoryId(boolean isDeleted, int categoryId, PageRequestDTO pageRequestDTO);
}
