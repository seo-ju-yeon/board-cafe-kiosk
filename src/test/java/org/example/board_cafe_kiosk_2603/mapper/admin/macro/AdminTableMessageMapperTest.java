package org.example.board_cafe_kiosk_2603.mapper.admin.macro;

import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.macro.AdminTableMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
class AdminTableMessageMapperTest {
    @Autowired
    private AdminTableMessageMapper adminTableMessageMapper;

    @Test
    public void insertMessageTest() {
        AdminTableMessage params = AdminTableMessage.builder()
                .tableId(1)
                .macroId(1)
                .direction("STAFF_TO_TABLE")
                .content("주문하신 음료 준비되었습니다.").build();

        adminTableMessageMapper.insertMessage(params);
    }

}