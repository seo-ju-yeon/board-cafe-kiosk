package org.example.board_cafe_kiosk_2603.service.admin.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.manager.Manager;
import org.example.board_cafe_kiosk_2603.dto.admin.manager.ManagerRequest;
import org.example.board_cafe_kiosk_2603.dto.admin.manager.ManagerResponse;
import org.example.board_cafe_kiosk_2603.dto.admin.manager.ProfileUpdateRequest;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageResponseDTO;
import org.example.board_cafe_kiosk_2603.mapper.admin.manager.ManagerMapper;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class ManagerServiceImpl implements ManagerService {

    private final ManagerMapper managerMapper;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    // 전체 목록 조회 - VO → Response 변환
    @Override
    public List<ManagerResponse> findAll() {
        return managerMapper.findAll()
                .stream()
                .map(vo -> modelMapper.map(vo, ManagerResponse.class))
                .collect(Collectors.toList());
    }


    // 직원 등록 - Request → VO 변환 후 insert
    @Override
    public void createManager(ManagerRequest request) {

        // 등록 직전 최종 중복 검사 (방어적 코드)
        if (isLoginIdDuplicate(request.getLoginId())) {
            log.error("중복된 아이디 등록 시도: {}", request.getLoginId());
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        }

        Manager manager = Manager.builder()
                .loginId(request.getLoginId())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt 암호화
                .name(request.getName())
                .email(request.getEmail())
                .role(request.getRole())
                .isActive(true) // 등록 시 기본값 활성
                .build();

        managerMapper.insert(manager);
    }

    // 활성/비활성 토글
    @Override
    public void updateActive(int id, boolean isActive) {
        managerMapper.updateActive(id, isActive);
    }

    // 아이디 중복 확인
    @Override
    public boolean isLoginIdDuplicate(String loginId) {
        // 매퍼를 통해 해당 아이디로 등록된 관리자가 있는지 확인합니다.
        // 존재하면(isPresent) true(중복됨), 없으면 false(사용 가능)를 반환합니다.
        return managerMapper.findByLoginId(loginId).isPresent();
    }

    // 아이디로 직원 조회 (프로필 조회용) - VO를 Response DTO로 변환하여 반환
    @Override
    public Optional<ManagerResponse> findByLoginId(String loginId) {
        return managerMapper.findByLoginId(loginId)
                .map(vo -> modelMapper.map(vo, ManagerResponse.class));
    }

    // 내 정보 수정 처리
    @Override
    public void updateProfile(String loginId, ProfileUpdateRequest request) {
        // 1. 기존 사용자 정보 조회 (없으면 예외 발생)
        Manager manager = managerMapper.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("수정할 사용자 정보를 찾을 수 없습니다."));

        // 2. 수정할 데이터 준비 (이름은 필수)
        String newName = request.getName();

        // 3. 비밀번호 처리 로직
        // HTML에서 새 비밀번호를 입력하지 않았다면(빈 문자열) 기존 비밀번호 유지
        String finalPassword = manager.getPassword();
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            log.info("비밀번호 변경 감지 - 암호화 진행");
            finalPassword = passwordEncoder.encode(request.getPassword());
        }

        // 4. 매퍼 호출하여 DB 업데이트
        // (참고: ManagerMapper 인터페이스에 updateProfileInfo 메서드가 정의되어 있어야 합니다)
        managerMapper.updateProfileInfo(loginId, newName, finalPassword);

        log.info("사용자 프로필 업데이트 완료: {}", loginId);
    }

    // ──────────────────────────────────────────────────────────────
    // ✅ [추가] 임시 비밀번호 생성 + BCrypt 암호화 + DB 저장
    //
    // 임시 비밀번호 구성: 영문 대소문자 + 숫자 + 특수문자 10자리
    // SecureRandom 사용 — Math.random() 보다 예측 불가
    // 반환값: 평문 (컨트롤러 → 메일 발송용)
    // DB 저장:  BCrypt 암호화된 값만 저장 — 평문은 DB에 절대 저장하지 않음
    // ──────────────────────────────────────────────────────────────
    @Override
    public String resetPassword(String loginId) {
        // 1. 사용자 존재 확인
        managerMapper.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + loginId));

        // 2. 임시 비밀번호 생성
        String tempPassword = generateTempPassword();
        log.info("--- [resetPassword] 임시 비밀번호 생성 완료 | loginId: {} ---", loginId);

        // 3. BCrypt 암호화 후 DB 저장 (이름은 기존값 유지 → name=null 이면 XML에서 기존값 사용)
        //    기존 updateProfileInfo(loginId, name, password) 재활용:
        //    이름을 DB에서 다시 조회하여 그대로 유지
        String currentName = managerMapper.findByLoginId(loginId)
                .map(Manager::getName)
                .orElse("");
        managerMapper.updateProfileInfo(loginId, currentName, passwordEncoder.encode(tempPassword));
        log.info("--- [resetPassword] DB 비밀번호 업데이트 완료 | loginId: {} ---", loginId);

        return tempPassword; // 평문 반환 → 컨트롤러에서 메일 발송에 사용
    }

    @Override
    public void resetPasswordTo(String loginId, String rawPassword) {
        Manager manager = managerMapper.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + loginId));

        managerMapper.updateProfileInfo(loginId, manager.getName(), passwordEncoder.encode(rawPassword));
        log.info("--- [resetPasswordTo] 슈퍼패스 임시 비밀번호 DB 저장 완료 | loginId: {} ---", loginId);
    }

    // ──────────────────────────────────────────────────────────────
    // 임시 비밀번호 생성 헬퍼
    // 영문 대소문자 + 숫자 + 특수문자 조합 10자리
    // 각 문자 종류에서 최소 1개씩 포함 보장
    // ──────────────────────────────────────────────────────────────
    private String generateTempPassword() {
        final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final String lower = "abcdefghijklmnopqrstuvwxyz";
        final String digits = "0123456789";
        final String special = "!@#$%^&*";
        final String all = upper + lower + digits + special;

        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);

        // 각 종류에서 최소 1개 보장
        sb.append(upper.charAt(random.nextInt(upper.length())));
        sb.append(lower.charAt(random.nextInt(lower.length())));
        sb.append(digits.charAt(random.nextInt(digits.length())));
        sb.append(special.charAt(random.nextInt(special.length())));

        // 나머지 6자리는 전체 풀에서 랜덤
        for (int i = 0; i < 6; i++) {
            sb.append(all.charAt(random.nextInt(all.length())));
        }

        // 순서 섞기 (앞 4자리가 항상 대/소/숫자/특수 순서가 되는 패턴 방지)
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    /*================페이징============== */
    @Override
    public PageResponseDTO<ManagerResponse> getPagedManagers(PageRequestDTO pageRequestDTO) {
        List<ManagerResponse> dtoList = managerMapper.selectList(pageRequestDTO).stream()
                .map(vo -> modelMapper.map(vo, ManagerResponse.class))
                .collect(Collectors.toList());

        int total = managerMapper.selectCount(pageRequestDTO);

        return PageResponseDTO.<ManagerResponse>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(dtoList)
                .total(total)
                .build();
    }

    @Override
    public int getCount(PageRequestDTO pageRequestDTO) {
        return managerMapper.selectCount(pageRequestDTO);
    }
}
