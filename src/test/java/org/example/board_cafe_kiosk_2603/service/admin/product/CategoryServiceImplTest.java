package org.example.board_cafe_kiosk_2603.service.admin.product;

import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.product.CategoryType;
import org.example.board_cafe_kiosk_2603.dto.admin.product.CategoryRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.product.CategoryResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
class CategoryServiceImplTest {

    @Autowired
    private CategoryService categoryService;

    @Test
    void getAllTest() {
        List<CategoryResponseDTO> list = categoryService.getAll();
        list.forEach(category -> log.info(category));
    }

    @Test
    void getByTypeTest() {
        List<CategoryResponseDTO> list = categoryService.getByType(CategoryType.GAME);
        list.forEach(category -> log.info(category));
    }

    @Test
    void getByIdTest() {
        CategoryResponseDTO category = categoryService.getById(1);
        log.info(category);
    }

    @Test
    void registerTest() {
        CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                .name("테스트카테고리")
                .type(CategoryType.GAME)
                .build();
        categoryService.register(categoryRequestDTO);
        log.info("register 완료");
    }

    @Test
    void modifyTest() {
        CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                .name("수정된카테고리")
                .type(CategoryType.FOOD)
                .build();
        categoryService.modify(1, categoryRequestDTO);
        log.info("modify 완료");
    }

    @Test
    void removeTest() {
        categoryService.remove(1);
        log.info("remove 완료");
    }
}