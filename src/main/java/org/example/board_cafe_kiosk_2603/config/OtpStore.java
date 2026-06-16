package org.example.board_cafe_kiosk_2603.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Component
public class OtpStore {

    // 내부 클래스 -> OTP 번호와 만료 시간을 하나로 묶어 저장하는 데이터 구조
    private record OtpEntry(String code, LocalDateTime expiredAt) {
    }

    // 저장소 -> 이메일을 Key로, OTP 정보를 Value로 저장
    // ConcurrentHashMap을 사용하여 여러 사용자가 동시에 접근해도 안전함.
    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();

    /* OTP 저장 (3분 유효) */
    public void save(String email, String code) {
        log.info("--- [OTP 생성] Email: {}, ExpiredAt: {} ---", email, LocalDateTime.now().plusMinutes(3));
        store.put(email, new OtpEntry(code, LocalDateTime.now().plusMinutes(3)));
    }

    /* OTP 검증 —> 일치 + 만료 여부 확인 후 즉시 삭제 (1회용) */
    public boolean verify(String email, String code) {
        OtpEntry entry = store.get(email);

        // 해당 이메일로 발급된 OTP가 없는 경우
        if (entry == null) {
            log.warn("--- [OTP 검증 실패] 발급 기록 없음 - Email: {} ---", email);
            return false;
        }

        // 만료 시간 체크
        if (LocalDateTime.now().isAfter(entry.expiredAt())) {
            log.warn("--- [OTP 검증 실패] 시간 만료 - Email: {}, 만료시각: {} ---", email, entry.expiredAt());
            store.remove(email);  // 만료된 데이터는 즉시 삭제
            return false;
        }

        // 번호 일치 여부 확인
        if (!entry.code().equals(code)) {
            log.warn("--- [OTP 검증 실패] 번호 불일치 - Email: {} ---", email);
            return false;
        }

        // 한 번 성공한 번호로 다시 인증할 수 없도록 메모리에서 제거
        log.info("--- [OTP 검증 성공] 인증 완료 및 삭제 - Email: {} ---", email);
        store.remove(email); // 검증 성공 시 즉시 삭제
        return true;
    }
}
