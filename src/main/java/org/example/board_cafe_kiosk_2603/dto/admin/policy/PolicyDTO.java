package org.example.board_cafe_kiosk_2603.dto.admin.policy;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDTO {

    // === 도메인 VO 필드 ===
    private int           id;
    private String        name;
    private String        type;
    private Integer       durationMinutes;
    private int           basePrice;
    private Double        extraPricePerMin;
    private boolean       active;
    private LocalDateTime updatedAt;

    // === 응답(Response)용 필드 ===
    private boolean success;
    private String  message;

    // === 정적 팩토리 ===
    public static PolicyDTO fail(String message) {
        return PolicyDTO.builder()
                .success(false)
                .message(message)
                .build();
    }

    public static PolicyDTO success(String message) {
        return PolicyDTO.builder()
                .success(true)
                .message(message)
                .build();
    }

    // === 표시용 헬퍼 ===
    public String getDisplayTime() {
        if (durationMinutes == null) return "Free";
        if (durationMinutes < 60) return durationMinutes + "분";
        return (durationMinutes / 60) + "시간";
    }
}
