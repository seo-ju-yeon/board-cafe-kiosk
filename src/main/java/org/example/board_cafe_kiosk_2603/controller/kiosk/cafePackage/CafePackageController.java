package org.example.board_cafe_kiosk_2603.controller.kiosk.cafePackage;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.common.cafeTableSession.CafeTableSession;
import org.example.board_cafe_kiosk_2603.service.admin.cafeTable.CafeTableService;
import org.example.board_cafe_kiosk_2603.service.admin.cafeTable.TableSessionAdminService;
import org.example.board_cafe_kiosk_2603.service.kiosk.cafePackage.CafePackageService;
import org.example.board_cafe_kiosk_2603.service.kiosk.tableSession.TableSessionKioskService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 패키지 선택 페이지 + REST API 컨트롤러.
 *
 * [페이지] GET  /kiosk/package_selection  → package_selection.html
 * [API]   POST /kiosk/package/select      → 패키지 선택 처리 (JSON)
 */
@Log4j2
@Controller
@RequestMapping("/kiosk")
@RequiredArgsConstructor
public class CafePackageController {

    private final CafePackageService cafePackageService;
    private final CafeTableService cafeTableService;
    private final TableSessionKioskService tableSessionKioskService;
    private final TableSessionAdminService tableSessionAdminService;

    // ===========================================================
    // 페이지
    // ===========================================================

    @GetMapping("/package_selection")
    public String packageSelectionPage(HttpSession session, Model model) {
        Integer tableNumber = (Integer) session.getAttribute("tableNumber");
        Integer partySize = (Integer) session.getAttribute("partySize");

        model.addAttribute("tableNumber", tableNumber);
        model.addAttribute("partySize", partySize);
        model.addAttribute("packageList", cafePackageService.getActivePackages());

        log.info("패키지 선택 화면 - 테이블: {}, 인원: {}", tableNumber, partySize);
        return "kiosk/package_selection";
    }

    // ===========================================================
    // REST API
    // ===========================================================

    @PostMapping("/package/select")
    @ResponseBody
    public Map<String, Object> selectPackage(
            @RequestBody Map<String, Object> req,
            HttpSession session) {

        int packageId = ((Number) req.get("packageId")).intValue();
        Integer tableId = (Integer) session.getAttribute("tableId");
        Integer tableNumber = (Integer) session.getAttribute("tableNumber");
        Integer partySize = (Integer) session.getAttribute("partySize");

        if (tableId == null || tableNumber == null || partySize == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", "세션 정보가 유효하지 않습니다. 다시 로그인해 주세요.");
            return error;
        }

        log.info("패키지 선택 요청 | tableId: {}, tableNumber: {}, packageId: {}, partySize: {}",
                tableId, tableNumber, packageId, partySize);

        var pkg = cafePackageService.getById(packageId);

        Map<String, Object> res = new LinkedHashMap<>();
        if (pkg == null) {
            res.put("success", false);
            res.put("message", "패키지를 찾을 수 없습니다.");
            return res;
        }

        // 활성 세션 있으면 DB 인원수로 HTTP 세션 덮어씌우기
        CafeTableSession activeSession = tableSessionAdminService.getActiveSession(tableId);

        if (activeSession != null) {
            // 기존 활성 세션 존재 -> DB 인원수로 덮어씌우기
            session.setAttribute("partySize", activeSession.getInitialGuestCnt());
            log.info("기존 활성 세션 존재 - DB 인원수로 덮어씌움: {}명",
                    activeSession.getInitialGuestCnt());
        } else {
            // 활성 세션 없으면 새로 생성 후 cafe_table 동기화
            Long newSessionId = tableSessionKioskService.createSession(tableId, packageId, partySize);
            cafeTableService.syncTableWithSession(tableId, newSessionId);
            log.info("table_session 생성 + cafe_table 동기화 완료 | tableId: {}, tableNumber: {}, sessionId: {}",
                    tableId, tableNumber, newSessionId);
        }

        // 세션에 패키지 정보 저장
        session.setAttribute("packageId",          pkg.getId());
        session.setAttribute("selectedPackageId",    pkg.getId());
        session.setAttribute("selectedPackageName",  pkg.getName());
        session.setAttribute("selectedPackagePrice", pkg.getBasePrice());
        session.setAttribute("sessionStartTime",     System.currentTimeMillis());
        session.setAttribute("durationMinutes", pkg.getDurationMinutes());

        log.info("패키지 선택 완료 - 테이블: {}, 패키지: {} ({}원)",
                session.getAttribute("tableNumber"), pkg.getName(), pkg.getBasePrice());

        res.put("success",     true);
        res.put("packageId",   pkg.getId());
        res.put("packageName", pkg.getName());
        res.put("basePrice",   pkg.getBasePrice());
        return res;
    }
}
