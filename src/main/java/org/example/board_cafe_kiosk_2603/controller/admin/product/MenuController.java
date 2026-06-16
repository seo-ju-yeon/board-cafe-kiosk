package org.example.board_cafe_kiosk_2603.controller.admin.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.product.CategoryType;
import org.example.board_cafe_kiosk_2603.dto.admin.product.CategoryResponseDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.product.MenuRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.product.MenuResponseDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageResponseDTO;
import org.example.board_cafe_kiosk_2603.service.admin.product.CategoryService;
import org.example.board_cafe_kiosk_2603.service.admin.product.MenuService;
import org.example.board_cafe_kiosk_2603.util.FileUploadUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Log4j2
@Controller
@RequestMapping("/admin/product/menu")
@RequiredArgsConstructor
public class MenuController {
    /* 메뉴 CRUD 컨트롤러 */
    // 탭별 목록 조회
    // 메뉴 등록/수정
    // 소프트 삭제(숨김) 및 복원
    // 품절 상태 관리

    private final MenuService menuService;
    private final CategoryService categoryService;
    private final FileUploadUtil fileUploadUtil;

    /* 메뉴 목록 조회 */
    // tab 파라미터(food, drink, guest, hidden)에 따라 데이터 분기 처리
    // 페이징 포함
    @GetMapping
    public String getList(@RequestParam(defaultValue = "food") String tab,
                          @RequestParam(required = false) Integer categoryId,
                          PageRequestDTO pageRequestDTO,
                          Model model) {
        log.info("--- 메뉴 목록 조회 요청 (Tab: {}, CategoryID Filter: {}, page: {}) ---", tab, categoryId, pageRequestDTO.getPage());

        // 탭 기준으로 기본 목록 조회
        // (소프트 삭제된 데이터는 hidden 탭에서만 조회)
        // 1. 서비스의 페이징 메서드 호출로 변경 (PageResponseDTO를 직접 받음)
        PageResponseDTO<MenuResponseDTO> pageResponse = switch (tab) {
            case "drink" -> (categoryId != null)
                    ? menuService.getByTypeAndCategoryId("DRINK", categoryId, pageRequestDTO)
                    : menuService.getByType("DRINK", pageRequestDTO);
            case "guest" -> (categoryId != null)
                    ? menuService.getByTypeAndCategoryId("GUEST", categoryId, pageRequestDTO)
                    : menuService.getByType("GUEST", pageRequestDTO);
//            case "hidden" -> menuService.getByIsDeleted(true, pageRequestDTO);
            case "hidden" -> (categoryId != null)
                    ? menuService.getByIsDeletedAndCategoryId(true, categoryId, pageRequestDTO)
                    : menuService.getByIsDeleted(true, pageRequestDTO);
            default -> (categoryId != null)
                    ? menuService.getByTypeAndCategoryId("FOOD", categoryId, pageRequestDTO)
                    : menuService.getByType("FOOD", pageRequestDTO);
        };

        // 2. 카테고리 목록 로직 (hidden 탭도 카테고리 목록 제공)
        List<CategoryResponseDTO> categoryList;
        if ("hidden".equals(tab)) {
            categoryList = categoryService.getAll().stream()
                    .filter(c -> List.of(CategoryType.FOOD, CategoryType.DRINK, CategoryType.GUEST).contains(c.getType()))
                    .toList();
        } else {
            CategoryType type = CategoryType.valueOf(tab.toUpperCase());
            categoryList = categoryService.getByType(type);
        }

        model.addAttribute("pageResponse", pageResponse); // HTML 페이징 에러 해결
        model.addAttribute("menuList", pageResponse.getDtoList()); // 기존 목록 호환
        model.addAttribute("categoryList", categoryList);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("activeTab", tab);
        model.addAttribute("activePage", "productReg");
        model.addAttribute("currentPage", "menu");  // html tab 분기 인식에 사용

        log.info("--- 메뉴 목록 조회 완료 - tab: {}, categoryId: {}, 건수: {}", tab, categoryId, pageResponse.getTotal());
        return "admin/product_menu";
    }

    /* 메뉴 등록 페이지 이동 */
    @GetMapping("/add")
    public String addForm(@RequestParam(defaultValue = "food") String tab, Model model) {
        log.info("--- 메뉴 등록 폼 요청 (Tab: {}) ---", tab);

        // 게임을 제외한 메뉴 관련 카테고리 타입들 정의
//        List<CategoryResponseDTO> categoryList = categoryService.getAll();
        List<CategoryType> menuTypes = List.of(CategoryType.FOOD, CategoryType.DRINK, CategoryType.GUEST);
        // 정의한 타입에 해당하는 카테고리만 수집
        List<CategoryResponseDTO> categoryList = categoryService.getAll().stream()
                .filter(categoryResponseDTO -> menuTypes.contains(categoryResponseDTO.getType()))
                .toList();
        model.addAttribute("categoryList", categoryList);
        model.addAttribute("activeTab", tab);
        model.addAttribute("activePage", "productReg");

        return "admin/product_menu_form";
    }

