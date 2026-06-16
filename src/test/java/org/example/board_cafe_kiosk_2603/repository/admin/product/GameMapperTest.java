package org.example.board_cafe_kiosk_2603.repository.admin.product;

import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.product.Game;
import org.example.board_cafe_kiosk_2603.dto.admin.product.GameResponseDTO;
import org.example.board_cafe_kiosk_2603.mapper.admin.product.GameMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

@Log4j2
@SpringBootTest
class GameMapperTest {

    @Autowired
    private GameMapper gameMapper;

    @Test
    void findAllTest() {
        List<GameResponseDTO> list = gameMapper.findAll();
        list.forEach(game -> log.info(game));
    }

    @Test
    void findByCategoryIdTest() {
        List<GameResponseDTO> list = gameMapper.findByCategoryId(1);
        list.forEach(game -> log.info(game));
    }

    @Test
    void findByIsActiveTest() {
        List<GameResponseDTO> list = gameMapper.findByIsActive(true);
        list.forEach(game -> log.info(game));
    }

    @Test
    void findByIdTest() {
        Optional<GameResponseDTO> game = gameMapper.findById(1);
        log.info(game);
    }

    @Test
    void insertTest() {
        Game game = Game.builder()
                .categoryId(1)
                .name("테스트게임")
                .minPlayers(2)
                .maxPlayers(4)
                .playTime(30)
                .isActive(true)
                .build();
        int result = gameMapper.insert(game);
        log.info("insert 결과: " + result);
    }

    @Test
    void updateTest() {
        Game game = Game.builder()
                .id(1)
                .categoryId(1)
                .name("수정된게임")
                .minPlayers(2)
                .maxPlayers(6)
                .playTime(60)
                .isActive(true)
                .build();
        int result = gameMapper.update(game);
        log.info("update 결과: " + result);
    }

    @Test
    void deleteTest() {
        int result = gameMapper.delete(1);
        log.info("delete 결과: " + result);
    }

    @Test
    void toggleActiveTest() {
        int result = gameMapper.toggleActive(1);
        log.info("toggleActive 결과: " + result);
    }
}