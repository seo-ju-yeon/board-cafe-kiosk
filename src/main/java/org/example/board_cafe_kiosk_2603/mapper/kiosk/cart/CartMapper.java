package org.example.board_cafe_kiosk_2603.mapper.kiosk.cart;

import org.apache.ibatis.annotations.Mapper;
import org.example.board_cafe_kiosk_2603.domain.kiosk.cart.Cart;

@Mapper
public interface CartMapper {

    // table_number(표시 번호)로 cafe_table의 PK(id) 조회
    Integer findCafeTableIdByTableNumber(int tableNumber);

    // cafe_table.id(PK)로 장바구니 조회
    Cart findByTableId(int tableId);

    // 장바구니 생성
    void insert(Cart cart);

    // 장바구니 삭제
    void deleteByTableId(int tableId);

    // updated_at 갱신
    void updateTimestamp(int cartId);
}
