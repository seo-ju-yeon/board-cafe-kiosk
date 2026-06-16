package org.example.board_cafe_kiosk_2603.service.admin.macro;

import org.example.board_cafe_kiosk_2603.dto.admin.macro.MacroMessageResponseDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageResponseDTO;

import java.util.List;

public interface MacroMessageService {
    // 활성화된 모든 매크로 메시지 조회
    List<MacroMessageResponseDTO> getAllActiveMessages();
    // 관리자 화면에서 테이블로 메세지 전송
    void sendMessage(Integer tableId, Integer macroId);
    // session이 활성화 되어 있는 전체 티이블에 공지
    void sendToAllActiveTables(Integer macroId);
    // 메세지 등록
    void createMacro(String direction, String messageText);
    // 메세지 비활성화 처리
    void deleteMacro(Integer id);

    // direction별 페이징 조회
    PageResponseDTO<MacroMessageResponseDTO> getPagedMessage(String direction, PageRequestDTO pageRequestDTO);
}
