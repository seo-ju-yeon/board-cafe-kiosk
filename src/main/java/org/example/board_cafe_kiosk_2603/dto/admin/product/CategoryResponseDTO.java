package org.example.board_cafe_kiosk_2603.dto.admin.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.board_cafe_kiosk_2603.domain.admin.product.CategoryType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponseDTO {
    /* 카테고리 조회 응답 DTO */
    private int id;
    private String name;
    private CategoryType type;  // Enum

    /* 연결된 상품(product) 수 - DB LEFT JOIN COUNT 결과 */
    // 목록 화면에서 연결 상품 수 표시 및 삭제 가능 여부 판단에 사용
    // HTML에서 th:disabled="${cat.productCount > 0}" 조건과 연동
    private int productCount;
}
