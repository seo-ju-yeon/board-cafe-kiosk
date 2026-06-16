package org.example.board_cafe_kiosk_2603.controller.admin.point;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.dto.admin.point.PointAdminDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.point.PointHistoryDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageResponseDTO;
import org.example.board_cafe_kiosk_2603.service.admin.point.PointService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 포인트 관리 컨트롤러
 * URL: /admin/points/*
 */
@Log4j2
@Controller
@RequestMapping("/admin/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    /** 포인트 관리 페이지 */
//    @GetMapping
//    public String pointManagement(Model model) {
//        model.addAttribute("pointList",      pointService.getAllPoints());
//        model.addAttribute("totalCustomers", pointService.getTotalCustomers());
//        model.addAttribute("totalPoints",    pointService.getTotalPoints());
//        model.addAttribute("avgPoints",      pointService.getAvgPoints());
//        model.addAttribute("activePage",     "pointManagement");
//        return "admin/point";
//
//    }

    @GetMapping
    public String pointManagement() {
        return "redirect:/admin/points/list";
    }

    /** 포인트 이력 조회 (AJAX) */
    @GetMapping("/{pointId}/history")
    @ResponseBody
    public List<PointHistoryDTO> pointHistory(@PathVariable int pointId) {
        return pointService.getHistoryByPointId(pointId);
    }

    /* 페이징 처리된 포인트 목록 페이지 */
    @GetMapping("/list")
    public String list(PageRequestDTO pageRequestDTO, Model model) {
        log.info("포인트 페이징 목록 요청: " + pageRequestDTO);

        PageResponseDTO<PointAdminDTO> responseDTO = pointService.getPagedPoints(pageRequestDTO);

        model.addAttribute("responseDTO", responseDTO);

        log.info("responseDTO: " + responseDTO);
        model.addAttribute("pageRequestDTO", pageRequestDTO);

        // 3. 상단 대시보드용 통계 데이터
        model.addAttribute("totalCustomers", pointService.getTotalCustomers());
        model.addAttribute("totalPoints",    pointService.getTotalPoints());
        model.addAttribute("avgPoints",      pointService.getAvgPoints());
        model.addAttribute("activePage",     "pointManagement");

        return "admin/point";
    }
}
