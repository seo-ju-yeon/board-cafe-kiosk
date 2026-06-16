//package org.example.board_cafe_kiosk_2603.service.admin;
//
//import lombok.extern.log4j.Log4j2;
//import org.example.board_cafe_kiosk_2603.domain.admin.point.Point;
//import org.example.board_cafe_kiosk_2603.dto.admin.point.PointAdminDTO;
//import org.example.board_cafe_kiosk_2603.mapper.admin.point.PointMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.BDDMockito.*;
//
//@Log4j2
//@ExtendWith(MockitoExtension.class)
//class PointServiceTest {
//
//    @Mock private PointMapper pointMapper;
//
//    @InjectMocks
//    private PointService pointService;
//
//    private Point mockPoint;
//
//    @BeforeEach
//    void setUp() {
//        mockPoint = Point.builder().id(1).phone("010-1234-5678").balance(5000).build();
//    }
//
//    // ===================================================
//    // getPointByPhone
//    // ===================================================
//
//    @Test
//    void getPointByPhone_success() {
//        given(pointMapper.findByPhone("010-1234-5678")).willReturn(mockPoint);
//
//        PointAdminDTO result = pointService.getPointByPhone("010-1234-5678");
//
//        assertThat(result).isNotNull();
//        assertThat(result.getBalance()).isEqualTo(5000);
//        assertThat(result.getPhone()).isEqualTo("010-1234-5678");
//    }
//
//    @Test
//    void getPointByPhone_notFound() {
//        given(pointMapper.findByPhone("000-0000-0000")).willReturn(null);
//
//        assertThat(pointService.getPointByPhone("000-0000-0000")).isNull();
//    }
//
//    // ===================================================
//    // createAccount
//    // ===================================================
//
//    @Test
//    void createAccount_newAccount() {
//        given(pointMapper.findByPhone("010-9999-9999")).willReturn(null);
//
//        pointService.createAccount("010-9999-9999");
//
//        then(pointMapper).should().insert(any(Point.class));
//    }
//
//    @Test
//    void createAccount_alreadyExists_skips() {
//        given(pointMapper.findByPhone("010-1234-5678")).willReturn(mockPoint);
//
//        pointService.createAccount("010-1234-5678");
//
//        then(pointMapper).should(never()).insert(any());
//    }
//
//    // ===================================================
//    // earnPoint
//    // ===================================================
//
//    @Test
//    void earnPoint_success() {
//        given(pointMapper.findByPhone("010-1234-5678")).willReturn(mockPoint);
//
//        pointService.earnPoint("010-1234-5678", 500, null);
//
//        then(pointMapper).should().updateBalance(argThat(p -> p.getBalance() == 5500));
//        then(pointMapper).should().insertHistory(argThat(h ->
//                "EARN".equals(h.getType()) && h.getAmount() == 500 && h.getBalanceAfter() == 5500));
//    }
//
//    @Test
//    void earnPoint_autoCreateAccount() {
//        given(pointMapper.findByPhone("010-0000-0000"))
//                .willReturn(null)
//                .willReturn(Point.builder().id(2).phone("010-0000-0000").balance(0).build());
//
//        pointService.earnPoint("010-0000-0000", 300, null);
//
//        then(pointMapper).should().insert(any(Point.class));
//        then(pointMapper).should().updateBalance(argThat(p -> p.getBalance() == 300));
//    }
//
//    @Test
//    void earnPoint_withOrderId() {
//        given(pointMapper.findByPhone("010-1234-5678")).willReturn(mockPoint);
//
//        pointService.earnPoint("010-1234-5678", 200, 42);
//
//        then(pointMapper).should().insertHistory(
//                argThat(h -> h.getOrderId() != null && h.getOrderId() == 42));
//    }
//
//    // ===================================================
//    // usePoint
//    // ===================================================
//
//    @Test
//    void usePoint_success() {
//        given(pointMapper.findByPhone("010-1234-5678")).willReturn(mockPoint);
//
//        pointService.usePoint("010-1234-5678", 1000, null);
//
//        then(pointMapper).should().updateBalance(argThat(p -> p.getBalance() == 4000));
//        then(pointMapper).should().insertHistory(argThat(h ->
//                "USE".equals(h.getType()) && h.getAmount() == 1000 && h.getBalanceAfter() == 4000));
//    }
//
//    @Test
//    void usePoint_accountNotFound_throws() {
//        given(pointMapper.findByPhone("010-9999-9999")).willReturn(null);
//
//        assertThatThrownBy(() -> pointService.usePoint("010-9999-9999", 500, null))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("포인트 계좌를 찾을 수 없습니다");
//    }
//
//    @Test
//    void usePoint_insufficientBalance_throws() {
//        given(pointMapper.findByPhone("010-1234-5678")).willReturn(mockPoint);
//
//        assertThatThrownBy(() -> pointService.usePoint("010-1234-5678", 9999, null))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("포인트 잔액이 부족합니다");
//    }
//
//    // ===================================================
//    // 통계
//    // ===================================================
//
//    @Test
//    void getTotalCustomers() {
//        given(pointMapper.countAll()).willReturn(10);
//        assertThat(pointService.getTotalCustomers()).isEqualTo(10);
//    }
//
//    @Test
//    void getTotalPoints() {
//        given(pointMapper.sumTotalBalance()).willReturn(50000);
//        assertThat(pointService.getTotalPoints()).isEqualTo(50000);
//    }
//
//    @Test
//    void getAvgPoints() {
//        given(pointMapper.countAll()).willReturn(4);
//        given(pointMapper.sumTotalBalance()).willReturn(20000);
//        assertThat(pointService.getAvgPoints()).isEqualTo(5000);
//    }
//
//    @Test
//    void getAvgPoints_noCustomers_returnsZero() {
//        given(pointMapper.countAll()).willReturn(0);
//        assertThat(pointService.getAvgPoints()).isEqualTo(0);
//    }
//
//    @Test
//    void getAllPoints() {
//        given(pointMapper.findAll()).willReturn(List.of(mockPoint));
//        assertThat(pointService.getAllPoints()).hasSize(1);
//    }
//}
