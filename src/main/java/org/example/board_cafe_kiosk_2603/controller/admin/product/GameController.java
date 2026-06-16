package org.example.board_cafe_kiosk_2603.controller.admin.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.product.CategoryType;
import org.example.board_cafe_kiosk_2603.dto.admin.product.*;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageResponseDTO;
import org.example.board_cafe_kiosk_2603.service.admin.product.CategoryService;
import org.example.board_cafe_kiosk_2603.service.admin.product.GameItemService;
import org.example.board_cafe_kiosk_2603.service.admin.product.GameService;
import org.example.board_cafe_kiosk_2603.util.FileUploadUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Log4j2
@Controller
@RequestMapping("/admin/product/game")
@RequiredArgsConstructor
public class GameController {
    /* 게임 CRUD 컨트롤러 */
    // 게임 목록 조회 (필터 포함)
    // 등록/수정/삭제
    // 재고(GameItem) 연동관리
    // 화면 노출(활성/비활성) 상태 변경

    private final CategoryService categoryService;
    private final GameService gameService;
    private final GameItemService gameItemService;
    private final FileUploadUtil fileUploadUtil;

    /* 게임 전체 목록 조회 */
    // 카테고리별 필터링 기능 포함
    @GetMapping
    public String getAll(@RequestParam(required = false) Integer categoryId,
                         @RequestParam(required = false) String tab,
                         PageRequestDTO pageRequestDTO,
                         Model model) {
        log.info("--- 게임 목록 조회 : (categoryId: {}, tab: {}, page: {}) ---", categoryId, tab, pageRequestDTO.getPage());

        try {
            // 카테고리 목록
            List<CategoryResponseDTO> categoryList = categoryService.getByType(CategoryType.GAME);
            model.addAttribute("categoryList", categoryList);
            model.addAttribute("selectedCategoryId", categoryId);
            model.addAttribute("pageRequestDTO", pageRequestDTO);
            model.addAttribute("activePage", "productReg");
            model.addAttribute("currentPage", "game");  // html tab 분기 인식에 사용

            // 숨김 탭
            if ("hidden".equals(tab)) {
//                model.addAttribute("activeTab", "hidden");
//
//                List<GameResponseDTO> gameList = gameService.getByIsActive(false);
//
//                // 카테고리 필터 적용
//                if (categoryId != null) {
//                    gameList = gameList.stream()
//                            .filter(g -> g.getCategoryId() == categoryId)
//                            .collect(Collectors.toList());
//                }
//
//                model.addAttribute("gameList", gameList);
//                log.info("숨김 게임 목록 조회 성공 - 건수: {}", gameList.size());

                model.addAttribute("activeTab", "hidden");

                PageResponseDTO<GameResponseDTO> hiddenPageResponse =
                        gameService.getByIsActive(false, categoryId, pageRequestDTO);

                model.addAttribute("hiddenPageResponse", hiddenPageResponse);
                log.info("숨김 게임 목록 조회 성공 - 건수: {}, 전체: {}", hiddenPageResponse.getDtoList().size(), hiddenPageResponse.getTotal());

                // 일반 탭
            } else {
                model.addAttribute("activeTab", "game");

//                PageResponseDTO<GameResponseDTO> pageResponse = (categoryId != null)
//                        ? gameService.getByCategoryId(categoryId, pageRequestDTO)
//                        : gameService.getAll(pageRequestDTO);
                // 서비스 인터페이스의 getByIsActive(boolean, Integer, PageRequestDTO)를 활용
                PageResponseDTO<GameResponseDTO> pageResponse =
                        gameService.getByIsActive(true, categoryId, pageRequestDTO);

                model.addAttribute("pageResponse", pageResponse);
                log.info("게임 목록 조회 성공 - 건수: {}, 전체: {}", pageResponse.getDtoList().size(), pageResponse.getTotal());
            }

        } catch (Exception e) {
            log.error("--- 게임 목록 조회 중 오류 발생: {} ---", e.getMessage());
        }
        return "admin/product_game";
    }

    /* 게임 등록 페이지 */
    @GetMapping("/add")
    public String addForm(Model model) {
        log.info("--- 게임 등록 폼 요청 ---");
        List<CategoryResponseDTO> categoryList = categoryService.getByType(CategoryType.GAME);
        model.addAttribute("categoryList", categoryList);
        model.addAttribute("activePage", "productReg");
        return "admin/product_game_form";
    }

    // 이미지 업로드 + 신규 game_item 등록
    /* 게임 및 개별 재고(Item) 등록 처리 */
    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    @Transactional(rollbackFor = Exception.class)
    public String register(@ModelAttribute GameRequestDTO gameRequestDTO,
                           @RequestParam(value = "imageFile", required = false) MultipartFile imageFile)
            throws IOException {
        log.info("--- 게임 신규 등록 시작, gameRequestDTO: {} ---", gameRequestDTO);

        // 이미지 저장
        // 새 이미지 파일이 있으면 저장 후 imageUrl 세팅
        // 없으면 hidden name="imageUrl"로 넘어온 기존 값 유지
        if (imageFile != null && !imageFile.isEmpty()) {
            gameRequestDTO.setImageUrl(fileUploadUtil.save(imageFile));
            log.debug("--- 게임 이미지 저장 완료: {} ---", gameRequestDTO.getImageUrl());
        }

        // 게임 등록 (insert 후 gameRequestDTO.getId()에 생성된 PK 세팅됨)
        // 생성된 PK 반환
        int gameId = gameService.register(gameRequestDTO);  // game 등록 후 PK 반환
        log.debug("게임 등록 완료 - gameId: {}", gameId);

        // 신규 game_item 등록
        if (gameRequestDTO.getNewItems() != null) {
            for (GameItemRequestDTO item : gameRequestDTO.getNewItems()) {
                if (item.getSerialNumber() != null && !item.getSerialNumber().isEmpty()) {
                    item.setGameId(gameId);  // gameId 세팅
                    gameItemService.register(item);
                    log.debug("game_item 등록 완료 - serialNumber: {}", item.getSerialNumber());
                }
            }
        }
        log.info("--- 전체 게임 등록 프로세스 완료 ---");
        return "redirect:/admin/product/game";
    }

