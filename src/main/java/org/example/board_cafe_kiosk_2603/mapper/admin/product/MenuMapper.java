package org.example.board_cafe_kiosk_2603.mapper.admin.product;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.board_cafe_kiosk_2603.domain.admin.product.Menu;
import org.example.board_cafe_kiosk_2603.dto.admin.product.MenuResponseDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;

import java.util.List;
import java.util.Optional;

@Mapper
public interface MenuMapper {
    /* 전체 메뉴 목록 조회 (소프트 삭제 제외, category JOIN 포함) */
    List<MenuResponseDTO> findAll();

    /* category_id 기준 메뉴 목록 조회 */
    List<MenuResponseDTO> findByCategoryId(int categoryId);

    /* category type 기준 메뉴 목록 조회 (FOOD / DRINK / GUEST) */
    List<MenuResponseDTO> findByType(String type);

    /* 판매 가능 여부 기준 메뉴 목록 조회 */
    List<MenuResponseDTO> findByIsAvailable(boolean isAvailable);

    /*소프트 삭제 여부 기준 메뉴 목록 조회 (숨김 탭용) */
    List<MenuResponseDTO> findByIsDeleted(boolean isDeleted);

    /* PK로 메뉴 단건 조회 (category JOIN 포함) */
//    Optional<MenuResponseDTO> findById(int id);

    /*PK로 메뉴 단건 조회 (삭제 여부 무관) */
    Optional<MenuResponseDTO> findByIdIncludeDeleted(int id);

    /* 메뉴 등록 */
    int insert(Menu menu);

    /* 메뉴 수정 */
    int update(Menu menu);

    /* 메뉴 소프트 삭제 (is_deleted = true) */
    int softDelete(int id);

    /* 메뉴 복원 (is_deleted = false) */
    int restore(int id);

    /* 메뉴 판매 상태 토글 (is_available 반전) */
    int toggleAvailable(int id);

    /* =======페이지========= */
    /* category type 기준 메뉴 목록 조회 - 페이징 */
    List<MenuResponseDTO> findByTypePaged(@Param("type") String type,
                                          @Param("pageRequestDTO") PageRequestDTO pageRequestDTO);

    /* category type 기준 메뉴 수 */
    int countByType(String type);

    /* category type + category_id 기준 메뉴 목록 조회 - 페이징 */
    List<MenuResponseDTO> findByTypeAndCategoryIdPaged(@Param("type") String type,
                                                       @Param("categoryId") int categoryId,
                                                       @Param("pageRequestDTO") PageRequestDTO pageRequestDTO);

    /* category type + category_id 기준 메뉴 수 */
    int countByTypeAndCategoryId(@Param("type") String type, @Param("categoryId") int categoryId);

    /* 소프트 삭제 여부 기준 메뉴 목록 조회 - 페이징 (숨김 탭용) */
    List<MenuResponseDTO> findByIsDeletedPaged(@Param("isDeleted") boolean isDeleted,
                                               @Param("pageRequestDTO") PageRequestDTO pageRequestDTO);

    /* 소프트 삭제 여부 기준 메뉴 수 */
    int countByIsDeleted(boolean isDeleted);

    /* 소프트 삭제 여부 + category_id 기준 메뉴 목록 조회 - 페이징 */
    List<MenuResponseDTO> findByIsDeletedAndCategoryIdPaged(@Param("isDeleted") boolean isDeleted,
                                                            @Param("categoryId") int categoryId,
                                                            @Param("pageRequestDTO") PageRequestDTO pageRequestDTO);

    /* 소프트 삭제 여부 + category_id 기준 메뉴 수 */
    int countByIsDeletedAndCategoryId(@Param("isDeleted") boolean isDeleted,
                                      @Param("categoryId") int categoryId);

    /* game 등록 시 menu(가격 0) 레코드가 없으면 생성 */
    int insertGameMenuIfNotExists(@Param("categoryId") Integer categoryId,
                                  @Param("name") String name,
                                  @Param("description") String description);

    /* 게임명 기준 menu 설명 업데이트 (price=0 대상) */
    int updateGameMenuDescriptionByName(@Param("name") String name,
                                        @Param("description") String description);

    /* 게임명 변경 시 menu 이름 동기화 (price=0 대상)*/
    int renameGameMenuName(@Param("oldName") String oldName,
                           @Param("newName") String newName);

    /* SpringAI & RAG */

    /* AI 임베딩 연동, 게임 이름을 기반으로 매핑된 메뉴 ID를 조회 (게임 이름으로 menu.id 조회) */
    // GameServiceImpl.tryUpsertByGameName() 에서 사용
    Integer findMenuIdByGameName(String gameName);

    /* AI 임베딩 연동, 게임 ID를 통해 연관된 메뉴 ID를 역추적하여 조회 */
    // GameItemServiceImpl.tryUpsertByGameId() 에서 사용
    // game.id → category_id → menu.id 경로로 menu.id 조회
    Integer findMenuIdByGameId(int gameId);
}
