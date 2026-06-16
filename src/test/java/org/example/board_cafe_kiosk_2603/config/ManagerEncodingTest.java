package org.example.board_cafe_kiosk_2603.config;

import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.manager.Manager;
import org.example.board_cafe_kiosk_2603.domain.admin.manager.RoleType;
import org.example.board_cafe_kiosk_2603.mapper.admin.manager.ManagerMapper;
import org.example.board_cafe_kiosk_2603.mapper.admin.table.CafeTableMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Log4j2
class ManagerEncodingTest {

    @Autowired
    private ManagerMapper managerMapper;
    @Autowired
    private CafeTableMapper cafeTableMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    // 비밀번호 암호화 후 manager 테이블에 저장
    @Test
    void passwordEncoderTest() {
        // 1. 준비
        String rawPassword = "12345";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // VO 생성 (생성자 또는 빌더 사용)
        Manager newManager = Manager.builder()
                .loginId("qwer")
                .password(encodedPassword)
                .name("테스트")
                .role(RoleType.ADMIN)
                .isActive(true)
                .build();

        // 2. 실행
        managerMapper.insert(newManager);

        // 3. 검증 (Optional 처리 중요!)
        // .orElseThrow()를 사용하면 Optional 안의 Manager를 바로 꺼낼 수 있습니다.
        Manager savedManager = managerMapper.findByLoginId("qwer")
                .orElseThrow(() -> new RuntimeException("관리자를 찾을 수 없습니다."));

        log.info("입력 평문: {}", rawPassword);
        log.info("DB에 저장된 암호문: {}", savedManager.getPassword());

        // 4. 단언
        // BCrypt 암호화 결과가 평문과 다른지 확인
        Assertions.assertNotEquals(rawPassword, savedManager.getPassword());

        // BCrypt 전용 매칭 확인
        boolean isMatch = passwordEncoder.matches(rawPassword, savedManager.getPassword());
        Assertions.assertTrue(isMatch, "암호화된 비밀번호가 일치해야 합니다.");
    }

//    @Test
//    void updateRealTablePassword() {
//        // 1. 준비: 테스트할 평문 비밀번호
//        int targetTableNumber = 2;  // 비밀번호를 바꿀 '테이블 번호'
//        String rawPassword = "2222";  // 사용할 비밀번호
//
//        // 2. 실행: 암호화 진행
//        String encodedPassword = passwordEncoder.encode(rawPassword);
//        log.info("--- [비밀번호 변경 시작] 테이블: {}, 암호문: {} ---", targetTableNumber, encodedPassword);
//
//        // 로그로 직접 눈으로 확인하기
//        log.info("=========================================");
//        log.info("1. 원본 평문(Raw): {}", rawPassword);
//        log.info("2. 암호화된 값(Encoded): {}", encodedPassword);
//        log.info("3. 암호화된 문자열 길이: {}", encodedPassword.length());
//        log.info("=========================================");
//

    /// /        // 3. 검증 (Assertions)
    /// /        // (1) 평문과 암호화된 값은 절대 같으면 안 됨 (단방향 해시의 핵심)
    /// /        assertNotEquals(rawPassword, encodedPassword, "평문과 암호문은 달라야 합니다.");
    /// /        // (2) matches 메서드를 사용하여 검증 (내부적으로 salt를 추출하여 비교함)
    /// /        boolean isMatch = passwordEncoder.matches(rawPassword, encodedPassword);
    /// /        assertTrue(isMatch, "평문과 암호문이 로직상 일치해야 합니다.");
    /// /        // (3) 틀린 비밀번호로 시도했을 때 false가 나오는지 확인
    /// /        boolean isWrongMatch = passwordEncoder.matches("wrong_password", encodedPassword);
    /// /        assertFalse(isWrongMatch, "틀린 비밀번호는 매칭에 실패해야 합니다.");
//
//        // 3. DB 업데이트 (직접 쿼리 실행)
//        // 주의: 해당 매퍼에 특정 테이블 번호의 비밀번호만 바꾸는 메서드가 있다면 그것을 쓰세요.
//        // 여기서는 기존에 있던 findByTableNumber로 객체를 가져와서 업데이트하는 방식을 예로 듭니다.
//        var table = cafeTableMapper.findByTableNumber(targetTableNumber)
//                .orElseThrow(() -> new RuntimeException("테이블을 찾을 수 없습니다."));
//
//        // 매퍼에 updatePassword와 같은 메서드가 없다면, 아래처럼 직접 쿼리를 날리는 메서드를 매퍼에 추가해야 합니다.
//        // cafeTableMapper.updatePassword(table.getId(), encoded);
//
//        log.info("--- [변경 완료] 이제 로그인 창에 '{}'를 입력하세요. ---", rawPassword);
//    }
    @Test
    void getEncodedPasswordForManualUpdate() {
        String raw = "2222"; // 내가 쓰고 싶은 비번
        String encoded = passwordEncoder.encode(raw);

        // 로그에 찍힌 이 SQL 문을 그대로 복사해서 DB 툴에서 실행하세요!
        log.info("실행할 SQL 문장: UPDATE cafe_table SET password = '{}' WHERE table_number = 2;", encoded);
    }
}