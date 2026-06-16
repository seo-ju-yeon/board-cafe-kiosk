package org.example.board_cafe_kiosk_2603.service.admin.product;

import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.product.GameItemStatus;
import org.example.board_cafe_kiosk_2603.dto.admin.product.GameItemRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.product.GameItemResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
class GameItemServiceImplTest {

    @Autowired
    private GameItemService gameItemService;

    @Test
    void getAllTest() {
        List<GameItemResponseDTO> list = gameItemService.getAll();
        list.forEach(item -> log.info(item));
    }

    @Test
    void getByGameIdTest() {
        List<GameItemResponseDTO> list = gameItemService.getByGameId(1);
        list.forEach(item -> log.info(item));
    }

    @Test
    void getByStatusTest() {
        List<GameItemResponseDTO> list = gameItemService.getByStatus(GameItemStatus.NORMAL);
        list.forEach(item -> log.info(item));
    }

    @Test
    void getByIdTest() {
        GameItemResponseDTO gameItemResponseDTO = gameItemService.getById(1);
        log.info(gameItemResponseDTO);
    }

    @Test
    void registerTest() {
        GameItemRequestDTO gameItemRequestDTO = GameItemRequestDTO.builder()
                .gameId(1)
                .serialNumber("SN-TEST-001")
                .status(GameItemStatus.NORMAL)
                .build();
        gameItemService.register(gameItemRequestDTO);
        log.info("register 완료");
    }

    @Test
    void modifyTest() {
        GameItemRequestDTO gameItemRequestDTO = GameItemRequestDTO.builder()
                .gameId(1)
                .serialNumber("SN-TEST-002")
                .status(GameItemStatus.RENTED)
                .build();
        gameItemService.modify(1, gameItemRequestDTO);
        log.info("modify 완료");
    }

    @Test
    void removeTest() {
        String serial = "SN-REMOVE-POLICY-" + System.currentTimeMillis();

        GameItemRequestDTO gameItemRequestDTO = GameItemRequestDTO.builder()
                .gameId(1)
                .serialNumber(serial)
                .status(GameItemStatus.NORMAL)
                .build();
        gameItemService.register(gameItemRequestDTO);

        GameItemResponseDTO created = gameItemService.getAll().stream()
                .filter(item -> serial.equals(item.getSerialNumber()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("방금 등록한 테스트 아이템을 찾지 못했습니다."));

        assertThrows(IllegalStateException.class, () -> gameItemService.remove(created.getId()));

        gameItemService.changeStatus(created.getId(), GameItemStatus.DAMAGED);
        assertDoesNotThrow(() -> gameItemService.remove(created.getId()));
        log.info("remove 정책 테스트 완료 - itemId: {}", created.getId());
    }

    @Test
    void changeStatusTest() {
        gameItemService.changeStatus(1, GameItemStatus.DAMAGED);
        log.info("changeStatus 완료");
    }
}
