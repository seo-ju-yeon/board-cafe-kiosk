package org.example.board_cafe_kiosk_2603.service.admin.manager;

import org.example.board_cafe_kiosk_2603.dto.admin.manager.ManagerRequest;
import org.example.board_cafe_kiosk_2603.dto.admin.manager.ManagerResponse;
import org.example.board_cafe_kiosk_2603.dto.admin.manager.ProfileUpdateRequest;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageResponseDTO;

import java.util.List;
import java.util.Optional;

public interface ManagerService {

    // 전체 목록 조회
    List<ManagerResponse> findAll();

    // 직원 등록
    void createManager(ManagerRequest request);

    // 활성/비활성 토글
    void updateActive(int id, boolean isActive);

    // 아이디로 직원 조회 (중복 확인용 -> 중복 여부만 알수있음)
    boolean isLoginIdDuplicate(String loginId);

    // profile
    // 프로필 조회용
    Optional<ManagerResponse> findByLoginId(String loginId);
    // 프로필 수정용
    void updateProfile(String loginId, ProfileUpdateRequest request);

    // 임시 비밀번호 생성 + BCrypt 암호화 + DB 저장
    // 반환값: 평문 임시 비밀번호 (메일 발송용 — DB에는 암호화된 값만 저장)
    String resetPassword(String loginId);
//    void updateProfile(String loginId, ManagerRequest request);

    // ✅ [추가] 포트폴리오 슈퍼패스용 — 고정 임시 비밀번호 DB 저장 (메일 발송 없음)
    // rawPassword: 평문 고정 비밀번호 (SuperKeyProperties.tempPasswd)
    // → BCrypt 암호화 후 DB 저장
    void resetPasswordTo(String loginId, String rawPassword);

    /*================페이징============== */
    // 페이징 목록 조회
    PageResponseDTO<ManagerResponse> getPagedManagers(PageRequestDTO pageRequestDTO);

    // 조건별 개수 조회
    int getCount(PageRequestDTO pageRequestDTO);
}
