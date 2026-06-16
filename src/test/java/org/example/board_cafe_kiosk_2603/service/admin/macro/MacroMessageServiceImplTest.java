package org.example.board_cafe_kiosk_2603.service.admin.macro;

import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.dto.admin.macro.MacroMessageResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
class MacroMessageServiceImplTest {
    @Autowired
    private MacroMessageService macroMessageService;

    @Test
    public void getAllActiveMessagesTest() {
        List<MacroMessageResponseDTO> messages = macroMessageService.getAllActiveMessages();

        log.info("=== 서비스: 활성 매크로 목록 ({}건) ===", messages.size());
        messages.forEach(dto ->
                log.info("ID: {} | 내용: {} | 방향: {}", dto.getId(), dto.getMessageText(), dto.getDirection())
        );
    }

    @Test
    public void sendMessageTest() {
        // 실제 DB에 존재하는 table_id와 macro_id를 사용해야 함
        Integer tableId = 1;
        Integer macroId = 1;

        log.info("메시지 전송 실행: Table {}, Macro {}", tableId, macroId);

        // 예외 없이 실행되는지 확인
        macroMessageService.sendMessage(tableId, macroId);

        log.info("메시지 전송 로직 완료");
    }

    @Test
    public void sendToAllActiveTablesTest() {
        // 실제 DB에 'OCCUPIED' 상태인 테이블이 최소 1개 이상 있어야 로그 확인 가능
        Integer macroId = 1;

        log.info("전체 테이블 대상 공지 전송 시작 (Macro ID: {})", macroId);

        macroMessageService.sendToAllActiveTables(macroId);

        log.info("전체 공지 전송 로직 완료");
    }

    @Test
    public void sendMessageWithInvalidIdTest() {
        Integer tableId = 1;
        Integer invalidMacroId = 9999; // 존재하지 않는 ID

        log.info("부적절한 ID로 메시지 전송 시도: {}", invalidMacroId);

        // 서비스 내부에서 null 체크 후 return 하므로 에러 없이 종료되어야 함
        macroMessageService.sendMessage(tableId, invalidMacroId);

        log.info("부적절한 ID 처리 완료");
    }

    @Test
    public void createMacroTest() {
        // 1. 준비
        String direction = "STAFF_TO_TABLE";
        String content = "테스트: 서비스에서 등록한 메시지입니다.";

        // 2. 실행
        macroMessageService.createMacro(direction, content);

        // 3. 검증 (전체 활성 목록 조회 시 포함되어 있는지 확인)
        List<MacroMessageResponseDTO> messages = macroMessageService.getAllActiveMessages();

        boolean isPresent = messages.stream()
                .anyMatch(m -> m.getMessageText().equals(content));

        log.info("새 매크로 등록 여부: {}", isPresent);
    }

    @Test
    public void deleteMacroTest() {
        // 1. 준비: 삭제할 대상을 먼저 하나 찾음 (목록이 비어있지 않다고 가정)
        List<MacroMessageResponseDTO> beforeList = macroMessageService.getAllActiveMessages();

        Integer targetId = beforeList.get(0).getId();
        String targetText = beforeList.get(0).getMessageText();

        // 2. 실행: 삭제(is_active = false)
        macroMessageService.deleteMacro(targetId);
        log.info("삭제 요청 ID: {} (내용: {})", targetId, targetText);

        // 3. 검증: 활성 목록 재조회 시 해당 ID가 없어야 함
        List<MacroMessageResponseDTO> afterList = macroMessageService.getAllActiveMessages();

        boolean stillExists = afterList.stream()
                .anyMatch(m -> m.getId().equals(targetId));

        log.info("삭제 후 목록에 존재 여부: {}", stillExists);
    }
}