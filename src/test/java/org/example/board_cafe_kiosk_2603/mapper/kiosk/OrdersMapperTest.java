package org.example.board_cafe_kiosk_2603.mapper.kiosk;

import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.common.cafeTableSession.CafeTableSession;
import org.example.board_cafe_kiosk_2603.domain.kiosk.order.OrderItem;
import org.example.board_cafe_kiosk_2603.domain.kiosk.order.Orders;
import org.example.board_cafe_kiosk_2603.mapper.common.cafeTableSession.CafeTableSessionMapper;
import org.example.board_cafe_kiosk_2603.mapper.kiosk.cart.CartMapper;
import org.example.board_cafe_kiosk_2603.mapper.kiosk.order.OrdersMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Log4j2
@SpringBootTest
class OrdersMapperTest {

    @Autowired private OrdersMapper ordersMapper;
    @Autowired private CartMapper cartMapper;
    @Autowired private CafeTableSessionMapper tableSessionMapper;

    private int  tableId;
    private long sessionId;

    @BeforeEach
    void setUp() {
        // table_number=1 의 활성 세션을 동적으로 조회 (init.sql 더미데이터 기반)
        tableId = cartMapper.findCafeTableIdByTableNumber(1);
        CafeTableSession activeSession = tableSessionMapper.findActiveByTableId(tableId);
        assertThat(activeSession)
                .as("table_number=1 의 활성 세션이 init.sql 더미데이터에 존재해야 합니다.")
                .isNotNull();
        sessionId = activeSession.getId();
    }

    private Orders buildOrder() {
        return Orders.builder()
                .sessionId(sessionId)
                .tableId(tableId)
                .customerPhone("010-1234-5678")
                .status("PAID")
                .totalAmount(8500)
                .build();
    }

    @Test
    void insertOrder_and_findLatest() {
        Orders order = buildOrder();
        ordersMapper.insertOrder(order);
        assertThat(order.getId()).isPositive();

        Orders found = ordersMapper.findLatestByTableId(tableId);
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(order.getId());
        assertThat(found.getStatus()).isEqualTo("PAID");
        assertThat(found.getTotalAmount()).isEqualTo(8500);
        assertThat(found.getCustomerPhone()).isEqualTo("010-1234-5678");
    }

    @Test
    void insertOrder_and_findByOrderId() {
        Orders order = buildOrder();
        ordersMapper.insertOrder(order);

        Orders found = ordersMapper.findByOrderId(order.getId());
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(order.getId());
        assertThat(found.getSessionId()).isEqualTo(sessionId);
    }

    @Test
    void findByOrderId_notFound() {
        Orders found = ordersMapper.findByOrderId(99999);
        assertThat(found).isNull();
    }

    @Test
    void findLatestByTableId_notFound() {
        Orders found = ordersMapper.findLatestByTableId(99999);
        assertThat(found).isNull();
    }

    @Test
    void insertOrderItem_and_findItems() {
        Orders order = buildOrder();
        ordersMapper.insertOrder(order);

        OrderItem item = OrderItem.builder()
                .orderId(order.getId())
                .menuId(1)
                .menuName("아이스 아메리카노")
                .price(4000)
                .quantity(2)
                .build();
        ordersMapper.insertOrderItem(item);
        assertThat(item.getId()).isPositive();

        List<OrderItem> items = ordersMapper.findItemsByOrderId(order.getId());
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getMenuName()).isEqualTo("아이스 아메리카노");
        assertThat(items.get(0).getPrice()).isEqualTo(4000);
        assertThat(items.get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void findItemsByOrderId_empty() {
        Orders order = buildOrder();
        ordersMapper.insertOrder(order);

        List<OrderItem> items = ordersMapper.findItemsByOrderId(order.getId());
        assertThat(items).isEmpty();
    }

    @Test
    void findBySessionId() {
        Orders order = buildOrder();
        ordersMapper.insertOrder(order);

        List<Orders> orders = ordersMapper.findBySessionId(sessionId);
        assertThat(orders).isNotEmpty();
        assertThat(orders.stream().anyMatch(o -> o.getId() == order.getId())).isTrue();
    }

    @Test
    void updateOrderStatus() {
        Orders order = buildOrder();
        ordersMapper.insertOrder(order);

        ordersMapper.updateOrderStatus(
                Orders.builder().id(order.getId()).status("COMPLETED").build());

        Orders found = ordersMapper.findByOrderId(order.getId());
        assertThat(found.getStatus()).isEqualTo("COMPLETED");
    }
}
