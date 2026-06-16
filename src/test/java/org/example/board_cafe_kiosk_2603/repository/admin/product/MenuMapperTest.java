package org.example.board_cafe_kiosk_2603.repository.admin.product;

import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.product.Menu;
import org.example.board_cafe_kiosk_2603.dto.admin.product.MenuResponseDTO;
import org.example.board_cafe_kiosk_2603.mapper.admin.product.MenuMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

@Log4j2
@SpringBootTest
class MenuMapperTest {

    @Autowired
    private MenuMapper menuMapper;

    @Test
    void findAllTest() {
        List<MenuResponseDTO> menuResponseDTOList = menuMapper.findAll();
        menuResponseDTOList.forEach(menu -> log.info(menu));
    }

    @Test
    void findByCategoryIdTest() {
        List<MenuResponseDTO> list = menuMapper.findByCategoryId(1);
        list.forEach(menu -> log.info(menu));
    }

    @Test
    void findByTypeTest() {
        List<MenuResponseDTO> list = menuMapper.findByType("DRINK");
        list.forEach(menu -> log.info(menu));
    }

    @Test
    void findByIsAvailableTest() {
        List<MenuResponseDTO> list = menuMapper.findByIsAvailable(true);
        list.forEach(menu -> log.info(menu));
    }

    @Test
    void findByIsDeletedTest() {
        List<MenuResponseDTO> list = menuMapper.findByIsDeleted(false);
        list.forEach(menu -> log.info(menu));
    }

    @Test
    void findByIdIncludeDeletedTest() {
        Optional<MenuResponseDTO> menu = menuMapper.findByIdIncludeDeleted(1);
        log.info(menu);
    }

    @Test
    void insertTest() {
        Menu menu = Menu.builder()
            .categoryId(1)
            .name("테스트음료")
            .price(5000)
            .description("테스트용 음료입니다")
            .imageUrl("/images/test.jpg")
            .isAvailable(true)
            .build();
        int result = menuMapper.insert(menu);
        log.info("insert 결과: " + result);
    }

    @Test
    void updateTest() {
        Menu menu = Menu.builder()
                .id(1)
                .categoryId(1)
                .name("수정된음료")
                .price(6000)
                .description("수정된 음료입니다")
                .imageUrl("/images/updated.jpg")
                .isAvailable(true)
                .build();
        int result = menuMapper.update(menu);
        log.info("update 결과: " + result);
    }

    @Test
    void softDeleteTest() {
        int result = menuMapper.softDelete(1);
        log.info("softDelete 결과: " + result);
    }

    @Test
    void restoreTest() {
        int result = menuMapper.restore(1);
        log.info("restore 결과: " + result);
    }

    @Test
    void toggleAvailableTest() {
        int result = menuMapper.toggleAvailable(1);
        log.info("toggleAvailable 결과: " + result);
    }
}