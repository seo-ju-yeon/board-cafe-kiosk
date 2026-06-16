package org.example.board_cafe_kiosk_2603.dto.admin.manager;

import lombok.Getter;

@Getter
public class ProfileUpdateRequest {
    private String name;
    private String password; // 빈 값이면 변경 안 함
    private String otp;      // 추가
}
