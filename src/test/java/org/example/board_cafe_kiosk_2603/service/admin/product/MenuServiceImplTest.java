package org.example.board_cafe_kiosk_2603.service.admin.product;

import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.dto.admin.product.MenuRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.product.MenuResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
class MenuServiceImplTest {

    @Autowired
    private MenuService menuService;

    @Test
    void getAllTest() {
        List<MenuResponseDTO> list = menuService.getAll();
        list.forEach(menu -> log.info(menu));
    }

    @Test
    void getByCategoryIdTest() {
        List<MenuResponseDTO> list = menuService.getByCategoryId(1);
        list.forEach(menu -> log.info(menu));
    }

    @Test
    void getByTypeTest() {
        List<MenuResponseDTO> list = menuService.getByType("DRINK");
        list.forEach(menu -> log.info(menu));
    }

    @Test
    void getByIsDeletedTest() {
        List<MenuResponseDTO> list = menuService.getByIsDeleted(false);
        list.forEach(menu -> log.info(menu));
    }

    @Test
    void getByIsAvailableTest() {
        List<MenuResponseDTO> list = menuService.getByIsAvailable(true);
        list.forEach(menu -> log.info(menu));
    }

    @Test
    void getByIdTest() {
        MenuResponseDTO menu = menuService.getById(1);
        log.info(menu);
    }

    @Test
    void registerTest() {
        MenuRequestDTO menuRequestDTO = MenuRequestDTO.builder()
                .categoryId(1)
                .name("테스트음료")
                .price(5000)
                .description("테스트용 음료입니다")
                .imageUrl("/images/test.jpg")
                .isAvailable(true)
                .build();
        menuService.register(menuRequestDTO);
        log.info("register 완료");
    }

    @Test
    void modifyTest() {
        MenuRequestDTO menuRequestDTO = MenuRequestDTO.builder()
                .categoryId(1)
                .name("수정된음료")
                .price(6000)
                .description("수정된 음료입니다")
                .imageUrl("/images/updated.jpg")
                .isAvailable(true)
                .build();
        menuService.modify(1, menuRequestDTO);
        log.info("modify 완료");
    }

    @Test
    void removeTest() {
        menuService.remove(1);
        log.info("remove(소프트삭제) 완료");
    }

    @Test
    void restoreTest() {
        menuService.restore(1);
        log.info("restore 완료");
    }

    @Test
    void toggleAvailableTest() {
        menuService.toggleAvailable(1);
        log.info("toggleAvailable 완료");
    }
}