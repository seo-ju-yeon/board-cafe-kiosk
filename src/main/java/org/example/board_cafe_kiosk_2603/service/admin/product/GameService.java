package org.example.board_cafe_kiosk_2603.service.admin.product;


import org.example.board_cafe_kiosk_2603.dto.admin.product.GameRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.product.GameResponseDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageResponseDTO;

import java.util.List;

/**
 * Game 비즈니스 로직 인터페이스
 */
public interface GameService {
    /** 전체 게임 목록 반환 */
    List<GameResponseDTO> getAll();

    /** category_id 기준 게임 목록 반환 */
    List<GameResponseDTO> getByCategoryId(int categoryId);

    /** 활성 여부 기준 게임 목록 반환 */
    List<GameResponseDTO> getByIsActive(boolean isActive);

    /** PK로 게임 단건 반환 */
    GameResponseDTO getById(int id);

    /** 게임명 리스트로 게임 상세 조회 */
    List<GameResponseDTO> getByNames(List<String> names);

    /** 게임 등록 */
    int register(GameRequestDTO gameRequestDTO);

    /** 게임 수정 */
    void modify(int id, GameRequestDTO gameRequestDTO);

    /** 게임 삭제 */
    void remove(int id);

    /** 게임 활성 상태 토글 */
    void toggleActive(int id);

    /*============페이지=============*/
    /** 전체 게임 목록 - 페이징 */
    PageResponseDTO<GameResponseDTO> getAll(PageRequestDTO pageRequestDTO);

    /** category_id 기준 게임 목록 - 페이징 */
    PageResponseDTO<GameResponseDTO> getByCategoryId(int categoryId, PageRequestDTO pageRequestDTO);

    /** 활성 여부 기준 게임 목록 - 페이징 */
    PageResponseDTO<GameResponseDTO> getByIsActive(boolean isActive, Integer categoryId, PageRequestDTO pageRequestDTO);
}
