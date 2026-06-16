package org.example.board_cafe_kiosk_2603.dto.common.pagination;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class PageResponseDTO<E> {

    // 현재 페이지 번호
    private int page;

    // 한 페이지당 데이터 개수
    private int size;

    // 전체 데이터 개수
    private int total;

    // 페이징 버튼의 시작 번호
    private int start;

    // 페이징 버튼의 끝 번호
    private int end;

    // 이전 버튼 표시 여부 (start > 1 이면 true)
    private boolean prev;

    // 다음 버튼 표시 여부 (아직 더 보여줄 데이터가 있으면 true)
    private boolean next;

    // 화면에 표시할 데이터 목록
    private List<E> dtoList;

    @Builder(builderMethodName = "withAll")
    public PageResponseDTO(PageRequestDTO pageRequestDTO, int total, List<E> dtoList) {
        this.page = pageRequestDTO.getPage();
        this.size = pageRequestDTO.getSize();
        this.total = total;
        this.dtoList = dtoList;

        int pageRangeCount = 10; // 한 번에 보여줄 페이지 버튼 개수

        // 기본 start/end 계산 (페이지 버튼 10개 단위)
        this.end = (int) (Math.ceil(this.page / (double) pageRangeCount) * pageRangeCount);
        this.start = this.end - (pageRangeCount - 1);

        // 마지막 페이지 번호 계산
        int last = (int) (Math.ceil(total / (double) size));

        // end가 마지막 페이지를 넘지 않도록 보정
        this.end = Math.min(end, last);

        // 데이터가 없으면 1페이지로 고정
        if (last == 0) {
            this.start = 1;
            this.end = 1;
        }

        // 이전 버튼: start가 1보다 크면 표시
        this.prev = this.start > 1;

        // 다음 버튼: 아직 표시 안 된 데이터가 남아있으면 표시
        this.next = total > this.end * this.size;
    }
}