    /* 게임 수정 페이지 이동 */
    // 기존 이미지 및 game_item 목록 포함
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable int id, Model model) {
        log.info("--- 게임 수정 폼 요청 (ID: {}) ---", id);

        GameResponseDTO game = gameService.getById(id);
        List<CategoryResponseDTO> categoryList = categoryService.getByType(CategoryType.GAME);
        List<GameItemResponseDTO> gameItemList = gameItemService.getByGameId(id);

        model.addAttribute("game", game);
        model.addAttribute("categoryList", categoryList);
        model.addAttribute("gameItemList", gameItemList);
        model.addAttribute("activePage", "productReg");

        log.info("게임 수정 폼 로드 완료 - gameId: {}, 재고수: {}", id, gameItemList.size());
        return "admin/product_game_form";
    }

    /* 게임 정보 수정(이미지 교체) 및 재고(game_item) 변경/삭제 처리 */
    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    @Transactional(rollbackFor = Exception.class)
    public String modify(@PathVariable int id,
                         @ModelAttribute GameRequestDTO gameRequestDTO,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                         @RequestParam(value = "deletedItemIds", required = false, defaultValue = "") String deletedItemIds)
            throws IOException {
        log.info("--- 게임 정보 수정 시작 (ID: {}, gameRequestDTO: {}) ---", id, gameRequestDTO);

        // 새 이미지 업로드 시 기존 이미지 삭제 후 교체
        // 없으면 hidden name="imageUrl"로 넘어온 기존 값이 dto에 자동 바인딩
        if (imageFile != null && !imageFile.isEmpty()) {
            // 기존 이미지 삭제 후 새 이미지 저장
            fileUploadUtil.delete(gameService.getById(id).getImageUrl());
            gameRequestDTO.setImageUrl(fileUploadUtil.save(imageFile));
            log.debug("게임 이미지 교체 완료: {}", gameRequestDTO.getImageUrl());
        }  // 새파일 없음 → hidden name="imageUrl" 값이 DTO에 이미 바인딩되어 있으므로 그대로 사용

        // 게임 기본 정보 수정
        gameService.modify(id, gameRequestDTO);
        log.info("게임 수정 완료 - id: {}", id);

        // 기존 game_item 수정
        if (gameRequestDTO.getItems() != null) {
            for (GameItemRequestDTO item : gameRequestDTO.getItems()) {
                if (item.getId() != 0) {
                    validateItemOwnership(id, item.getId());
                    item.setGameId(id);
                    gameItemService.modify(item.getId(), item);
                    log.debug("game_item 수정 완료 - itemId: {}", item.getId());
                }
            }
        }

        // 신규 game_item 등록
        if (gameRequestDTO.getNewItems() != null) {
            for (GameItemRequestDTO newItem : gameRequestDTO.getNewItems()) {
                if (newItem.getSerialNumber() != null && !newItem.getSerialNumber().isEmpty()) {
                    newItem.setGameId(id);
                    gameItemService.register(newItem);
                    log.debug("game_item 신규 등록 완료 - serialNumber: {}", newItem.getSerialNumber());
                }
            }
        }

        // game_item 삭제 (콤마 구분 id 목록)
        if (!deletedItemIds.isEmpty()) {
            for (String itemIdStr : deletedItemIds.split(",")) {
                String trimmedId = itemIdStr.trim();
                if (trimmedId.isEmpty()) {
                    continue;
                }
                int itemId = Integer.parseInt(trimmedId);
                validateItemOwnership(id, itemId);
                gameItemService.remove(itemId);
                log.debug("game_item 삭제 완료 - itemId: {}", itemId);
            }
        }
        return "redirect:/admin/product/game";
    }

    private void validateItemOwnership(int gameId, int itemId) {
        GameItemResponseDTO targetItem = gameItemService.getById(itemId);
        if (targetItem.getGameId() != gameId) {
            throw new IllegalArgumentException(
                    "현재 게임(id=" + gameId + ")에 속하지 않은 재고(itemId=" + itemId + ")는 수정/삭제할 수 없습니다.");
        }
    }

    /* 게임 활성/비활성 상태 토글 (키오스크 노출 여부) */
    // 카테고리 필터 유지하며 리다리렉트
    @PostMapping("/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public String toggleActive(@PathVariable int id,
                               @RequestParam(required = false) Integer categoryId) {
        log.info("--- 게임 활성 상태 토글 (ID: {}) ---", id);

        // 토글 전 현재 상태 확인 (토글 후 확인하면 반대로 동작)
        boolean wasActive = gameService.getById(id).isActive();
        gameService.toggleActive(id);

        // 숨기기(활성 → 비활성): 게임 숨김 탭으로 이동
        if (wasActive) {
            return "redirect:/admin/product/game";
        }

        // 필터링 상태 유지를 위해 categoryId와 함께 리다이렉트
        return categoryId != null
                ? "redirect:/admin/product/game?categoryId=" + categoryId
                : "redirect:/admin/product/game";
    }
}