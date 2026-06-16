package org.example.board_cafe_kiosk_2603.mapper.kiosk;

import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.kiosk.cart.Cart;
import org.example.board_cafe_kiosk_2603.domain.kiosk.cart.CartItem;
import org.example.board_cafe_kiosk_2603.mapper.kiosk.cart.CartItemMapper;
import org.example.board_cafe_kiosk_2603.mapper.kiosk.cart.CartMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Log4j2
@SpringBootTest
class CartItemMapperTest {

    @Autowired private CartMapper cartMapper;
    @Autowired private CartItemMapper cartItemMapper;

    private int cartId;
    private int menuId;

    @BeforeEach
    void setUp() {
        Integer tableId = cartMapper.findCafeTableIdByTableNumber(1);
        Cart cart = Cart.builder().tableId(tableId).build();
        cartMapper.insert(cart);
        cartId = cart.getId();

        menuId = cartItemMapper.findMenuIdByNameAndPrice("아이스 아메리카노", 4000);
    }

    @Test
    void findMenuIdByNameAndPrice_success() {
        assertThat(menuId).isNotNull().isPositive();
    }

    @Test
    void findMenuIdByNameAndPrice_notFound() {
        Integer id = cartItemMapper.findMenuIdByNameAndPrice("없는메뉴", 99999);
        assertThat(id).isNull();
    }

    @Test
    void insert_and_findByCartId() {
        CartItem item = CartItem.builder()
                .cartId(cartId).menuId(menuId).quantity(2).build();
        cartItemMapper.insert(item);
        assertThat(item.getId()).isPositive();

        List<CartItem> items = cartItemMapper.findByCartId(cartId);
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getMenuId()).isEqualTo(menuId);
        assertThat(items.get(0).getQuantity()).isEqualTo(2);
        assertThat(items.get(0).getMenuName()).isEqualTo("아이스 아메리카노");
        assertThat(items.get(0).getMenuPrice()).isEqualTo(4000);
    }

    @Test
    void findByCartId_empty() {
        List<CartItem> items = cartItemMapper.findByCartId(cartId);
        assertThat(items).isEmpty();
    }

    @Test
    void findByCartIdAndMenuId_success() {
        cartItemMapper.insert(CartItem.builder().cartId(cartId).menuId(menuId).quantity(1).build());

        CartItem found = cartItemMapper.findByCartIdAndMenuId(cartId, menuId);
        assertThat(found).isNotNull();
        assertThat(found.getQuantity()).isEqualTo(1);
    }

    @Test
    void findByCartIdAndMenuId_notFound() {
        CartItem found = cartItemMapper.findByCartIdAndMenuId(cartId, 99999);
        assertThat(found).isNull();
    }

    @Test
    void updateQuantity() {
        cartItemMapper.insert(CartItem.builder().cartId(cartId).menuId(menuId).quantity(1).build());
        cartItemMapper.updateQuantity(cartId, menuId, 5);

        CartItem found = cartItemMapper.findByCartIdAndMenuId(cartId, menuId);
        assertThat(found.getQuantity()).isEqualTo(5);
    }

    @Test
    void deleteByCartIdAndMenuId() {
        cartItemMapper.insert(CartItem.builder().cartId(cartId).menuId(menuId).quantity(1).build());
        cartItemMapper.deleteByCartIdAndMenuId(cartId, menuId);

        assertThat(cartItemMapper.findByCartIdAndMenuId(cartId, menuId)).isNull();
    }

    @Test
    void deleteAllByCartId() {
        cartItemMapper.insert(CartItem.builder().cartId(cartId).menuId(menuId).quantity(1).build());
        cartItemMapper.deleteAllByCartId(cartId);

        assertThat(cartItemMapper.findByCartId(cartId)).isEmpty();
    }
}
