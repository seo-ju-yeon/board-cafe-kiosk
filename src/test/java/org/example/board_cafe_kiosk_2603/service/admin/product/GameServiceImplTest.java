package org.example.board_cafe_kiosk_2603.service.admin.product;

import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.dto.admin.product.GameRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.product.GameResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
class GameServiceImplTest {

    @Autowired
    private GameService gameService;

    @Test
    void getAllTest() {
        List<GameResponseDTO> list = gameService.getAll();
        list.forEach(game -> log.info(game));
    }

    @Test
    void getByCategoryIdTest() {
        List<GameResponseDTO> list = gameService.getByCategoryId(1);
        list.forEach(game -> log.info(game));
    }

    @Test
    void getByIsActiveTest() {
        List<GameResponseDTO> list = gameService.getByIsActive(true);
        list.forEach(game -> log.info(game));
    }

    @Test
    void getByIdTest() {
        GameResponseDTO gameResponseDTO = gameService.getById(1);
        log.info(gameResponseDTO);
    }

    @Test
    void registerTest() {
        GameRequestDTO gameRequestDTO = GameRequestDTO.builder()
                .categoryId(1)
                .name("테스트게임")
                .minPlayers(2)
                .maxPlayers(4)
                .playTime(30)
                .isActive(true)
                .build();
        gameService.register(gameRequestDTO);
        log.info("register 완료");
    }

    @Test
    void modifyTest() {
        GameRequestDTO gameRequestDTO = GameRequestDTO.builder()
                .categoryId(1)
                .name("수정된게임")
                .minPlayers(2)
                .maxPlayers(6)
                .playTime(60)
                .isActive(true)
                .build();
        gameService.modify(1, gameRequestDTO);
        log.info("modify 완료");
    }

    @Test
    void removeTest() {
        gameService.remove(1);
        log.info("remove 완료");
    }

    @Test
    void toggleActiveTest() {
        gameService.toggleActive(1);
        log.info("toggleActive 완료");
    }
}