package org.example.board_cafe_kiosk_2603.dto.kiosk.point;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PointKioskDTO {

    private final boolean exists;
    private final int     balance;

    public static PointKioskDTO of(boolean exists, int balance) {
        return PointKioskDTO.builder()
                .exists(exists)
                .balance(balance)
                .build();
    }

    public static PointKioskDTO found(int balance) {
        return PointKioskDTO.builder()
                .exists(true)
                .balance(balance)
                .build();
    }

    public static PointKioskDTO notFound() {
        return PointKioskDTO.builder()
                .exists(false)
                .balance(0)
                .build();
    }
}
