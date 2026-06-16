package org.example.board_cafe_kiosk_2603.mapper.admin.manager;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.board_cafe_kiosk_2603.domain.admin.manager.Manager;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ManagerMapper {

    // 전체 목록 조회
    List<Manager> findAll();

    // 로그인 ID로 단건 조회 (Security용)
    Optional<Manager> findByLoginId(String loginId);

    // 직원 등록
    void insert(Manager manager);

    // 활성/비활성 토글
    void updateActive(@Param("id") int id, @Param("isActive") boolean isActive);

    // 내 정보 수정 (이름, 비밀번호 업데이트)
    void updateProfileInfo(@Param("loginId") String loginId,
                           @Param("name") String name,
                           @Param("password") String password);

    // 2차 인증용 이메일 단건 조회
    // - LoginController에서 loginId → email 변환에 사용
    // - Manager 전체 조회(findByLoginId)를 재사용해도 되지만,
    //   비밀번호 등 민감 필드를 불필요하게 로드하지 않기 위해 분리
    Optional<String> findEmailByLoginId(String loginId);

    /*============= 페이징 =============== */
    // 페이징 목록 조회
    List<Manager> selectList(PageRequestDTO pageRequestDTO);

    // 페이징 전체 개수 조회
    int selectCount(PageRequestDTO pageRequestDTO);
}
