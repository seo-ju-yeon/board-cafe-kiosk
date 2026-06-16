package org.example.board_cafe_kiosk_2603.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.dto.kiosk.order.OrdersDTO;
import org.example.board_cafe_kiosk_2603.service.kiosk.order.OrderService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * 주문 상태 WebSocket 핸들러
 * 
 * 클라이언트의 주문 상태 구독 요청을 처리하고
 * 서버에서 변경사항을 실시간으로 브로드캐스트
 */
@Log4j2
@Controller
@RequiredArgsConstructor
public class OrderWebSocketHandler {

    private final OrderService orderService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 테이블의 주문 상태 구독
     * 클라이언트가 /app/subscribe/{tableId}로 메시지를 보내면
     * /topic/orders/{tableId}로 현재 주문 상태를 전송
     */
    @MessageMapping("/subscribe/{tableId}")
    public void subscribeTableOrders(@DestinationVariable int tableId) {
        log.debug("테이블 {} 주문 상태 구독 시작", tableId);
        
        try {
            // 현재 테이블의 주문 조회
            List<OrdersDTO> orders = orderService.getOrdersByTableId(tableId);
            
            // 구독 클라이언트에게 전송
            messagingTemplate.convertAndSend(
                    "/topic/orders/" + tableId,
                    orders
            );
            
            log.debug("테이블 {} 주문 {}건 전송", tableId, orders.size());
        } catch (Exception e) {
            log.error("주문 상태 전송 실패 - tableId: {}", tableId, e);
        }
    }

    /**
     * 주문 상태 변경 이벤트 브로드캐스트
     * (OrderService에서 상태 변경 후 호출)
     */
    public void broadcastOrderUpdate(int orderId, int tableId) {
        try {
            List<OrdersDTO> orders = orderService.getOrdersByTableId(tableId);
            
            // 해당 테이블을 구독 중인 모든 클라이언트에게 전송
            messagingTemplate.convertAndSend(
                    "/topic/orders/" + tableId,
                    orders
            );
            
            log.debug("주문 상태 변경 브로드캐스트 - orderId: {}, tableId: {}", orderId, tableId);
        } catch (Exception e) {
            log.error("브로드캐스트 실패", e);
        }
    }

    /**
     * 신규 주문 알림 (새로 추가)
     * OrderService.createOrderFromCart()에서 호출할 것
     */
    public void broadcastNewOrder(OrdersDTO newOrder) {
        try {
            // 관리자 대시보드에 신규 주문 알림
            messagingTemplate.convertAndSend("/topic/new-orders", newOrder);
            log.debug("신규 주문 브로드캐스트 - orderId: {}, tableId: {}",
                    newOrder.getId(), newOrder.getTableId());
        } catch (Exception e) {
            log.error("신규 주문 브로드캐스트 실패", e);
        }
    }
}
