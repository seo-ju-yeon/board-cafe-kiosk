package org.example.board_cafe_kiosk_2603.mapper.admin.macro;

import org.apache.ibatis.annotations.Mapper;
import org.example.board_cafe_kiosk_2603.domain.admin.macro.AdminTableMessage;

@Mapper
public interface AdminTableMessageMapper {
    // 1클릭 전송 시 로그 저장
    void insertMessage(AdminTableMessage adminTableMessage);
}
