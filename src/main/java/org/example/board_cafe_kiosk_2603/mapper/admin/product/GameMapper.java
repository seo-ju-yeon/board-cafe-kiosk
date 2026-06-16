package org.example.board_cafe_kiosk_2603.mapper.admin.product;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.board_cafe_kiosk_2603.domain.admin.product.Game;
import org.example.board_cafe_kiosk_2603.dto.admin.product.GameResponseDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;

import java.util.List;
import java.util.Optional;

/**
 * game 테이블 CRUD MyBatis Mapper 인터페이스
 */
@Mapper
public interface GameMapper {

    /** 전체 게임 목록 조회 (category JOIN, game_item COUNT 포함) */
    List<GameResponseDTO> findAll();

    /** category_id 기준 게임 목록 조회 */
    List<GameResponseDTO> findByCategoryId(int categoryId);

    /** 활성 여부 기준 게임 목록 조회 */
    List<GameResponseDTO> findByIsActive(boolean isActive);

    /** PK로 게임 단건 조회 (category JOIN, game_item COUNT 포함) */
    Optional<GameResponseDTO> findById(int id);

    /** 게임명 리스트로 게임 상세 조회 */
    List<GameResponseDTO> findByNames(@Param("names") List<String> names);

    /** 게임 등록 */
    int insert(Game game);

    /** 게임 수정 */
    int update(Game game);

    /** 게임 삭제 */
    int delete(int id);

    /** 게임 활성 상태 토글 (is_active 반전) */
    int toggleActive(int id);

    /*==============페이지=============*/
    /**전체 게임 목록 조회 - 페이징 */
    List<GameResponseDTO> findAllPaged(PageRequestDTO pageRequestDTO);

    /** 전체 게임 수 */
    int countAll();

    /** category_id 기준 게임 목록 조회 - 페이징 */
    List<GameResponseDTO> findByCategoryIdPaged(@Param("categoryId") int categoryId,
                                                @Param("pageRequestDTO") PageRequestDTO pageRequestDTO);

    /** 활성 여부 기준 게임 목록 조회 - 페이징 */
    List<GameResponseDTO> findByIsActivePaged(@Param("isActive") boolean isActive,
                                              @Param("categoryId") Integer categoryId,
                                              @Param("pageRequestDTO") PageRequestDTO pageRequestDTO);

    /** 활성 여부 기준 게임 수 */
    int countByIsActive(@Param("isActive") boolean isActive,
                        @Param("categoryId") Integer categoryId);


    /** category_id 기준 게임 수 */
    int countByCategoryId(int categoryId);

}
