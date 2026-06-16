package org.example.board_cafe_kiosk_2603.dto.admin.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuRequestDTO {

    /*
     메뉴 등록·수정 요청 시 사용하는 DTO
     -> imageUrl 은 파일 업로드 후 저장된 경로를 문자열로 전달
     */

    /* FK → category.id */
    private Integer categoryId;

    private String name;
    private int price;
    private String description;
    private String imageUrl;

    /* 판매 가능 여부 */
    private boolean isAvailable;
}