    /* 메뉴 등록 처리 (이미지 업로드 포함) */
    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public String register(@ModelAttribute MenuRequestDTO menuRequestDTO,
                           @RequestParam(defaultValue = "imageFile", required = false) MultipartFile imageFile,
                           @RequestParam(defaultValue = "food") String tab) throws IOException {
        log.info("--- 메뉴 신규 등록 시작 (Tab: {}) ---", tab);
        log.debug("--- 요청 데이터: {} ---", menuRequestDTO);

        // 이미지 파일 저장 처리
        String imageUrl = fileUploadUtil.save(imageFile);
        if (imageUrl != null) {
            menuRequestDTO.setImageUrl(imageUrl);
            log.debug("메뉴 이미지 저장 완료: {}", imageUrl);
        }
        menuService.register(menuRequestDTO);
        log.info("--- 메뉴 등록 완료 ---");
        return "redirect:/admin/product/menu?tab=" + tab;
    }

    // 기존 이미지 표시
    /* 메뉴 수정 페이지 이동 */
    // 기존 이미지가 있는 경우, 이미지를 수정하지 않았을 때 기존 이미지 보존
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable int id,
                           @RequestParam(defaultValue = "food") String tab, Model model) {
        log.info("--- 메뉴 수정 폼 요청 (MenuID: {}, Tab: {}) ---", id, tab);

        MenuResponseDTO menu = menuService.getById(id);

        // 게임을 제외한 메뉴 관련 카테고리 타입들 정의
//        List<CategoryResponseDTO> categoryList = categoryService.getAll();
        List<CategoryType> menuTypes = List.of(CategoryType.FOOD, CategoryType.DRINK, CategoryType.GUEST);
        // 정의한 타입에 해당하는 카테고리만 수집
        List<CategoryResponseDTO> categoryList = categoryService.getAll().stream()
                .filter(categoryResponseDTO -> menuTypes.contains(categoryResponseDTO.getType()))
                .toList();

        model.addAttribute("menu", menu);
        model.addAttribute("categoryList", categoryList);
        model.addAttribute("activeTab", tab);
        model.addAttribute("activePage", "productReg");

        log.info("--- 수정 폼 데이터 로드 완료 (메뉴명: {}) ---", menu.getName());
        return "admin/product_menu_form";
    }

    /* 메뉴 정보 수정 처리 (이미지 교체 로직 포함) */
    // 새 이미지 업로드 시 기존 이미지 교체
    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public String modify(@PathVariable int id,
                         @ModelAttribute MenuRequestDTO menuRequestDTO,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                         @RequestParam(defaultValue = "food") String tab) throws IOException {
        log.info("--- 메뉴 수정 요청 (MenuID: {}, Tab: {}) ---", id, tab);

        // 새 이미지 업로드 시 기존 이미지 삭제 후 교체
        if (imageFile != null && !imageFile.isEmpty()) {
            // 변수 선언 없이 메서드 체이닝으로 기존 imageUrl 사용
            fileUploadUtil.delete(menuService.getById(id).getImageUrl());
            log.info("기존 이미지 삭제 완료");
            menuRequestDTO.setImageUrl(fileUploadUtil.save(imageFile));
            log.debug("메뉴 이미지 교체 완료: {}", menuRequestDTO.getImageUrl());
        }
        menuService.modify(id, menuRequestDTO);
        log.info("--- 메뉴 수정 완료 (id: {}) ---", id);
        return "redirect:/admin/product/menu?tab=" + tab;
    }

    /* 메뉴 숨김 처리 (소프트 삭제) */
    @PostMapping("/{id}/toggle-hide")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public String toggleHide(@PathVariable int id,
                             @RequestParam(defaultValue = "food") String tab) {
        log.info("--- 메뉴 숨김(소프트 삭제) 요청 (MenuID: {}, Tab: {}) ---", id, tab);

        menuService.remove(id);
        log.info("--- 메뉴 숨김 완료 (id: {}) ---", id);
        return "redirect:/admin/product/menu?tab=" + tab;
    }

    /* 메뉴 품절 상태 토글 */
    @PostMapping("/{id}/toggle-soldout")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public String toggleSoldout(@PathVariable int id,
                                @RequestParam(defaultValue = "food") String tab) {
        log.info("--- 메뉴 품절 상태 토글 요청 (MenuID: {}, Tab: {}) ---", id, tab);
        menuService.toggleAvailable(id);

        log.info("--- 판매 상태 변경 완료 (id: {}) ---", id);
        return "redirect:/admin/product/menu?tab=" + tab;
    }

    /* 숨김 메뉴 복원 (소프트 삭제 취소) */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public String restore(@PathVariable int id) {
        log.info("--- 숨김 메뉴 복원 요청 (MenuID: {}) ---", id);
        menuService.restore(id);
        log.debug("--- 메뉴 복원 완료 (id: {}) ---", id);
        return "redirect:/admin/product/menu?tab=hidden";
    }
}