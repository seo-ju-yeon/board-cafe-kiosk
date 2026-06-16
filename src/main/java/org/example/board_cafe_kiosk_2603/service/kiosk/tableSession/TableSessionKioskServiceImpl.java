package org.example.board_cafe_kiosk_2603.service.kiosk.tableSession;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.common.cafeTableSession.CafeTableSession;
import org.example.board_cafe_kiosk_2603.dto.admin.point.PointAdminDTO;
import org.example.board_cafe_kiosk_2603.dto.kiosk.cafePackage.CafePackageDTO;
import org.example.board_cafe_kiosk_2603.dto.kiosk.cart.CartDTO;
import org.example.board_cafe_kiosk_2603.domain.kiosk.order.Orders;
import org.example.board_cafe_kiosk_2603.mapper.common.cafeTableSession.CafeTableSessionMapper;
import org.example.board_cafe_kiosk_2603.mapper.kiosk.order.OrdersMapper;
import org.example.board_cafe_kiosk_2603.service.admin.cafeTable.TableSessionAdminService;
import org.example.board_cafe_kiosk_2603.service.admin.point.PointService;
import org.example.board_cafe_kiosk_2603.service.kiosk.cafePackage.CafePackageService;
import org.example.board_cafe_kiosk_2603.service.kiosk.cart.CartService;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class TableSessionKioskServiceImpl implements TableSessionKioskService{

    private final CafeTableSessionMapper tableSessionKioskMapper;
    private final CartService cartService;
    private final PointService pointService;
    private final CafePackageService cafePackageService;
    private final TableSessionAdminService tableSessionAdminService;
    private final OrdersMapper ordersMapper;

    @Override
    public Long createSession(int tableId, int packageId, int initialGuestCnt) {
        CafeTableSession tableSession = CafeTableSession.builder()
                .tableId(tableId)
                .packageId(packageId)
                .initialGuestCnt(initialGuestCnt)
                .build();

        tableSessionKioskMapper.insert(tableSession);  // useGEneratedKeys로 id 채워짐
        log.info("세션 생성 완료... tableId: {}, packageId: {}, 인원: {}",
                tableId, packageId, initialGuestCnt);
        return tableSession.getId();  // Long 반환
    }

    // 인원수 체크
    private int getPartySize(HttpSession session) {
        Integer partySize = (Integer) session.getAttribute("partySize");
        return partySize != null ? partySize : 2;
    }

    // 포인트 조회
    private int resolvePointBalance(String customerPhone) {
        if (customerPhone == null || customerPhone.isBlank()) return 0;
        PointAdminDTO point = pointService.getPointByPhone(customerPhone);
        return point != null ? point.getBalance() : 0;
    }

    @Override
    public void buildCartModel(Model model, int tableNumber, HttpSession session) {
        CartDTO cartDTO = cartService.getCart(tableNumber);
        model.addAttribute("tableNumber", tableNumber);
        model.addAttribute("partySize", getPartySize(session));
        model.addAttribute("cartItems", cartDTO.getCartItems());
        model.addAttribute("totalPrice", cartDTO.getTotalPrice());
        model.addAttribute("cartCount", cartDTO.getCartCount());
    }

    @Override
    public void buildCheckoutModel(Model model, int tableNumber, HttpSession session) {
        Integer tableId = (Integer) session.getAttribute("tableId");
        boolean adminCheckoutMode = Boolean.TRUE.equals(session.getAttribute("adminCheckoutMode"));
        CartDTO cartDTO = cartService.getCart(tableNumber);
        String customerPhone = (String) session.getAttribute("customerPhone");

        // DB에서 활성 세션 먼저 조회
        Integer packageId = null;
        Long sessionStartMillis = null;
        CafeTableSession activeSession = null;
        if (tableId != null) {
            activeSession = tableSessionAdminService.getActiveSession(tableId);
            if (activeSession != null) {
                packageId = activeSession.getPackageId();
                sessionStartMillis = toEpochMillis(activeSession.getCheckInTime());
            }
        }

        String resolvedCustomerPhone = resolveCheckoutCustomerPhone(customerPhone, activeSession);
        if (resolvedCustomerPhone != null && !resolvedCustomerPhone.isBlank()) {
            session.setAttribute("customerPhone", resolvedCustomerPhone);
        }
        int pointBalance = resolvePointBalance(resolvedCustomerPhone);

        int partySize = resolveCheckoutPartySize(activeSession, session);
        session.setAttribute("partySize", partySize);

        // 활성 세션이 없으면 기존 세션값을 사용하되, 값이 없으면 현재 시각으로 초기화해 과도한 초과시간 표시를 방지한다.
        if (sessionStartMillis == null && !adminCheckoutMode) {
            sessionStartMillis = readSessionStartMillis(session.getAttribute("sessionStartTime"));
        }
        if (sessionStartMillis == null) {
            sessionStartMillis = System.currentTimeMillis();
            session.setAttribute("sessionStartTime", sessionStartMillis);
        }
        model.addAttribute("sessionStartTime", sessionStartMillis);
        int sessionDuration = Math.max(0, (int) ((System.currentTimeMillis() - sessionStartMillis) / 60000));

        // 패키지 금액 계산 (packageId가 확정된 후)
        int packageTotal   = 0;
        String packageName = "";
        model.addAttribute("durationMinutes",  null);  // ← 기본값 먼저 설정
        model.addAttribute("extraPricePerMin", 0.0);   // ← 기본값 먼저 설정

        if (packageId != null) {
            CafePackageDTO pkg = cafePackageService.getById(packageId);
            if (pkg != null) {
                packageTotal = pkg.getBasePrice() * partySize;
                packageName  = pkg.getName();
                model.addAttribute("durationMinutes",  pkg.getDurationMinutes());
                model.addAttribute("extraPricePerMin", pkg.getExtraPricePerMin() != null ? pkg.getExtraPricePerMin() : 0.0);
            }
        }

        int totalPrice = cartDTO.getTotalPrice() + packageTotal;

        model.addAttribute("tableNumber",   tableNumber);
        model.addAttribute("partySize",     partySize);
        model.addAttribute("cartItems",     cartDTO.getCartItems());
        model.addAttribute("menuTotal",     cartDTO.getTotalPrice());
        model.addAttribute("packageName",   packageName);
        model.addAttribute("packageTotal",  packageTotal);
        model.addAttribute("totalPrice",    totalPrice);
        model.addAttribute("cartCount",     cartDTO.getCartCount());
        model.addAttribute("sessionHours",  sessionDuration / 60);
        model.addAttribute("sessionMinutes",sessionDuration % 60);
        model.addAttribute("pointBalance",  pointBalance);
        model.addAttribute("customerPhone", resolvedCustomerPhone != null ? resolvedCustomerPhone : "");

        log.info("정산 화면 - 테이블: {}, 메뉴: ₩{}, 패키지: {} ₩{}, 합계: ₩{}, 포인트: {}P",
                tableNumber, cartDTO.getTotalPrice(), packageName, packageTotal, totalPrice, pointBalance);
    }

    @Override
    public Map<String, Object> buildCheckoutMeta(Integer tableId) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("packageTotal", 0);
        meta.put("sessionStartTime", 0);
        meta.put("durationMinutes", 0);
        meta.put("extraPricePerMin", 0);
        meta.put("partySize", 1);

        if (tableId == null) {
            return meta;
        }

        CafeTableSession activeSession = tableSessionAdminService.getActiveSession(tableId);
        if (activeSession == null) {
            return meta;
        }

        int partySize = activeSession.getInitialGuestCnt() != null ? activeSession.getInitialGuestCnt() : 1;
        meta.put("partySize", partySize);
        meta.put("sessionStartTime", toEpochMillis(activeSession.getCheckInTime()));

        if (activeSession.getPackageId() != null) {
            CafePackageDTO pkg = cafePackageService.getById(activeSession.getPackageId());
            if (pkg != null) {
                meta.put("packageTotal", pkg.getBasePrice() * partySize);
                meta.put("durationMinutes", pkg.getDurationMinutes() != null ? pkg.getDurationMinutes() : 0);
                meta.put("extraPricePerMin", pkg.getExtraPricePerMin() != null ? pkg.getExtraPricePerMin() : 0);
            }
        }

        return meta;
    }

    // 세션 활성화시 인원수 체크
    private int resolveCheckoutPartySize(CafeTableSession activeSession, HttpSession session) {
        if (activeSession != null && activeSession.getInitialGuestCnt() != null) {
            return activeSession.getInitialGuestCnt();
        }
        return getPartySize(session);
    }

    private String resolveCheckoutCustomerPhone(String sessionCustomerPhone, CafeTableSession activeSession) {
        if (sessionCustomerPhone != null && !sessionCustomerPhone.isBlank()) {
            return sessionCustomerPhone;
        }
        if (activeSession == null) {
            return "";
        }

        return ordersMapper.findBySessionId(activeSession.getId()).stream()
                .map(Orders::getCustomerPhone)
                .filter(phone -> phone != null && !phone.isBlank())
                .findFirst()
                .orElse("");
    }

    // 테이블 시작 시간 계산
    private Long readSessionStartMillis(Object rawStartTime) {
        if (rawStartTime instanceof Long) {
            return (Long) rawStartTime;
        }
        if (rawStartTime instanceof Integer) {
            return ((Integer) rawStartTime).longValue();
        }
        if (rawStartTime instanceof LocalDateTime) {
            return toEpochMillis((LocalDateTime) rawStartTime);
        }
        if (rawStartTime instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    // 테이블 종료 시간 계산
    private Long toEpochMillis(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
