package org.example.board_cafe_kiosk_2603.mapper.kiosk.tableMessage;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.board_cafe_kiosk_2603.domain.kiosk.tableMessage.TableMessage;

import java.util.List;

@Mapper
public interface TableMessageMapper {

    // 테이블 → 관리자 메시지 저장
    void insert(TableMessage message);

    // 관리자용: 안 읽은 메시지 전체 조회
    List<TableMessage> findUnread();

    // 관리자용: 테이블별 메시지 조회
    List<TableMessage> findByTableId(@Param("tableId") int tableId);

    // 키오스크용: 관리자 -> 테이블 미확인 메시지 조회
    List<TableMessage> findUnreadStaffByTableId(@Param("tableId") int tableId);

    // 읽음 처리
    void markAsRead(@Param("id") long id);

    // 전체 읽음 처리
    void markAllAsRead();
}
