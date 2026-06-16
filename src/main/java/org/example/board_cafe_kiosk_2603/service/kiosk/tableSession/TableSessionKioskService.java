package org.example.board_cafe_kiosk_2603.service.kiosk.tableSession;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;

import java.util.Map;

public interface TableSessionKioskService {
    /**
     * 패키지 선택이 완료되면 테이블에 연결된 새 이용 세션을 생성한다.
     */
    Long createSession(int tableId, int packageId, int initialGuestCnt);

    /**
     * 장바구니 화면에 필요한 테이블 정보와 장바구니 합계를 담는다.
     */
    void buildCartModel(Model model, int tableNumber, HttpSession session);

    /**
     * 정산 화면에 필요한 세션, 패키지, 포인트, 장바구니 정보를 담는다.
     */
    void buildCheckoutModel(Model model, int tableNumber, HttpSession session);

    /**
     * 관리자 대시보드에 실시간 정산 계산에 사용할 메타 정보를 반환.
     */
    Map<String, Object> buildCheckoutMeta(Integer tableId);
}
