package org.example.board_cafe_kiosk_2603.service.kiosk.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.common.cafeTableSession.CafeTableSession;
import org.example.board_cafe_kiosk_2603.domain.kiosk.cafePackage.CafePackage;
import org.example.board_cafe_kiosk_2603.domain.kiosk.order.OrderItem;
import org.example.board_cafe_kiosk_2603.domain.kiosk.order.OrderStatus;
import org.example.board_cafe_kiosk_2603.domain.kiosk.order.Orders;
import org.example.board_cafe_kiosk_2603.domain.kiosk.payment.Payment;
import org.example.board_cafe_kiosk_2603.dto.kiosk.cafePackage.CafePackageDTO;
import org.example.board_cafe_kiosk_2603.dto.kiosk.payment.PaymentDTO;
import org.example.board_cafe_kiosk_2603.mapper.admin.table.CafeTableMapper;
import org.example.board_cafe_kiosk_2603.mapper.common.cafeTableSession.CafeTableSessionMapper;
import org.example.board_cafe_kiosk_2603.mapper.kiosk.cafePackage.CafePackageMapper;
import org.example.board_cafe_kiosk_2603.mapper.kiosk.cart.CartMapper;
import org.example.board_cafe_kiosk_2603.mapper.kiosk.order.OrdersMapper;
import org.example.board_cafe_kiosk_2603.mapper.kiosk.payment.PaymentMapper;
import org.example.board_cafe_kiosk_2603.mapper.admin.product.GameItemMapper;
import org.example.board_cafe_kiosk_2603.service.admin.point.PointService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final CafePackageMapper cafePackageMapper;
    private final OrdersMapper ordersMapper;
    private final PaymentMapper paymentMapper;
    private final GameItemMapper gameItemMapper;
    private final CartMapper cartMapper;
    private final PointService pointService;
    private final CafeTableSessionMapper tableSessionMapper;
    private final CafeTableMapper cafeTableMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${toss.payments.secret-key}")
    private String secretKey;

    @Value("${toss.payments.client-key}")
    private String clientKey;

    @Value("${toss.payments.confirm-url}")
    private String confirmUrl;

    private static final double EARN_RATE = 0.05;

    // ===================================================
    // 1단계: 결제 준비
    // ===================================================

    /**
     * 결제 준비 - 토스 결제창 호출 전 준비 단계
     * 세션 내 여러 주문을 한 번의 결제로 처리
     * 패키지 요금 + 주문 금액 합산, 포인트 사용 가능
     */
    public PaymentDTO preparePayment(int tableNumber, PaymentDTO request) {
        try {
            log.debug("=== 결제 준비 시작 - tableNumber: {}", tableNumber);

            // 테이블 검증
            Integer tableId = cartMapper.findCafeTableIdByTableNumber(tableNumber);
            if (tableId == null) {
                return errorResponse("존재하지 않는 테이블입니다: " + tableNumber);
            }

            // 세션 검증
            CafeTableSession session = tableSessionMapper.findActiveByTableId(tableId);
            if (session == null) {
                return errorResponse("진행 중인 세션이 없습니다. 패키지를 먼저 선택해 주세요.");
            }

            // 세션 주문 목록 조회 (패키지 단독 이용일 수 있으므로 빈 목록 허용)
            List<Orders> activeOrders = getActiveOrders(session.getId());

            // 패키지 정보 조회
            CafePackageDTO cafePackage = null;
            int pkgPrice = 0;
            if (session.getPackageId() != null) {
                CafePackage pkg = cafePackageMapper.findById(session.getPackageId());
                if (pkg != null) {
                    cafePackage = buildCafePackageDTO(pkg);
                    int guestCount = session.getInitialGuestCnt() != null ? session.getInitialGuestCnt() : 1;
                    pkgPrice = pkg.getBasePrice() * guestCount;
                    log.debug("Package - id: {}, name: {}, price: {}", pkg.getId(), pkg.getName(), pkgPrice);
                } else {
                    log.warn("Package not found - packageId: {}", session.getPackageId());
                }
            }

            // 금액 계산
            int menuTotal   = activeOrders.stream().mapToInt(Orders::getTotalAmount).sum();
            int overCharge  = calculateOverCharge(session);
            int totalAmount = pkgPrice + menuTotal + overCharge;
            int pointUsed   = request.getPointUsed() != null ? request.getPointUsed() : 0;
            int finalAmount = Math.max(totalAmount - pointUsed, 0);

            // 토스 결제 식별자 생성
            String orderIdToss  = generateOrderId(tableNumber);
            String orderName    = buildOrderNameFromOrders(activeOrders);
            String customerKey  = generateCustomerKey(tableNumber, session.getId());

            PaymentDTO response = PaymentDTO.builder()
                    .success(true)
                    .orderIdToss(orderIdToss)
                    .amount(finalAmount)
                    .orderName(orderName)
                    .totalAmount(totalAmount)
                    .pointUsed(pointUsed)
                    .clientKey(clientKey)
                    .customerKey(customerKey)
                    .cafePackage(cafePackage)
                    .build();

            log.debug("=== 결제 준비 완료 - orderId: {}, amount: {}, packageId: {}, overCharge: {}",
                    orderIdToss, finalAmount, cafePackage != null ? cafePackage.getId() : "없음", overCharge);
            return response;

        } catch (Exception e) {
            log.error("결제 준비 중 오류 - tableNumber: {}", tableNumber, e);
            return errorResponse("결제 준비 중 오류가 발생했습니다.");
        }
    }

    // ===================================================
    // 2단계: 결제 승인
    // ===================================================

    /**
     * 토스 결제 승인 및 DB 저장
     * 세션 내 여러 주문(Order)을 한 번의 결제(Payment)로 처리
     * Payment.session_id UNIQUE → 세션당 최종 1회만 결제
     */
    @Transactional
    public PaymentDTO confirmPayment(String paymentKey, String orderIdToss,
                                     int amount, int tableNumber,
                                     int pointUsed, String customerPhone) {
        try {
            log.debug("=== 결제 승인 시작 - orderIdToss: {}, amount: {}, pointUsed: {}",
                    orderIdToss, amount, pointUsed);

            // 중복 결제 방지
            if (paymentMapper.findByPaymentKey(paymentKey) != null) {
                return errorResponse("이미 처리된 결제입니다.");
            }

            // 토스 API 승인 호출
            TossConfirmResponse tossResponse = callTossConfirmApi(paymentKey, orderIdToss, amount);
            if (tossResponse == null) {
                return errorResponse("토스 API 응답 파싱 실패");
            }

            // DB 데이터 조회
            Integer tableId = cartMapper.findCafeTableIdByTableNumber(tableNumber);
            if (tableId == null) {
                return errorResponse("존재하지 않는 테이블입니다.");
            }

            CafeTableSession session = tableSessionMapper.findActiveByTableId(tableId);
            if (session == null) {
                return errorResponse("진행 중인 세션이 없습니다.");
            }

            // 금액 재계산 (서버 사이드 검증)
            List<Orders> activeOrders = getActiveOrders(session.getId());

            int pkgPrice = calculatePackagePrice(session);
            int menuTotal   = activeOrders.stream().mapToInt(Orders::getTotalAmount).sum();
            int overCharge  = calculateOverCharge(session);
            int totalAmount = pkgPrice + menuTotal + overCharge;
            int finalAmount = Math.max(totalAmount - pointUsed, 0);

            // 토스에서 받은 금액과 서버 계산 금액 검증
            if (finalAmount != amount) {
                log.error("금액 불일치 - 서버 계산: {}, 토스 요청: {}", finalAmount, amount);
                return errorResponse("결제 금액이 일치하지 않습니다.");
            }

            // 최신 주문 ID (포인트 참조용)
            Long latestOrderId = activeOrders.stream()
                    .map(Orders::getId)
                    .max(Integer::compareTo)
                    .map(Integer::longValue)
                    .orElse(null);

            // 결제 정보 DB 저장
            createPayment(session, finalAmount, paymentKey, orderIdToss, tossResponse, tableNumber);

            // 포인트 사용/적립 처리
            int earnedPoints = processPoints(customerPhone, pointUsed, finalAmount, latestOrderId);

            // 결제 완료 시점에 남아있는 게임 대여 이력을 자동 반납 처리한다.
            settleRemainingGameRentalsOnCheckout(session.getId());

            // 결제 완료 후 테이블 세션 종료 + 청소중 상태 전환
            closeTableSessionAndSetCleaning(tableId, session.getId());

            log.debug("=== 결제 완료 - latestOrderId: {}, finalAmount: {}, earnedPoints: {}",
                    latestOrderId, finalAmount, earnedPoints);

            return PaymentDTO.builder()
                    .success(true)
                    .orderId(latestOrderId)
                    .totalAmount(totalAmount)
                    .pointUsed(pointUsed)
                    .finalAmount(finalAmount)
                    .earnedPoints(earnedPoints)
                    .paymentKey(paymentKey)
                    .method(tossResponse.method)
                    .build();

        } catch (HttpClientErrorException e) {
            String errorBody = new String(e.getResponseBodyAsByteArray(), StandardCharsets.UTF_8);
            log.error("토스 API 호출 실패 - {}", errorBody);
            return errorResponse(parseTossError(errorBody));
        } catch (Exception e) {
            log.error("결제 승인 중 오류", e);
            return errorResponse("결제 승인 중 오류가 발생했습니다.");
        }
    }

    // ===================================================
    // 토스 API 호출
    // ===================================================

    private TossConfirmResponse callTossConfirmApi(String paymentKey, String orderId, int amount) {
        try {
            String encoded = Base64.getEncoder().encodeToString(
                    (secretKey + ":").getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("Authorization", "Basic " + encoded);

            Map<String, Object> body = new HashMap<>();
            body.put("paymentKey", paymentKey);
            body.put("orderId", orderId);
            body.put("amount", amount);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    confirmUrl, HttpMethod.POST, new HttpEntity<>(body, headers), byte[].class);

            byte[] responseBytes = response.getBody();
            if (responseBytes == null || responseBytes.length == 0) {
                return null;
            }

            String responseBody = new String(responseBytes, StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(responseBody);
            return TossConfirmResponse.builder()
                    .method(root.path("method").asText())
                    .approvedAt(root.path("approvedAt").asText())
                    .rawResponse(responseBody)
                    .build();

        } catch (Exception e) {
            log.error("토스 API 파싱 오류", e);
            return null;
        }
    }

    private String parseTossError(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String message = root.path("message").asText();
            return !message.isBlank() ? message : "결제 승인에 실패했습니다.";
        } catch (Exception e) {
            return "결제 승인에 실패했습니다.";
        }
    }

    // ===================================================
    // 비즈니스 로직 헬퍼
    // ===================================================

    /**
     * 세션의 활성 주문 목록 조회 (CANCELLED 제외)
     */
    private List<Orders> getActiveOrders(long sessionId) {
        List<Orders> sessionOrders = ordersMapper.findBySessionId(sessionId);
        return sessionOrders.stream()
                .filter(o -> !OrderStatus.CANCELLED.name().equals(o.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * 패키지 금액 계산 (인원수 곱하기)
     */
    private int calculatePackagePrice(CafeTableSession session) {
        if (session.getPackageId() == null) {
            return 0;
        }
        CafePackage pkg = cafePackageMapper.findById(session.getPackageId());
        if (pkg == null) {
            return 0;
        }
        int guestCount = session.getInitialGuestCnt() != null ? session.getInitialGuestCnt() : 1;
        return pkg.getBasePrice() * guestCount;
    }

    private int calculateOverCharge(CafeTableSession session) {
        if (session == null || session.getPackageId() == null || session.getCheckInTime() == null) {
            return 0;
        }

        CafePackage pkg = cafePackageMapper.findById(session.getPackageId());
        if (pkg == null || pkg.getDurationMinutes() == null || pkg.getExtraPricePerMin() == null) {
            return 0;
        }

        int durationMinutes = pkg.getDurationMinutes();
        double extraPricePerMin = pkg.getExtraPricePerMin();
        if (durationMinutes <= 0 || extraPricePerMin <= 0) {
            return 0;
        }

        long elapsedMinutes = Math.max(0, Duration.between(session.getCheckInTime(), LocalDateTime.now()).toMinutes());
        long overMinutes = elapsedMinutes - durationMinutes;
        if (overMinutes <= 0) {
            return 0;
        }

        long overUnits = (long) Math.ceil(overMinutes / 10.0);
        int guestCount = session.getInitialGuestCnt() != null ? session.getInitialGuestCnt() : 1;
        return (int) Math.ceil(overUnits * extraPricePerMin * guestCount);
    }

    /**
     * CafePackage → CafePackageDTO 변환
     */
    private CafePackageDTO buildCafePackageDTO(CafePackage pkg) {
        return CafePackageDTO.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .type(pkg.getType())
                .durationMinutes(pkg.getDurationMinutes())
                .basePrice(pkg.getBasePrice())
                .extraPricePerMin(pkg.getExtraPricePerMin())
                .active(pkg.isActive())
                .updatedAt(pkg.getUpdatedAt())
                .build();
    }

    private String generateOrderId(int tableNumber) {
        return "KIOSK-T" + tableNumber + "-" + System.currentTimeMillis();
    }

    private String generateCustomerKey(int tableNumber, long sessionId) {
        return "GUEST-T" + tableNumber + "-S" + sessionId;
    }

    private String buildOrderNameFromOrders(List<Orders> orders) {
        if (orders.isEmpty()) return "이용요금";
        List<OrderItem> items = ordersMapper.findItemsByOrderId(orders.get(0).getId());
        if (items.isEmpty()) return "이용요금";
        int extra = orders.size() - 1;
        return items.get(0).getMenuName() + (extra > 0 ? " 외 " + extra + "건" : "");
    }

    /**
     * 결제 정보 DB 저장
     */
    private void createPayment(CafeTableSession session, int finalAmount,
                               String paymentKey, String orderIdToss,
                               TossConfirmResponse tossResponse, int tableNumber) {
        LocalDateTime approvedAt = parseApprovedAt(tossResponse.approvedAt);

        Payment payment = Payment.builder()
                .sessionId(session.getId())
                .tableNumber(tableNumber)
                .status("DONE")
                .finalAmount(finalAmount)
                .paymentKey(paymentKey)
                .orderIdToss(orderIdToss)
                .method(tossResponse.method)
                .rawResponse(tossResponse.rawResponse)
                .approvedAt(approvedAt)
                .paidAt(LocalDateTime.now())
                .build();

        paymentMapper.insert(payment);
    }

    private LocalDateTime parseApprovedAt(String approvedAt) {
        if (approvedAt == null || approvedAt.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return OffsetDateTime.parse(approvedAt).toLocalDateTime();
        } catch (DateTimeParseException e) {
            log.warn("approvedAt 파싱 실패 - raw: {}, 현재 시각으로 대체", approvedAt);
            return LocalDateTime.now();
        }
    }

    /**
     * 포인트 사용 + 적립 처리
     */
    private int processPoints(String customerPhone, int pointUsed, int finalAmount, Long orderId) {
        int earnedPoints = 0;

        if (pointUsed > 0 && isValidPhone(customerPhone)) {
            pointService.usePoint(customerPhone, pointUsed, orderId);
            log.debug("포인트 사용 - {}: -{}P", customerPhone, pointUsed);
        }

        if (pointUsed > 0) {
            log.debug("포인트 사용 주문은 적립 제외 - phone: {}, orderId: {}", customerPhone, orderId);
            return 0;
        }

        if (isValidPhone(customerPhone) && finalAmount > 0) {
            earnedPoints = (int) Math.floor(finalAmount * EARN_RATE);
            pointService.earnPoint(customerPhone, earnedPoints, orderId);
            log.debug("포인트 적립 - {}: +{}P", customerPhone, earnedPoints);
        }

        return earnedPoints;
    }

    private boolean isValidPhone(String customerPhone) {
        return customerPhone != null && !customerPhone.isBlank();
    }

    private void closeTableSessionAndSetCleaning(Integer tableId, Long sessionId) {
        int readUpdated = cafeTableMapper.updateMessagesReadStatusBySessionId(sessionId);
        int closedRows = cafeTableMapper.closeSession(sessionId);
        int statusRows = cafeTableMapper.updateTableStatusAndSession(tableId, "CLEANING", null);

        String currentStatus = cafeTableMapper.selectStatusById(tableId);
        Long currentSessionId = cafeTableMapper.selectCurrentSessionId(tableId);

        // 간헐적인 반영 누락을 방지하기 위해 1회 재보정
        if (!"CLEANING".equals(currentStatus) || currentSessionId != null) {
            log.warn("결제 후 상태 반영 불일치 감지 - 재보정 시도 | tableId: {}, status: {}, currentSessionId: {}",
                    tableId, currentStatus, currentSessionId);

            cafeTableMapper.updateTableStatusAndSession(tableId, "CLEANING", null);
            currentStatus = cafeTableMapper.selectStatusById(tableId);
            currentSessionId = cafeTableMapper.selectCurrentSessionId(tableId);
        }

        log.debug("결제 완료 후 세션 종료/상태 반영 결과 | " + "tableId: {}, sessionId: {}, " +
                        "msgReadRows: {}, closeRows: {}, statusRows: {}, finalStatus: {}, finalCurrentSessionId: {}",
                tableId, sessionId, readUpdated, closedRows, statusRows, currentStatus, currentSessionId);
    }

    private void settleRemainingGameRentalsOnCheckout(Long sessionId) {
        int historyUpdated = gameItemMapper.returnActiveRentalsBySessionId(sessionId);
        int itemUpdated = gameItemMapper.normalizeNormalItemsBySessionId(sessionId);
        log.debug("결제 완료 시 게임 대여 자동 반납 처리 | sessionId: {}, historyUpdated: {}, itemUpdated: {}",
                sessionId, historyUpdated, itemUpdated);
    }

    private PaymentDTO errorResponse(String message) {
        return PaymentDTO.builder()
                .success(false)
                .message(message)
                .build();
    }

    // ===================================================
    // 내부 응답 클래스
    // ===================================================

    @Builder
    private static class TossConfirmResponse {
        String method;
        String approvedAt;
        String rawResponse;
    }
}
