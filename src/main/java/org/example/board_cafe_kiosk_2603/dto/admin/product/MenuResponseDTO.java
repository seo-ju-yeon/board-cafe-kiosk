package org.example.board_cafe_kiosk_2603.dto.admin.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuResponseDTO {

    /*
     메뉴 조회 응답 시 사용하는 DTO
     -> categoryName 을 JOIN 하여 함께 반환
     -> isDeleted 는 관리자 응답에만 포함 (소프트 삭제 상태 확인용)
     */

    private int id;
    private Integer categoryId;

    /** JOIN으로 가져온 카테고리명 */
    private String categoryName;

    /** 카테고리 타입 (FOOD, DRINK, GAME 등) - 게임 판별 로직에 사용 */
    private String categoryType;

    private String name;
    private int price;
    private String description;
    private String imageUrl;
    private boolean isAvailable;
    private boolean isDeleted;
    private LocalDateTime createdAt;
}
