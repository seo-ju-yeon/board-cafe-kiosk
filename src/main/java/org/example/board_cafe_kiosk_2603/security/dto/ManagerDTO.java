package org.example.board_cafe_kiosk_2603.security.dto;

import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
@Log4j2
public class ManagerDTO extends User {
    /* 시큐리티 전용 DTO */
    // admin_layout에 현재 로그인되어있는 이름을 출력하기 위함

    private final String name;  // 실명 추가

    public ManagerDTO(String username,
                      String password,
                      String name,
                      boolean enabled, // isActive 값
                      Collection<? extends GrantedAuthority> authorities) {

        // User 생성자: (username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities)
        // 부모 클래스(User)의 생성자를 반드시 호출해야 함.
        super(username, password,
                enabled,  // false면 Spring Security가 자동으로 DisabledException 발생
                true, true, true,
                authorities);

        this.name = name;
        log.info("--- [ManagerDTO 생성] username: {}, enabled: {} ---", username, enabled);
    }
}
