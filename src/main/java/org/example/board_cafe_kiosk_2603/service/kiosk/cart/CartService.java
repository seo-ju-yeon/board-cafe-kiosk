package org.example.board_cafe_kiosk_2603.service.kiosk.cart;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.kiosk.cart.Cart;
import org.example.board_cafe_kiosk_2603.domain.kiosk.cart.CartItem;
import org.example.board_cafe_kiosk_2603.dto.kiosk.cart.CartDTO;
import org.example.board_cafe_kiosk_2603.dto.kiosk.cart.CartItemDTO;
import org.example.board_cafe_kiosk_2603.mapper.kiosk.cart.CartItemMapper;
import org.example.board_cafe_kiosk_2603.mapper.kiosk.cart.CartMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartMapper     cartMapper;
    private final CartItemMapper cartItemMapper;

    // ===================================================
    // 장바구니 조회
    // ===================================================

    public CartDTO getCart(int tableNumber) {
        try {
            int tableId = resolveTableId(tableNumber);
            Cart cart = cartMapper.findByTableId(tableId);
            if (cart == null) {
                return CartDTO.builder()
                        .success(true)
                        .cartItems(Collections.emptyList())
                        .build();
            }

            List<CartItemDTO> itemDTOs = cartItemMapper.findByCartId(cart.getId()).stream()
                    .map(i -> CartItemDTO.builder()
                            .id(i.getId())
                            .cartId(i.getCartId())
                            .menuId(i.getMenuId())
                            .menuName(i.getMenuName())
                            .menuPrice(i.getMenuPrice())
                            .quantity(i.getQuantity())
                            .build())
                    .collect(Collectors.toList());
            int total = itemDTOs.stream().mapToInt(i -> i.getMenuPrice() * i.getQuantity()).sum();

            return CartDTO.builder()
                    .success(true)
                    .cartItems(itemDTOs)
                    .totalPrice(total)
                    .cartCount(itemDTOs.size())
                    .build();

        } catch (Exception e) {
            log.warn("장바구니 조회 실패 - tableNumber: {}, 원인: {}", tableNumber, e.getMessage());
            return CartDTO.builder()
                    .success(false)
                    .cartItems(Collections.emptyList())
                    .build();
        }
    }

    // ===================================================
    // 상품 추가
    // ===================================================

    @Transactional
    public CartDTO addItem(int tableNumber, CartItemDTO request) {
        Integer menuId = cartItemMapper.findMenuIdByNameAndPrice(request.getMenuName(), request.getMenuPrice());
        if (menuId == null) {
            return CartDTO.builder()
                    .success(false)
                    .message("메뉴를 찾을 수 없습니다: " + request.getMenuName())
                    .cartItems(Collections.emptyList())
                    .build();
        }

        int tableId = resolveTableId(tableNumber);
        Cart cart = getOrCreateCart(tableId);

        CartItem existing = cartItemMapper.findByCartIdAndMenuId(cart.getId(), menuId);
        int requestQty = Math.max(request.getQuantity(), 0);
        int targetQty = (existing != null ? existing.getQuantity() : 0) + requestQty;

        if (isGameMenu(menuId) && !hasEnoughGameStock(menuId, targetQty)) {
            int available = availableGameStock(menuId);
            return CartDTO.builder()
                    .success(false)
                    .message("게임 재고가 부족합니다. 요청 수량: " + targetQty + ", 현재 가능 수량: " + available)
                    .cartItems(Collections.emptyList())
                    .build();
        }

        if (existing != null) {
            int newQty = existing.getQuantity() + request.getQuantity();
            cartItemMapper.updateQuantity(cart.getId(), menuId, newQty);
            log.debug("수량 누적 - 메뉴: {}, {}→{}", request.getMenuName(), existing.getQuantity(), newQty);
        } else {
            cartItemMapper.insert(CartItem.builder()
                    .cartId(cart.getId())
                    .menuId(menuId)
                    .quantity(request.getQuantity())
                    .build());
            log.debug("신규 추가 - 메뉴: {}, 수량: {}", request.getMenuName(), request.getQuantity());
        }

        cartMapper.updateTimestamp(cart.getId());
        int cartCount = cartItemMapper.findByCartId(cart.getId()).size();

        return CartDTO.builder()
                .success(true)
                .message(request.getMenuName() + "이(가) 장바구니에 추가되었습니다.")
                .cartCount(cartCount)
                .build();
    }

    // ===================================================
    // 수량 변경 (quantity <= 0 이면 삭제)
    // ===================================================

    @Transactional
    public CartDTO updateItem(int tableNumber, CartItemDTO request) {
        Integer menuId = cartItemMapper.findMenuIdByNameAndPrice(request.getMenuName(), request.getMenuPrice());
        if (menuId == null) {
            return CartDTO.builder()
                    .success(false)
                    .message("메뉴를 찾을 수 없습니다.")
                    .cartItems(Collections.emptyList())
                    .build();
        }

        int tableId = resolveTableId(tableNumber);
        Cart cart = cartMapper.findByTableId(tableId);
        if (cart == null) {
            return CartDTO.builder()
                    .success(false)
                    .message("장바구니가 없습니다.")
                    .cartItems(Collections.emptyList())
                    .build();
        }

        if (request.getQuantity() > 0 && isGameMenu(menuId) && !hasEnoughGameStock(menuId, request.getQuantity())) {
            int available = availableGameStock(menuId);
            return CartDTO.builder()
                    .success(false)
                    .message("게임 재고가 부족합니다. 요청 수량: " + request.getQuantity() + ", 현재 가능 수량: " + available)
                    .cartItems(Collections.emptyList())
                    .build();
        }

        if (request.getQuantity() <= 0) {
            cartItemMapper.deleteByCartIdAndMenuId(cart.getId(), menuId);
            log.debug("항목 삭제 - 메뉴: {}", request.getMenuName());
        } else {
            cartItemMapper.updateQuantity(cart.getId(), menuId, request.getQuantity());
            log.debug("수량 변경 - 메뉴: {}, 수량: {}", request.getMenuName(), request.getQuantity());
        }

        cartMapper.updateTimestamp(cart.getId());

        List<CartItem> items = cartItemMapper.findByCartId(cart.getId());
        int total = items.stream().mapToInt(i -> i.getMenuPrice() * i.getQuantity()).sum();

        return CartDTO.builder()
                .success(true)
                .cartCount(items.size())
                .totalPrice(total)
                .build();
    }

    // ===================================================
    // 장바구니 비우기
    // ===================================================

    @Transactional
    public CartDTO clearCart(int tableNumber) {
        try {
            int tableId = resolveTableId(tableNumber);
            Cart cart = cartMapper.findByTableId(tableId);
            if (cart != null) {
                cartItemMapper.deleteAllByCartId(cart.getId());
                log.debug("장바구니 비우기 - tableNumber: {}", tableNumber);
            }
            return CartDTO.builder()
                    .success(true)
                    .cartCount(0)
                    .totalPrice(0)
                    .cartItems(Collections.emptyList())
                    .build();
        } catch (Exception e) {
            log.warn("장바구니 비우기 실패: {}", e.getMessage());
            return CartDTO.builder()
                    .success(false)
                    .message("장바구니 비우기에 실패했습니다.")
                    .cartItems(Collections.emptyList())
                    .build();
        }
    }

    // ===================================================
    // 헬퍼
    // ===================================================

    private int resolveTableId(int tableNumber) {
        Integer tableId = cartMapper.findCafeTableIdByTableNumber(tableNumber);
        if (tableId == null) {
            throw new IllegalArgumentException("존재하지 않는 테이블 번호입니다: " + tableNumber);
        }
        return tableId;
    }

    private Cart getOrCreateCart(int tableId) {
        Cart cart = cartMapper.findByTableId(tableId);
        if (cart == null) {
            Cart newCart = Cart.builder().tableId(tableId).build();
            cartMapper.insert(newCart);
            cart = cartMapper.findByTableId(tableId);
            log.debug("장바구니 신규 생성 - tableId: {}", tableId);
        }
        return cart;
    }

    private boolean isGameMenu(int menuId) {
        return cartItemMapper.countGameMenuByMenuId(menuId) > 0;
    }

    private int availableGameStock(int menuId) {
        return cartItemMapper.countAvailableGameStockByMenuId(menuId);
    }

    private boolean hasEnoughGameStock(int menuId, int requiredQty) {
        return availableGameStock(menuId) >= requiredQty;
    }
}
