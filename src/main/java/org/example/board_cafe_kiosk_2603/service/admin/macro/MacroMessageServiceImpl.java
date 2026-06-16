package org.example.board_cafe_kiosk_2603.service.admin.macro;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.macro.AdminTableMessage;
import org.example.board_cafe_kiosk_2603.domain.admin.macro.MacroMessage;
import org.example.board_cafe_kiosk_2603.dto.admin.macro.MacroMessageResponseDTO;
import org.example.board_cafe_kiosk_2603.mapper.admin.macro.AdminTableMessageMapper;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageResponseDTO;
import org.example.board_cafe_kiosk_2603.mapper.admin.macro.MacroMessageMapper;
import org.example.board_cafe_kiosk_2603.mapper.admin.table.CafeTableMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MacroMessageServiceImpl implements MacroMessageService {
    private final MacroMessageMapper macroMessageMapper;
    private final AdminTableMessageMapper adminTableMessageMapper;
    private final CafeTableMapper cafeTableMapper;

    @Override
    public List<MacroMessageResponseDTO> getAllActiveMessages() {
        log.info("--- MacroMessageServiceImpl getAllActiveMessages ---");

        List<MacroMessageResponseDTO> activeMessages = new ArrayList<>();
        List<MacroMessage> result = macroMessageMapper.findAllActive();
        result.forEach(item -> {
            MacroMessageResponseDTO macroMessageResponseDTO = MacroMessageResponseDTO.builder()
                    .id(item.getId())
                    .messageText(item.getMessageText())
                    .direction(item.getDirection()).build();
            activeMessages.add(macroMessageResponseDTO);
        });
        return activeMessages;
    }

    @Override
    @Transactional
    public void sendMessage(Integer tableId, Integer macroId) {
        log.info("--- MacroMessageServiceImpl sendMessage: Table {}, Macro {} ---", tableId, macroId);

        // 1. 매크로 문구 조회 (기존 macroMessageMapper 활용)
        MacroMessage macro = macroMessageMapper.findById(macroId);

        if (macro == null) {
            log.error("매크로 메세지를 찾을 수 없습니다. ID: {}", macroId);
            return;
        }

        // 2. 로그 엔티티 생성 및 저장
        AdminTableMessage logMessage = AdminTableMessage.builder()
                .tableId(tableId)
                .macroId(macroId)
                .direction("STAFF_TO_TABLE")
                .content(macro.getMessageText())
                .build();

        adminTableMessageMapper.insertMessage(logMessage);
        log.info("테이블 {}번으로 메세지 로그 저장 성공!", tableId);
    }

    @Override
    @Transactional
    public void sendToAllActiveTables(Integer macroId) {
        List<Integer> tableIds = cafeTableMapper.selectOccupiedTableIds();

        for (Integer tableId : tableIds) {
            this.sendMessage(tableId, macroId);
        }
        log.info("📢 총 {}개 테이블에 전체 메시지 전송 완료!", tableIds.size());
    }

    @Override
    @Transactional
    public void createMacro(String direction, String messageText) {
        MacroMessage newMacro = new MacroMessage(null, direction, messageText, true);
        macroMessageMapper.insertMacro(newMacro);
        log.info("새 매크로 등록 완료: [{}] {}", direction, messageText);
    }

    @Override
    @Transactional
    public void deleteMacro(Integer id) {
        macroMessageMapper.deactivateMacro(id);
        log.info("매크로 삭제(비활성화) 완료: ID {}", id);
    }

    // direction별 페이징 조회
    @Override
    public PageResponseDTO<MacroMessageResponseDTO> getPagedMessage(String direction, PageRequestDTO pageRequestDTO) {
        List<MacroMessageResponseDTO> dtoList = macroMessageMapper
                .selectList(direction, pageRequestDTO.getSkip(), pageRequestDTO.getSize())
                .stream()
                .map(item -> MacroMessageResponseDTO.builder()
                        .id(item.getId())
                        .messageText(item.getMessageText())
                        .direction(item.getDirection())
                        .build())
                .collect(Collectors.toList());

        int total = macroMessageMapper.selectCount(direction);

        return PageResponseDTO.<MacroMessageResponseDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(dtoList)
                .total(total)
                .build();
    }
}
