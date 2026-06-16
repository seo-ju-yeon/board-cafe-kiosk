package org.example.board_cafe_kiosk_2603.service.admin.point;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.point.Customer;
import org.example.board_cafe_kiosk_2603.domain.admin.point.Point;
import org.example.board_cafe_kiosk_2603.domain.admin.point.PointHistory;
import org.example.board_cafe_kiosk_2603.dto.admin.point.PointAdminDTO;
import org.example.board_cafe_kiosk_2603.dto.admin.point.PointHistoryDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageRequestDTO;
import org.example.board_cafe_kiosk_2603.dto.common.pagination.PageResponseDTO;
import org.example.board_cafe_kiosk_2603.mapper.admin.point.CustomerMapper;
import org.example.board_cafe_kiosk_2603.mapper.admin.point.PointMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class PointService {
    private final CustomerMapper customerMapper;
    private final PointMapper pointMapper;

    // ===================================================
    // 조회
    // ===================================================

    /** 전체 포인트 계좌 목록 (관리자 화면용) */
    public List<PointAdminDTO> getAllPoints() {
        return pointMapper.findAll().stream()
                .map(PointAdminDTO::from)
                .collect(Collectors.toList());
    }

    /** 전화번호로 포인트 계좌 조회 — 없으면 null */
    public PointAdminDTO getPointByPhone(String phone) {
        Point point = pointMapper.findByPhone(phone);
        return point != null ? PointAdminDTO.from(point) : null;
    }

    /** 특정 계좌의 이력 목록 */
    public List<PointHistoryDTO> getHistoryByPointId(int pointId) {
        return pointMapper.findHistoryByPointId(pointId).stream()
                .map(PointHistoryDTO::from)
                .collect(Collectors.toList());
    }

    // ===================================================
    // 통계
    // ===================================================

    public int getTotalCustomers() { return pointMapper.countAll(); }

    public int getTotalPoints()    { return pointMapper.sumTotalBalance(); }

    public int getAvgPoints() {
        int count = pointMapper.countAll();
        return count == 0 ? 0 : pointMapper.sumTotalBalance() / count;
    }

    // ===================================================
    // 계좌 생성
    // ===================================================

    /** 포인트 계좌 신규 생성 (이력 없음, 키오스크 신규 회원용) */
    @Transactional
    public void createAccount(String phone) {
        // 신규가입 시 customer + point를 함께 보장한다.
        getOrCreatePoint(phone);
        log.info("신규 회원 계정 준비 완료 - 전화번호: {}", phone);
    }

    // ===================================================
    // 포인트 적립 (EARN)
    // ===================================================

    @Transactional
    public void earnPoint(String phone, int amount, Long orderId) {
        if (amount <= 0) {
            return;
        }

        // 같은 주문에서 포인트 사용(USE)이 있었다면 적립(EARN) 금지
        if (orderId != null && pointMapper.countUseHistoryByOrderId(orderId) > 0) {
            String message = "포인트 사용이 포함된 주문은 적립할 수 없습니다. orderId: " + orderId;
            log.warn("포인트 적립 차단 - {} phone: {}", message, phone);
            throw new IllegalStateException(message);
        }

        Point point = getOrCreatePoint(phone);

        int newBalance = point.getBalance() + amount;
        pointMapper.updateBalance(Point.builder()
                .id(point.getId())
                .phone(point.getPhone())
                .balance(newBalance)
                .build());

        pointMapper.insertHistory(PointHistory.builder()
                .pointId(point.getId())
                .orderId(orderId)
                .type("EARN")
                .amount(amount)
                .balanceAfter(newBalance)
                .build());

        log.info("포인트 적립 - 전화번호: {}, 적립: {}P, 잔액: {}P", phone, amount, newBalance);
    }

    // ===================================================
    // 포인트 사용 (USE)
    // ===================================================

    @Transactional
    public void usePoint(String phone, int amount, Long orderId) {
        Point point = pointMapper.findByPhone(phone);
        if (point == null) {
            throw new IllegalArgumentException("포인트 계좌를 찾을 수 없습니다: " + phone);
        }
        if (point.getBalance() < amount) {
            throw new IllegalArgumentException(
                    "포인트 잔액이 부족합니다. 현재 잔액: " + point.getBalance());
        }

        int newBalance = point.getBalance() - amount;
        pointMapper.updateBalance(Point.builder()
                .id(point.getId())
                .phone(point.getPhone())
                .balance(newBalance)
                .build());

        pointMapper.insertHistory(PointHistory.builder()
                .pointId(point.getId())
                .orderId(orderId)
                .type("USE")
                .amount(amount)
                .balanceAfter(newBalance)
                .build());

        log.info("포인트 사용 - 전화번호: {}, 사용: {}P, 잔액: {}P", phone, amount, newBalance);
    }

    // ===================================================
    // 헬퍼
    // ===================================================

    private Point getOrCreatePoint(String phone) {
        // customer 테이블에도 없으면 생성
        if (customerMapper.selectByPhone(phone) == null) {
            customerMapper.insertCustomer(Customer.builder()
                    .phone(phone)
                    .isActive(true)
                    .build());
            log.info("신규 고객 등록 - 전화번호: {}", phone);
        }

        Point point = pointMapper.findByPhone(phone);
        if (point == null) {
            pointMapper.insert(Point.builder().phone(phone).balance(0).build());
            point = pointMapper.findByPhone(phone);
        }
        return point;
    }

    /* 페이징 처리 */
    public PageResponseDTO<PointAdminDTO> getPagedPoints(PageRequestDTO pageRequestDTO) {

        // 1. DB에서 페이징 처리된 목록(VO/Domain) 가져오기
        List<Point> voList = pointMapper.selectList(pageRequestDTO);

        // 2. VO 리스트를 화면용 DTO 리스트로 변환
        List<PointAdminDTO> dtoList = voList.stream()
                .map(PointAdminDTO::from)
                .collect(Collectors.toList());

        // 3. 검색 조건에 맞는 전체 데이터 개수 조회 (페이징 버튼 계산용)
        int total = pointMapper.selectCount(pageRequestDTO);

        // 4. PageResponseDTO 생성 (작성하신 DTO의 생성자가 자동으로 start, end, prev, next 계산)
        return PageResponseDTO.<PointAdminDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(dtoList)
                .total(total)
                .build();
    }
}
