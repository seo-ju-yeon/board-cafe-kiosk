package org.example.board_cafe_kiosk_2603.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.table.CafeTable;
import org.example.board_cafe_kiosk_2603.mapper.admin.table.CafeTableMapper;
import org.example.board_cafe_kiosk_2603.security.dto.KioskDTO;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class KioskUserDetailsService implements UserDetailsService {

    private final CafeTableMapper cafeTableMapper;

    @Override
    public UserDetails loadUserByUsername(String username) {
        // username = tableNumber (문자열로 들어옴)

        log.info("--- [키오스크 로그인 시도] tableNumber: {} ---", username);

        try {
            // 1. 숫자로 변환 (예외 발생 가능 지점)
            int tableNumber = Integer.parseInt(username);
            log.info("--- [tableNumber 파싱 성공] tableNumber: {} ---", username);

            // 2. DB에서 테이블 조회
            CafeTable table = cafeTableMapper.findByTableNumber(tableNumber)
                    .orElseThrow(() -> {
                        log.warn("--- [키오스크 로그인 실패] 존재하지 않는 테이블: {} ---", username);
                        return new UsernameNotFoundException("테이블 없음: " + username);
                    });

            log.info("--- [키오스크 조회 성공] tableId: {}, tableNumber: {} ---",
                    table.getId(), table.getTableNumber());

            // 3. 인증 성공 시 DTO 반환
            return new KioskDTO(
                    String.valueOf(table.getTableNumber()),
                    table.getPassword(),
                    table.getId(),
                    List.of(new SimpleGrantedAuthority("ROLE_TABLE"))
            );

        } catch (NumberFormatException e) {
            // 숫자가 아닌 값이 입력되었을 때 예외 처리 (username이 숫자가 아닌 경우)
            log.error("--- [키오스크 로그인 실패] 잘못된 테이블 번호 형식: {} ---", username);
            throw new UsernameNotFoundException("잘못된 테이블 번호 형식: " + username);
        }
    }
}
