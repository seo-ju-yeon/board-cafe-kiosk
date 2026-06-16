package org.example.board_cafe_kiosk_2603.controller.admin.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.config.OtpStore;
import org.example.board_cafe_kiosk_2603.dto.admin.manager.ManagerRequest;
import org.example.board_cafe_kiosk_2603.dto.admin.manager.ManagerResponse;
import org.example.board_cafe_kiosk_2603.dto.admin.manager.ProfileUpdateRequest;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageResponseDTO;
import org.example.board_cafe_kiosk_2603.service.admin.manager.ManagerService;
import org.example.board_cafe_kiosk_2603.service.admin.sms.MailSenderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Log4j2
@Controller
@RequestMapping("/admin/staff")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER')")
public class ManagerController {

    private final MailSenderService mailSenderService;
    private final OtpStore otpStore;
    private final ManagerService managerService;

    /* 신규 직원 등록 */
    @PostMapping
    @ResponseBody  // AJAX 요청이므로 데이터만 반환
    public ResponseEntity<String> createStaff(@RequestBody ManagerRequest managerRequest) {
        log.info("--- [직원 등록 시작] 입력 정보: {} ---", managerRequest);

        try {
            // JSON 데이터를 객체로 받아 직원 생성 서비스 호출
            managerService.createManager(managerRequest);
            log.info("--- [직원 등록 완료] ID: {} 등록 성공 ---", managerRequest.getLoginId());
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            log.error("--- [직원 등록 실패] 사유: {} ---", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("fail");
        }
    }

    /* 직원 활성화/비활성화 상태 토글 */
    @PostMapping("/toggle-status")
    @ResponseBody  // AJAX 요청이므로 데이터만 반환 (모달 폼 -> AJAX)
    public ResponseEntity<String> toggleStaffStatus(@RequestParam("id") Integer id,
                                                    @RequestParam("active") Boolean isActive) {

        log.info("--- 직원 상태 변경 요청 시작 ---");
        log.info("요청 ID: {}, 변경할 상태: {}", id, isActive);

        try {
            // 특정 직원의 계정 사용 여부를 실시간으로 바꿈
            managerService.updateActive(id, isActive);
            log.info("--- [상태 변경 성공] ID {}번 직원의 활성화 상태가 {}로 변경됨 ---", id, isActive);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            log.error("--- [상태 변경 실패] ID {}번 처리 중 에러: {} ---", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("fail");
        }
    }

    /* 아이디 중복 확인 */
    @GetMapping("/check-id")
    @ResponseBody  // AJAX 요청이므로 데이터만 반환
    public ResponseEntity<Boolean> checkId(@RequestParam("loginId") String loginId) {

        log.info("--- [아이디 중복 확인] 요청 ID: {} ---", loginId);
        // DB에 해당 아이디가 이미 존재하는지 확인
        boolean isDuplicate = managerService.isLoginIdDuplicate(loginId);

        // 결과가 true면 중복, false면 사용 가능
        log.info("중복 여부 결과: {}", isDuplicate ? "중복됨(사용불가)" : "미중복(사용가능)");
        return ResponseEntity.ok(isDuplicate);
    }

    /* 내 프로필 수정 페이지 이동 */
    @GetMapping("/profile")
    public String profilePage(Model model, Principal principal) {

        if (principal == null) {
            log.warn("[접근 제한] 로그인 정보가 없어 로그인 페이지로 리다이렉트합니다.");
            return "redirect:/login";
        }

        String loginId = principal.getName();  // 현재 로그인한 ID 추출
        log.info("[프로필 조회] 로그인 사용자 ID: {}", loginId);

        ManagerResponse manager = managerService.findByLoginId(loginId)
                .orElseThrow(() -> {
                    log.error("[조회 에러] 로그인한 ID({})를 DB에서 찾을 수 없음", loginId);
                    return new RuntimeException("사용자를 찾을 수 없습니다.");
                });

        model.addAttribute("manager", manager);
        return "admin/staff_profile";
    }

    /* OTP 인증 번호 메일 발송 (프로필 수정 전) */
    @PostMapping("/profile/send-otp")
    @ResponseBody  // AJAX 요청이므로 데이터만 반환
    public ResponseEntity<String> sendOtp(Principal principal) {
        // 세션에서 현재 로그인한 관리자 이메일 가져오기
        String loginId = principal.getName();
        log.info("--- [OTP 발송 요청] 사용자: {} ---", loginId);

        // 서비스 레이어 통해 조회
        ManagerResponse manager = managerService.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        String email = manager.getEmail();
        log.info("--- [OTP 발송 대상 이메일] {} ---", email);

        // 번호 생성 및 메모리(OtpStore) 임시 저장
        String code = mailSenderService.generateVerificationCode();
        otpStore.save(email, code);

        try {
            // 메일 전송
            mailSenderService.sendMailForAlarm(email, code);
            log.info("--- [OTP 메일 전송 성공] 이메일: {}, 번호: {} ---", email, code);
            return ResponseEntity.ok("OTP 발송 완료");
        } catch (Exception e) {
            log.error("--- [OTP 메일 전송 실패] 에러: {} ---", e.getMessage());
            return ResponseEntity.status(500).body("메일 발송 실패");
        }
    }

    /* 프로필 수정 최종 제출 (OTP 검증 포함) */
    @PostMapping("/profile/update")
    @ResponseBody  // AJAX 요청이므로 데이터만 반환
    public ResponseEntity<String> updateProfile(
            @RequestBody ProfileUpdateRequest request,
            Principal principal) {

        String loginId = principal.getName();
        log.info("[프로필 업데이트 시작] 사용자: {}", loginId);

        ManagerResponse manager = managerService.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        String email = manager.getEmail();

        // OTP 검증
        log.info("--- [OTP 검증 시도] 이메일: {}, 입력코드: {} ---", email, request.getOtp());
        if (!otpStore.verify(email, request.getOtp())) {
            log.warn("--- [OTP 검증 실패] 올바르지 않은 코드이거나 시간이 만료됨 - 이메일: {} ---", email);
            return ResponseEntity.status(401).body("OTP가 올바르지 않거나 만료되었습니다.");
        }

        log.info("--- [OTP 검증 성공] 프로필 DB 업데이트 진행 ---");
        managerService.updateProfile(loginId, request);

        log.info("[프로필 업데이트 완료] 사용자: {}", loginId);
        return ResponseEntity.ok("수정 완료");
    }

    /*================페이징============== */
    // 직원 목록 페이지
    @GetMapping
    public String staffPage(PageRequestDTO pageRequestDTO, Model model) {
        log.info("--- 직원 목록 페이지 진입 ---");

        PageResponseDTO<ManagerResponse> responseDTO = managerService.getPagedManagers(pageRequestDTO);

        // 각 탭별 개수 조회
        PageRequestDTO allReq = PageRequestDTO.builder().page(1).size(1).build();
        PageRequestDTO activeReq = PageRequestDTO.builder().page(1).size(1).filter("active").build();
        PageRequestDTO inactiveReq = PageRequestDTO.builder().page(1).size(1).filter("inactive").build();

        model.addAttribute("responseDTO", responseDTO);
        model.addAttribute("pageRequestDTO", pageRequestDTO);
        // filter가 null이면 "all"로 기본값 설정
        model.addAttribute("filter", pageRequestDTO.getFilter() != null ? pageRequestDTO.getFilter() : "all");
        model.addAttribute("countAll", managerService.getCount(allReq));
        model.addAttribute("countActive", managerService.getCount(activeReq));
        model.addAttribute("countInactive", managerService.getCount(inactiveReq));
        model.addAttribute("activePage", "staffManagement");

        return "admin/staff";  // templates/admin/staff.html
    }
}
