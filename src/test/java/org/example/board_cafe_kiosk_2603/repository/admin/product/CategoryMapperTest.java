package org.example.board_cafe_kiosk_2603.repository.admin.product;

import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.product.Category;
import org.example.board_cafe_kiosk_2603.domain.admin.product.CategoryType;
import org.example.board_cafe_kiosk_2603.mapper.admin.product.CategoryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;


@Log4j2
@SpringBootTest
class CategoryMapperTest {

    @Autowired
    private CategoryMapper categoryMapper;

    /* 전체 카테고리 목록 조회 */
    @Test
    void findAllTest() {
        List<Category> list = categoryMapper.findAll();
        log.info("=== 전체 카테고리 목록 ===");
        list.forEach(category -> log.info("전체 카테고리 목록: {}", category));
    }

    /* type 기준 카테고리 조회 */
    // GAME, FOOD, DRINK, GUEST
    @Test
    void findByTypeTest() {
        List<Category> list = categoryMapper.findByType(CategoryType.GAME);
        log.info("=== type 기준 카테고리 조회 ===");
        list.forEach(category -> log.info("type 기준 카테고리 조회: {}", category));
    }

    /* 카테고리 단건 조회 */
    @Test
    void findByIdTest() {
        // DB에 존재하는 id
        Optional<Category> category = categoryMapper.findById(1);
        log.info("카테고리 단건 조회: {}", category);
        // Optional[Category(id=1, name=커피·에스프레소, type=DRINK)]
    }

    /* 카테고리 - 등록 */
    // 새로운 카테고리 생성
//    @Test
//    void insertTest() {
//        Category category = Category.builder()
//                .name("테스트카테고리")
//                .type(CategoryType.GAME)
//                .build();
//        int result = categoryMapper.insert(category);
//        log.info("insert 결과: " + result);
//    }

    /* 카테고리 - 수정 */
    // 카테고리 이름 변경
//    @Test
//    void updateTest() {
//        Category category = Category.builder()
//                .id(1)
//                .name("수정된카테고리")
//                .type(CategoryType.FOOD)
//                .build();
//        int result = categoryMapper.update(category);
//        log.info("update 결과: " + result);
//    }

    /* 카테고리 - 삭제 */
//    @Test
//    void deleteTest() {
//        int result = categoryMapper.delete(1);
//        log.info("delete 결과: " + result);
//    }
}