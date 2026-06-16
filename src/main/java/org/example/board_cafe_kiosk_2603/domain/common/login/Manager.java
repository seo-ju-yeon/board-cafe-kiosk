package org.example.board_cafe_kiosk_2603.domain.common.login;

import lombok.*;

import java.time.LocalDateTime;

/**
 * manager — 관리자·직원 계정.
 */
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Manager {
    private int id;
    private String loginId;
    private String password;
    private String name;
    private String role;       // ADMIN | STAFF
    private boolean isActive;
    private LocalDateTime createdAt;
}
