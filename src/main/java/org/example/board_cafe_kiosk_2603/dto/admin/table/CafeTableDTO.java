package org.example.board_cafe_kiosk_2603.dto.admin.table;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CafeTableDTO {
    /**
     * 테이블 현황 응답 및 수정용 DTO
     * 추가된 내용: 입실 시간 포맷팅 및 토큰 정보 포함
     */
    private Integer id;
    private Integer tableNumber;
    private String status;      // EMPTY, OCCUPIED, CLEANING
    private String accessToken; // 테이블 인증 토큰
    private LocalDateTime checkInTime;
    private Integer guestCount; // 입장 인원 수
    private boolean hasUnreadMessage; // 읽지 않은 메시지 존재 여부
    /* 화면 UI(대시보드)에서 사용하기 적합한 형태로 가공하여 전달 */
}
