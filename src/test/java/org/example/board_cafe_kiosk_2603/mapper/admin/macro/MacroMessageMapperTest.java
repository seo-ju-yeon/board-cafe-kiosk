package org.example.board_cafe_kiosk_2603.mapper.admin.macro;

import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.macro.MacroMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
class MacroMessageMapperTest {
    @Autowired
    private MacroMessageMapper macroMessageMapper;

    @Test
    public void findAllActiveTest() {
        List<MacroMessage> activeMessages = macroMessageMapper.findAllActive();

        log.info("=== 활성 매크로 메시지 목록 (총 {}건) ===", activeMessages.size());
        for (MacroMessage msg : activeMessages) {
            log.info("ID: {} | 방향: {} | 내용: {} | 활성화여부: {}",
                    msg.getId(), msg.getDirection(), msg.getMessageText(), msg.isActive());
        }
    }

    @Test
    public void findByIdTest() {
        // 실제 DB에 존재하는 ID를 사용하세요
        Integer targetId = 1;
        MacroMessage message = macroMessageMapper.findById(targetId);

        if (message != null) {
            log.info("=== 매크로 상세 조회 결과 (ID: {}) ===", targetId);
            log.info("내용: {} | 전송방향: {}", message.getMessageText(), message.getDirection());

        } else {
            log.warn("ID {}번에 해당하는 매크로 메시지가 DB에 없습니다.", targetId);
        }
    }

    @Test
    public void checkOrderTest() {
        List<MacroMessage> activeMessages = macroMessageMapper.findAllActive();

        if (activeMessages.size() >= 2) {
            log.info("첫 번째 메시지 방향: {}, ID: {}", activeMessages.get(0).getDirection(), activeMessages.get(0).getId());
            log.info("두 번째 메시지 방향: {}, ID: {}", activeMessages.get(1).getDirection(), activeMessages.get(1).getId());
        }
    }

    @Test
    public void insertMacroTest() {
        // 새로운 매크로 객체 생성 (ID는 자동생성이므로 null)
        MacroMessage newMsg = new MacroMessage(null, "STAFF_TO_TABLE", "테스트용 매크로입니다.", true);

        macroMessageMapper.insertMacro(newMsg);

        // 등록 후 생성된 ID가 존재하는지 확인
        log.info("등록된 매크로 ID: {}", newMsg.getId());

        // 실제 DB에 들어갔는지 재조회
        MacroMessage savedMsg = macroMessageMapper.findById(newMsg.getId());
        log.info(savedMsg);
    }

    @Test
    public void deactivateMacroTest() {
        // 테스트용 데이터를 먼저 하나 등록
        MacroMessage tempMsg = new MacroMessage(null, "TABLE_TO_STAFF", "삭제될 메시지", true);
        macroMessageMapper.insertMacro(tempMsg);
        Integer targetId = tempMsg.getId();

        // 비활성화 처리 (is_active = false)
        macroMessageMapper.deactivateMacro(targetId);

        MacroMessage updatedMsg = macroMessageMapper.findById(targetId);

        log.info("비활성화 후 상태: {}", updatedMsg.isActive());

        // findAllActive 목록에 포함되지 않아야 함
        List<MacroMessage> activeList = macroMessageMapper.findAllActive();
        boolean existsInList = activeList.stream()
                .anyMatch(m -> m.getId().equals(targetId));

        log.info("활성 목록 포함 여부: {}", existsInList);
    }
}