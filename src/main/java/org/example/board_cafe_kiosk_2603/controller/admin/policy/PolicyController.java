package org.example.board_cafe_kiosk_2603.controller.admin.policy;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.dto.admin.policy.PolicyDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageResponseDTO;
import org.example.board_cafe_kiosk_2603.service.admin.policy.PolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Log4j2
@Controller
@RequestMapping("/admin/policy")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER')")
public class PolicyController {

    private final PolicyService policyService;

    // ===========================================================
    // 페이지
    // ===========================================================
    @GetMapping
    public String policyPage(PageRequestDTO pageRequestDTO, Model model) {
        PageResponseDTO<PolicyDTO> responseDTO = policyService.selectPagedPolicies(pageRequestDTO);

        // 각 탭별 개수 조회
        PageRequestDTO allReq      = PageRequestDTO.builder().page(1).size(1).build();
        PageRequestDTO activeReq   = PageRequestDTO.builder().page(1).size(1).filter("active").build();
        PageRequestDTO inactiveReq = PageRequestDTO.builder().page(1).size(1).filter("inactive").build();

        model.addAttribute("responseDTO", responseDTO);
        model.addAttribute("pageRequestDTO", pageRequestDTO);
        // filter가 null이면 "all"로 기본값 설정
        model.addAttribute("filter", pageRequestDTO.getFilter() != null ? pageRequestDTO.getFilter() : "all");
        model.addAttribute("countAll",      policyService.getCount(allReq));
        model.addAttribute("countActive",   policyService.getCount(activeReq));
        model.addAttribute("countInactive", policyService.getCount(inactiveReq));
        model.addAttribute("activePage", "policyManagement");

        return "admin/package";
    }

    // 패키지 등록
    @PostMapping("/insert")
    @ResponseBody
    public ResponseEntity<PolicyDTO> insert(@RequestBody PolicyDTO dto) {
        try {
            policyService.insert(dto);
            log.info("패키지 등록 완료 - name: {}", dto.getName());
            return ResponseEntity.ok(PolicyDTO.success("패키지가 등록되었습니다."));
        } catch (Exception e) {
            log.error("패키지 등록 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(PolicyDTO.fail("패키지 등록에 실패했습니다."));
        }
    }

    // 활성/비활성 토글
    @PostMapping("/status")
    @ResponseBody
    public ResponseEntity<PolicyDTO> updateStatus(@RequestBody Map<String, Object> req) {
        try {
            int id       = ((Number) req.get("id")).intValue();
            boolean active = (Boolean) req.get("active");
            policyService.updateStatus(id, active);
            log.info("패키지 상태 변경 - id: {}, active: {}", id, active);
            return ResponseEntity.ok(PolicyDTO.success(active ? "활성화되었습니다." : "비활성화되었습니다."));
        } catch (Exception e) {
            log.error("패키지 상태 변경 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(PolicyDTO.fail("상태 변경에 실패했습니다."));
        }
    }
}
