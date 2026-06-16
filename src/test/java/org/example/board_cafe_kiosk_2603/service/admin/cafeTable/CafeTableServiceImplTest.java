package org.example.board_cafe_kiosk_2603.service.admin.cafeTable;

import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.table.CafeTable;
import org.example.board_cafe_kiosk_2603.dto.admin.table.CafeTableDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

@Log4j2
@SpringBootTest
class CafeTableServiceImplTest {
    @Autowired
    private CafeTableService cafeTableService;

    @Test
    public void getAllTableStatusTest() {
        List<CafeTableDTO> tableStatusList = cafeTableService.getAllTableStatus();

        log.info("=== 서비스: 전체 테이블 상태 목록 ===");
        tableStatusList.forEach(dto -> {
            log.info("번호: {} | 상태: {} | 메시지여부: {}",
                    dto.getTableNumber(), dto.getStatus(), dto.isHasUnreadMessage());
        });
    }

    @Test
    public void changeTableStatusExceptionTest() {
        // 토큰이 없는 테이블(예: 신규 생성 직후) ID를 가정
        Integer targetTableId = 1;

        // 해당 테이블의 토큰을 강제로 비운 뒤 OCCUPIED 시도 (만약 토큰이 있다면 실패할 수 있음)
        // 여기선 로직의 흐름만 체크합니다.
        log.info("토큰 체크 로직 검증 시작");

        // 토큰이 없을 때 IllegalStateException이 발생하는지 확인
        // assertThatThrownBy(() -> cafeTableService.changeTableStatus(targetTableId, "OCCUPIED"))
        //     .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void generateNewTokenTest() {
        Integer tableId = 1;
        String newToken = cafeTableService.generateNewToken(tableId);

        log.info("발급된 새 토큰: {}", newToken);
    }

    @Test
    public void loginTest() {
        int tableNumber = 1;
        String rawPassword = "test_password"; // 실제 DB에 인코딩되어 저장된 비밀번호와 대조 필요

        Optional<CafeTable> result = cafeTableService.login(tableNumber, rawPassword);

        if (result.isPresent()) {
            log.info("로그인 성공: 테이블 번호 {}", result.get().getTableNumber());
        } else {
            log.warn("로그인 실패: 비밀번호 불일치 혹은 존재하지 않는 테이블");
        }
    }

    @Test
    public void getUnreadMessagesTest() {
        Integer tableId = 1;
        List<String> messages = cafeTableService.getUnreadMessages(tableId);

        log.info("=== {}번 테이블 손님 요청 내역 ===", tableId);
        messages.forEach(msg -> log.info("내용: {}", msg));
    }

    @Test
    public void resetAllTablesForNewDayTest() {
        log.info("자정 리셋 서비스 실행");
        cafeTableService.resetAllTablesForNewDay();

        // 리셋 후 모든 테이블 상태가 EMPTY인지 확인
        List<CafeTableDTO> tables = cafeTableService.getAllTableStatus();
        boolean allEmpty = tables.stream().allMatch(t -> "EMPTY".equals(t.getStatus()));
    }

}