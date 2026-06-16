package org.example.board_cafe_kiosk_2603.security.dto;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
@Log4j2
public class KioskDTO extends User {
    private final int tableId; // cafe_table.id (세션 조회, 화면 분기에 활용)

    public KioskDTO(String username,    // tableNumber (문자열)
                    String password,
                    int tableId,
                    Collection<? extends GrantedAuthority> authorities) {

        super(username, password, authorities);
        this.tableId = tableId;
        log.info("--- KioskDTO 생성 | tableNumber: {}, tableId: {} ---", username, tableId);
    }
}
