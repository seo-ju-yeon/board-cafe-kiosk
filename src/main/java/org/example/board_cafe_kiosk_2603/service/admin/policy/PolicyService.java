package org.example.board_cafe_kiosk_2603.service.admin.policy;

import org.example.board_cafe_kiosk_2603.dto.admin.policy.PolicyDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageResponseDTO;

public interface PolicyService {
    // ID로 단건 조회
    PolicyDTO getById(int id);

    // 패키지 등록
    void insert(PolicyDTO policyDTO);

    // 패키지 수정
    void update(PolicyDTO policyDTO);

    // 활성/비활성 토글
    void updateStatus(int id, boolean active);

    // 페이징
    PageResponseDTO<PolicyDTO> selectPagedPolicies(PageRequestDTO pageRequestDTO);

    // 탭별 개수
    int getCount(PageRequestDTO pageRequestDTO);
}
