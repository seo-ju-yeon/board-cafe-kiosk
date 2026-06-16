package org.example.board_cafe_kiosk_2603.domain.kiosk.cafePackage;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CafePackage {
    private int           id;
    private String        name;
    private String        type;             // HOURLY | FIXED_TIME | FREE
    private Integer       durationMinutes;  // nullable
    private int           basePrice;
    private Double        extraPricePerMin; // nullable
    private boolean       active;
    private Integer       updatedBy;        // nullable
    private LocalDateTime updatedAt;
}
