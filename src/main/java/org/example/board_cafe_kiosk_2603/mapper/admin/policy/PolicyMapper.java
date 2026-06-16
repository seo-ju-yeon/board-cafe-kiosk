package org.example.board_cafe_kiosk_2603.mapper.admin.policy;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.board_cafe_kiosk_2603.domain.admin.policy.Policy;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;

import java.util.List;

@Mapper
public interface PolicyMapper {

    // ID로 단건 조회
    Policy findById(int id);

    // 패키지 등록
    void insert(Policy policy);

    // 패키지 수정
    void update(Policy policy);

    // 활성/비활성 토글
    void updateStatus(@Param("id") int id, @Param("active") boolean active);

    // 페이징
    List<Policy> selectList(PageRequestDTO pageRequestDTO);
    int selectCount(PageRequestDTO pageRequestDTO);
}
