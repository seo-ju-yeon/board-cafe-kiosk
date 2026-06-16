package org.example.board_cafe_kiosk_2603.service.admin.product;

import org.example.board_cafe_kiosk_2603.domain.admin.product.CategoryType;
import org.example.board_cafe_kiosk_2603.dto.admin.product.CategoryRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.product.CategoryResponseDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageResponseDTO;

import java.util.List;

public interface CategoryService {
    /* 카테고리 전체 목록 반환 (연결 상품 수 포함) */
    List<CategoryResponseDTO> getAll();

    /* type 기준 카테고리 목록 반환 */
    List<CategoryResponseDTO> getByType(CategoryType type);

    /* PK로 카테고리 단건 반환 */
    CategoryResponseDTO getById(int id);

    /* 카테고리 등록 */
    void register(CategoryRequestDTO categoryRequestDTO);

    /* 카테고리 수정 */
    void modify(int id, CategoryRequestDTO categoryRequestDTO);

    /* 카테고리 삭제 */
    // 연결된 상품이 존재할 경우 IllegalStateException 발생
    void remove(int id);

    /* 삭제 가능 여부 확인 */
    // 연결 상품 수가 0이면 true 반환
    // Controller에서 사전 검증 후 클라이언트에 피드백 제공 시 활용
    boolean canDelete(int id);

    /** 전체 카테고리 목록 - 페이징 */
    PageResponseDTO<CategoryResponseDTO> getAll(PageRequestDTO pageRequestDTO);
}
