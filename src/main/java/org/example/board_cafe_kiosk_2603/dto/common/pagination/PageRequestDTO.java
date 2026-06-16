package org.example.board_cafe_kiosk_2603.dto.common.pagination;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageRequestDTO {

    // 현재 페이지 번호 (기본값: 1, 최솟값: 1)
    @Builder.Default
    @Min(value = 1)
    @Positive
    private int page = 1;

    // 한 페이지에 보여줄 데이터 개수 (기본값: 10, 범위: 1~100)
    @Builder.Default
    @Min(value = 1)
    @Max(value = 100)
    @Positive
    private int size = 10;

    // 검색어
    private String keyword;

    // 필터 (예: "all", "active", "inactive")
    private String filter;

    /**
     * MyBatis LIMIT 절에 사용할 시작 위치 계산
     * 예) page=3, size=10 → skip=20
     */
    public int getSkip() {
        return (Math.max(1, page) - 1) * size;
    }

    /**
     * 페이징 링크에 붙을 쿼리 파라미터 문자열 생성
     * 예) size=10&keyword=010
     * page는 HTML에서 직접 붙이므로 여기서는 제외
     */
    public String getLink() {
        StringBuilder builder = new StringBuilder();
        builder.append("size=").append(this.size);

        if (this.keyword != null) {
            builder.append("&keyword=").append(URLEncoder.encode(keyword, StandardCharsets.UTF_8));
        }

        if (this.filter != null && !this.filter.equals("all")) {
            builder.append("&filter=").append(this.filter);
        }

        return builder.toString();
    }
}