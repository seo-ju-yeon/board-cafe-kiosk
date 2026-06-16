package org.example.board_cafe_kiosk_2603.mapper.admin.product;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.board_cafe_kiosk_2603.domain.admin.product.Category;
import org.example.board_cafe_kiosk_2603.domain.admin.product.CategoryType;
import org.example.board_cafe_kiosk_2603.dto.admin.product.CategoryRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.product.CategoryResponseDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;

import java.util.List;
import java.util.Optional;

/**
 * category 테이블 CRUD MyBatis Mapper 인터페이스
 */
@Mapper
public interface CategoryMapper {

    /* SELECT → ResponseDTO 직접 매핑 (JOIN 집계 포함) */
    // ──────────────────────────────────────────────
    // SELECT
    // ──────────────────────────────────────────────

    /* 카테고리 전체 목록 조회 (연결 상품 수 포함) */
    List<CategoryResponseDTO> findAll();

    /* type 기준 카테고리 목록 조회 (연결 상품 수 포함) */
    // DRINK / FOOD / GAME / GUEST
    List<CategoryResponseDTO> findByType(CategoryType type);

    /* PK로 카테고리 단건 조회 (연결 상품 수 포함) */
    Optional<CategoryResponseDTO> findById(int id);

    /* INSERT/UPDATE/DELETE → Category VO 파라미터 유지 */
    // ──────────────────────────────────────────────
    // INSERT  — VO 파라미터 유지 (generated key → VO.id 역주입)
    // Service 에서 RequestDTO → VO 변환 후 호출
    // ──────────────────────────────────────────────

    /* 카테고리 등록 */
    // insert 후 generated PK → category.id 반영
    int insert(Category category);

    // ──────────────────────────────────────────────
    // UPDATE  — RequestDTO + id 분리 파라미터
    // ──────────────────────────────────────────────

    /**
     * 카테고리 수정
     * @param id  수정 대상 PK
     * @param dto name / type 변경 값
     */
    int update(@Param("id") int id, @Param("dto") CategoryRequestDTO dto);

    /* 카테고리 삭제 */
    int delete(int id);

    /* 해당 카테고리에 연결된 상품(product) 수 조회 */
     // 삭제 가능 여부 검증에 활용
     // category_id FK 기준 COUNT 쿼리
    int countLinkedProducts(int id);

    /** 전체 카테고리 목록 조회 - 페이징 */
    List<CategoryResponseDTO> findAllPaged(PageRequestDTO pageRequestDTO);

    /** 전체 카테고리 수 */
    int countAll();
}
