package org.example.board_cafe_kiosk_2603.service.kiosk.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.kiosk.cart.Cart;
import org.example.board_cafe_kiosk_2603.domain.kiosk.cart.CartItem;
import org.example.board_cafe_kiosk_2603.domain.kiosk.order.OrderItem;
import org.example.board_cafe_kiosk_2603.domain.kiosk.order.OrderStatus;
import org.example.board_cafe_kiosk_2603.domain.kiosk.order.Orders;
import org.example.board_cafe_kiosk_2603.dto.kiosk.order.OrderItemDTO;
import org.example.board_cafe_kiosk_2603.dto.kiosk.order.OrdersDTO;
import org.example.board_cafe_kiosk_2603.mapper.common.cafeTableSession.CafeTableSessionMapper;
import org.example.board_cafe_kiosk_2603.mapper.admin.product.GameItemMapper;
import org.example.board_cafe_kiosk_2603.mapper.kiosk.cart.CartItemMapper;
import org.example.board_cafe_kiosk_2603.mapper.kiosk.cart.CartMapper;
import org.example.board_cafe_kiosk_2603.mapper.kiosk.order.OrdersMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 주문 관리 서비스
 * 웹소켓 통합 - 상태 변경 시 /topic/orders/{tableId}로 실시간 전송
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrdersMapper ordersMapper;
    private final CartMapper cartMapper;
    private final CartItemMapper cartItemMapper;
    private final CafeTableSessionMapper tableSessionMapper;
    private final GameItemMapper gameItemMapper;
    private final SimpMessagingTemplate messagingTemplate;

    // ===================================================
    // 주문 생성
    // ===================================================

    @Transactional
    public OrdersDTO createOrderFromCart(int tableNumber, String customerPhone, Integer requestedTotalAmount) {
        // tableNumber → tableId 변환
        Integer tableId = cartMapper.findCafeTableIdByTableNumber(tableNumber);
        if (tableId == null) {
            return OrdersDTO.builder()
                    .success(false)
                    .message("테이블을 찾을 수 없습니다.")
                    .build();
        }

        // 활성 세션 조회
        var session = tableSessionMapper.findActiveByTableId(tableId);
        if (session == null) {
            return OrdersDTO.builder()
                    .success(false)
                    .message("활성 세션이 없습니다. 패키지를 먼저 선택해주세요.")
                    .build();
        }

        // 카트 조회
        Cart cart = cartMapper.findByTableId(tableId);
        if (cart == null) {
            return OrdersDTO.builder().success(false).message("장바구니가 없습니다.").build();
        }

        List<CartItem> cartItems = cartItemMapper.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            return OrdersDTO.builder().success(false).message("장바구니가 비어있습니다.").build();
        }

        // 게임 메뉴는 주문 생성 직전에도 재고(NORMAL) 확인이 필요하다.
        // (장바구니 담은 이후 다른 테이블에서 대여되어 재고가 0이 될 수 있음)
        for (CartItem cartItem : cartItems) {
            int menuId = cartItem.getMenuId();
            if (cartItemMapper.countGameMenuByMenuId(menuId) > 0
                    && cartItemMapper.countAvailableGameStockByMenuId(menuId) <= 0) {
                return OrdersDTO.builder()
                        .success(false)
                        .message("보유중인 게임이 없습니다")
                        .build();
            }
        }

        int serverTotalAmount = cartItems.stream()
                .mapToInt(ci -> ci.getMenuPrice() * ci.getQuantity())
                .sum();
        if (requestedTotalAmount != null && requestedTotalAmount != serverTotalAmount) {
            log.warn("주문 금액 불일치 감지 - tableNumber: {}, 요청금액: {}, 서버계산금액: {} (서버금액으로 처리)",
                    tableNumber, requestedTotalAmount, serverTotalAmount);
        }

        // 주문 유형 분리: 게임(무료) / 일반(유료)
        List<CartItem> gameItems = cartItems.stream()
                .filter(ci -> ci.getMenuPrice() == 0)
                .collect(Collectors.toList());
        List<CartItem> normalItems = cartItems.stream()
                .filter(ci -> ci.getMenuPrice() != 0)
                .collect(Collectors.toList());

        List<OrdersDTO> createdOrders = new ArrayList<>();

        if (!normalItems.isEmpty()) {
            OrdersDTO normalOrder = createSeparatedOrder(
                    session.getId(),
                    tableId,
                    customerPhone,
                    normalItems
            );
            createdOrders.add(normalOrder);
        }

        if (!gameItems.isEmpty()) {
            OrdersDTO gameOrder = createSeparatedOrder(
                    session.getId(),
                    tableId,
                    customerPhone,
                    gameItems
            );
            createdOrders.add(gameOrder);
        }

        // 장바구니 비우기
        cartItemMapper.deleteAllByCartId(cart.getId());

        // 웹소켓: 신규 주문 알림 (분리 생성된 주문 각각 전송)
        for (OrdersDTO created : createdOrders) {
            broadcastNewOrder(created, tableId);
        }

        if (createdOrders.isEmpty()) {
            return OrdersDTO.builder()
                    .success(false)
                    .message("주문 생성 대상이 없습니다.")
                    .build();
        }

        // 응답 기본값: 일반 주문 우선, 없으면 게임 주문 반환
        OrdersDTO response = createdOrders.stream()
                .filter(o -> o.getItems() != null && o.getItems().stream()
                        .anyMatch(i -> i != null && i.getPrice() > 0))
                .findFirst()
                .orElse(createdOrders.get(0));

        if (createdOrders.size() > 1) {
            response.setMessage("일반 주문과 게임 요청이 분리 생성되었습니다.");
        }
        return response;
    }

    public boolean isOrderOwnedByTableNumber(int orderId, int tableNumber) {
        Orders order = ordersMapper.findByOrderId(orderId);
        if (order == null) {
            return false;
        }
        Integer tableId = cartMapper.findCafeTableIdByTableNumber(tableNumber);
        return tableId != null && Objects.equals(order.getTableId(), tableId);
    }

    public boolean isSessionOwnedByTableNumber(long sessionId, int tableNumber) {
        var session = tableSessionMapper.findById(sessionId);
        if (session == null) {
            return false;
        }
        Integer tableId = cartMapper.findCafeTableIdByTableNumber(tableNumber);
        return tableId != null && Objects.equals(session.getTableId(), tableId);
    }

    // ===================================================
    // 주문 조회
    // ===================================================

    public OrdersDTO getOrder(int orderId) {
        Orders order = ordersMapper.findByOrderId(orderId);
        if (order == null) {
            return OrdersDTO.builder().success(false).message("주문을 찾을 수 없습니다.").build();
        }
        return toDTO(order, fetchItemDTOs(orderId));
    }

    public List<OrdersDTO> getNewOrders() {
        return ordersMapper.findByStatus(OrderStatus.ORDERED.name()).stream()
                .map(order -> toDTO(order, fetchItemDTOs(order.getId())))
                .collect(Collectors.toList());
    }

    public List<OrdersDTO> getOrdersByTableId(int tableId) {
        var session = tableSessionMapper.findActiveByTableId(tableId);
        if (session == null) return List.of();
        return ordersMapper.findBySessionId(session.getId()).stream()
                .map(order -> toDTO(order, fetchItemDTOs(order.getId())))
                .collect(Collectors.toList());
    }

    public OrdersDTO getLatestOrderByTableNumber(int tableNumber) {
        Integer tableId = cartMapper.findCafeTableIdByTableNumber(tableNumber);
        if (tableId == null) {
            return OrdersDTO.builder().success(false).message("테이블을 찾을 수 없습니다.").build();
        }
        Orders order = ordersMapper.findLatestByTableId(tableId);
        if (order == null) {
            return OrdersDTO.builder().success(false).message("주문이 없습니다.").build();
        }
        return toDTO(order, fetchItemDTOs(order.getId()));
    }

    public List<OrdersDTO> getOrdersBySessionId(long sessionId) {
        return ordersMapper.findBySessionId(sessionId).stream()
                .map(order -> toDTO(order, fetchItemDTOs(order.getId())))
                .collect(Collectors.toList());
    }

    public List<OrdersDTO> getActiveSessionOrders(int tableNumber) {
        Integer tableId = cartMapper.findCafeTableIdByTableNumber(tableNumber);
        if (tableId == null) return List.of();
        return getOrdersByTableId(tableId);
    }

    // ===================================================
    // 주문 상태 변경
    // ===================================================

    @Transactional
    public OrdersDTO updateStatus(int orderId, String newStatus) {
        Orders order = ordersMapper.findByOrderId(orderId);
        if (order == null) {
            return OrdersDTO.builder().success(false).message("주문을 찾을 수 없습니다.").build();
        }

        List<OrderItemDTO> items = fetchItemDTOs(orderId);
        boolean gameOnlyOrder = isGameOnlyOrderItems(items);

        OrderStatus current;
        OrderStatus next;
        try {
            current = order.getStatusEnum();
            next = OrderStatus.valueOf(newStatus);
        } catch (IllegalArgumentException e) {
            return OrdersDTO.builder().success(false).message("알 수 없는 상태입니다.").build();
        }

        // 상태 전이 검증
        try {
            if (gameOnlyOrder) {
                if (next == OrderStatus.CONFIRMED) {
                    validateGameSerialMatched(order, items);
                }
                validateGameOrderTransition(current, next);
            } else {
                current.validateTransitionTo(next);
            }
        } catch (IllegalStateException e) {
            return OrdersDTO.builder().success(false).message(e.getMessage()).build();
        }

        // DB 업데이트
        ordersMapper.updateOrderStatus(Orders.builder().id(orderId).status(next.name()).build());
        log.debug(" 주문 상태 변경 - orderId: {}, gameOnly: {}, {} → {}",
                orderId, gameOnlyOrder, current.name(), next.name());

        Orders updated = ordersMapper.findByOrderId(orderId);
        OrdersDTO result = toDTO(updated, items);

        // 웹소켓: 상태 변경 실시간 전송
        broadcastOrderUpdate(result, order.getTableId());

        return result;
    }

    @Transactional
    public OrdersDTO cancelOrder(int orderId) {
        return updateStatus(orderId, OrderStatus.CANCELLED.name());
    }

    // ===================================================
    // 웹소켓 브로드캐스팅
    // ===================================================

    /**
     * 신규 주문 알림
     */
    private void broadcastNewOrder(OrdersDTO order, int tableId) {
        try {
            if (isGameOnlyOrder(order)) {
                messagingTemplate.convertAndSend("/topic/new-game-orders", order);
                log.debug("📡 신규 게임요청 브로드캐스트 - orderId: {}, tableId: {}", order.getId(), tableId);
            } else {
                messagingTemplate.convertAndSend("/topic/new-orders", order);
                log.debug("📡 신규 일반주문 브로드캐스트 - orderId: {}, tableId: {}", order.getId(), tableId);
            }
        } catch (Exception e) {
            log.warn("웹소켓 전송 실패: {}", e.getMessage());
        }
    }

    /**
     * 주문 상태 변경 알림
     */
    private void broadcastOrderUpdate(OrdersDTO order, int tableId) {
        try {
            List<OrdersDTO> orders = getOrdersByTableId(tableId);
            messagingTemplate.convertAndSend("/topic/orders/" + tableId, orders);
            log.debug("📡 주문 상태 업데이트 - tableId: {}, 주문 수: {}", tableId, orders.size());
        } catch (Exception e) {
            log.warn("웹소켓 전송 실패: {}", e.getMessage());
        }
    }

    // ===================================================
    // 헬퍼 메서드
    // ===================================================

    private List<OrderItemDTO> fetchItemDTOs(int orderId) {
        return ordersMapper.findItemsByOrderId(orderId).stream()
                .map(item -> OrderItemDTO.builder()
                        .id(item.getId())
                        .orderId(item.getOrderId())
                        .menuId(item.getMenuId())
                        .menuName(item.getMenuName())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());
    }

    private OrdersDTO toDTO(Orders order, List<OrderItemDTO> items) {
        return OrdersDTO.builder()
                .success(true)
                .id(order.getId())
                .tableId(order.getTableId())
                .customerPhone(order.getCustomerPhone())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .orderedAt(order.getOrderedAt())
                .items(items)
                .build();
    }

    private boolean isGameOnlyOrder(OrdersDTO order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            return false;
        }
        return order.getItems().stream()
                .allMatch(item -> item != null && item.getPrice() == 0);
    }

    private boolean isGameOnlyOrderItems(List<OrderItemDTO> items) {
        return items != null
                && !items.isEmpty()
                && items.stream().allMatch(item -> item != null && item.getPrice() == 0);
    }

    private void validateGameOrderTransition(OrderStatus current, OrderStatus next) {
        boolean allowed = switch (current) {
            case ORDERED -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.CANCELLED;
            case CANCELLED -> false;
            default -> false;
        };

        if (!allowed) {
            throw new IllegalStateException(
                    String.format("게임 주문은 허용되지 않는 상태 전이입니다: %s → %s", current.name(), next.name()));
        }
    }

    private void validateGameSerialMatched(Orders order, List<OrderItemDTO> items) {
        int requiredQty = items.stream()
                .filter(i -> i != null && i.getPrice() == 0)
                .mapToInt(OrderItemDTO::getQuantity)
                .sum();

        if (requiredQty <= 0) {
            throw new IllegalStateException("게임 주문 수량이 올바르지 않습니다.");
        }

        int assignedQty = gameItemMapper.countMatchedRentedGameHistoriesForOrder(
                order.getId(),
                order.getSessionId()
        );

        if (assignedQty < requiredQty) {
            throw new IllegalStateException("일련번호 매칭이 완료되어야 게임 주문을 확인할 수 있습니다.");
        }
    }

    private OrdersDTO createSeparatedOrder(long sessionId,
                                           int tableId,
                                           String customerPhone,
                                           List<CartItem> items) {
        int totalAmount = items.stream()
                .mapToInt(ci -> ci.getMenuPrice() * ci.getQuantity())
                .sum();

        Orders order = Orders.builder()
                .sessionId(sessionId)
                .tableId(tableId)
                .customerPhone((customerPhone != null && !customerPhone.isBlank()) ? customerPhone : null)
                .status(OrderStatus.ORDERED.name())
                .totalAmount(totalAmount)
                .build();
        ordersMapper.insertOrder(order);

        for (CartItem ci : items) {
            ordersMapper.insertOrderItem(OrderItem.builder()
                    .orderId(order.getId())
                    .menuId(ci.getMenuId())
                    .menuName(ci.getMenuName())
                    .price(ci.getMenuPrice())
                    .quantity(ci.getQuantity())
                    .build());
        }

        log.debug(" 주문 생성(분리) - orderId: {}, tableId: {}, amount: {}, itemCount: {}",
                order.getId(), tableId, totalAmount, items.size());
        return toDTO(order, fetchItemDTOs(order.getId()));
    }
}
