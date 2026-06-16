package org.example.board_cafe_kiosk_2603.service.admin.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.product.Category;
import org.example.board_cafe_kiosk_2603.domain.admin.product.CategoryType;
import org.example.board_cafe_kiosk_2603.dto.admin.product.CategoryRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.product.CategoryResponseDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageResponseDTO;
import org.example.board_cafe_kiosk_2603.mapper.admin.product.CategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Log4j2
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    /* 카테고리 전체 목록 조회 */
    @Override
    public List<CategoryResponseDTO> getAll() {
        List<CategoryResponseDTO> list = categoryMapper.findAll();
        log.info("--- 카테고리 목록 조회: {} ---", list.size());
        return list;
    }

    /* type 기준 카테고리 목록 조회 */
    @Override
    public List<CategoryResponseDTO> getByType(CategoryType type) {
        List<CategoryResponseDTO> list = categoryMapper.findByType(type);
        log.info("--- 카테고리 타입 조회 (type={}): {} ---", type, list.size());
        return list;
    }

    /* PK로 카테고리 단건 조회 */
    @Override
    public CategoryResponseDTO getById(int id) {
        return categoryMapper.findById(id)
                .orElseThrow(() -> {
                    log.warn("카테고리 단건 조회 실패 - id: {}", id);
                    return new NoSuchElementException("카테고리를 찾을 수 없습니다. id=" + id);
                });
    }

    /* 카테고리 등록 */
    @Override
    @Transactional
    public void register(CategoryRequestDTO categoryRequestDTO) {
        Category category = Category.builder()
                .name(categoryRequestDTO.getName())
                .type(categoryRequestDTO.getType())
                .build();
        int result = categoryMapper.insert(category);
        // useGeneratedKeys=true → insert 후 category.getId()에 PK 자동 주입
        log.info("[카테고리 등록 완료] name={}, type={}, 생성된 id={}, affected rows={}",
                categoryRequestDTO.getName(), categoryRequestDTO.getType(), category.getId(), result);
    }


    /* 카테고리 수정 */
    @Override
    @Transactional
    public void modify(int id, CategoryRequestDTO categoryRequestDTO) {
        categoryMapper.findById(id)
                .orElseThrow(() -> {
                    log.warn("--- 카테고리 수정 실패, id: {} ---", id);
                    return new NoSuchElementException("카테고리를 찾을 수 없습니다. id=" + id);
                });

        // Mapper 시그니처: update(@Param("id") int id, @Param("dto") CategoryRequestDTO dto)
        // Category VO가 아닌 id + RequestDTO 를 분리해서 전달
        int result = categoryMapper.update(id, categoryRequestDTO);
        log.info("--- 카테고리 수정 완료, id: {}, affected rows: {} ---", id, result);
    }

    /* 카테고리 삭제 */
    // remove() 삭제 전 연결 상품 수 검증 추가
    // 연결된 상품이 1개 이상 존재할 경우 IllegalStateException 발생
    // Controller에서 이 예외를 잡아 클라이언트에 적절한 응답 반환
    @Override
    @Transactional
    public void remove(int id) {
        // 삭제 대상 존재 여부 검증
        CategoryResponseDTO existing = categoryMapper.findById(id)
                .orElseThrow(() -> {
                    log.warn("--- 카테고리 삭제 실패 (삭제 대상 없음), id: {} ---", id);
                    return new NoSuchElementException("카테고리를 찾을 수 없습니다. id=" + id);
                });

        // 연결 상품 수 확인 → 존재하면 삭제 불가
        int linkedCount = categoryMapper.countLinkedProducts(id);
        log.info("--- 카테고리 삭제 검증, (id: {}, name: {}, 연결 상품 수: {}) ---",
                id, existing.getName(), linkedCount);

        if (linkedCount > 0) {
            log.warn("--- 카테고리 삭제 거부, (id: {}, name: {}, 연결 상품: {}) ---",
                    id, existing.getName(), linkedCount);
            throw new IllegalStateException(
                    "연결된 상품이 " + linkedCount + "개 있어 삭제할 수 없습니다. (카테고리: " + existing.getName() + ")");
        }

        int result = categoryMapper.delete(id);
        log.debug("--- 카테고리 삭제 완료, (id: {}, name: {}, affected rows: {}) ---",
                id, existing.getName(), result);
    }

    /* 삭제 가능 여부 확인 */
    // 연결 상품 수가 0이면 true
    @Override
    public boolean canDelete(int id) {
        int linkedCount = categoryMapper.countLinkedProducts(id);
        log.info("--- 삭제 가능 여부 (id: {}, 연결 상품 수: {}, canDelete: {}) ---", id, linkedCount, linkedCount == 0);
        return linkedCount == 0;
    }

    /* 전체 카테고리 목록 - 페이징 */
    @Override
    public PageResponseDTO<CategoryResponseDTO> getAll(PageRequestDTO pageRequestDTO) {
        log.info("--- 카테고리 페이징 실행 ---");
        List<CategoryResponseDTO> list = categoryMapper.findAllPaged(pageRequestDTO);
        int total = categoryMapper.countAll();
        log.info("조회된 카테고리 수: {}, 전체: {}", list.size(), total);
        return PageResponseDTO.<CategoryResponseDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(list)
                .total(total)
                .build();
    }
}