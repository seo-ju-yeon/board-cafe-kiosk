package org.example.board_cafe_kiosk_2603.mapper.kiosk.payment;

import org.apache.ibatis.annotations.Mapper;
import org.example.board_cafe_kiosk_2603.domain.kiosk.payment.Payment;

/**
 * 결제(Payment) 관련 MyBatis Mapper 인터페이스
 * Payment 엔티티의 데이터베이스 CRUD 작업
 */
@Mapper
public interface PaymentMapper {

    /**
     * 결제 레코드 생성
     * @param payment 저장할 Payment 객체
     */
    void insert(Payment payment);

    /**
     * 결제 키로 결제 조회 (중복 결제 방지)
     * @param paymentKey 토스 결제 키
     * @return Payment 객체, 없으면 null
     */
    Payment findByPaymentKey(String paymentKey);

    /**
     * 토스용 주문번호로 결제 조회
     * @param orderIdToss 토스용 주문번호
     * @return Payment 객체, 없으면 null
     */
    Payment findByOrderIdToss(String orderIdToss);

    /**
     * 세션 ID로 결제 조회
     * @param sessionId 세션 ID
     * @return Payment 객체, 없으면 null
     */
    Payment findBySessionId(long sessionId);

    /**
     * 결제 상태 업데이트 (READY → DONE)
     * @param payment 업데이트할 Payment 객체
     */
    void updateStatus(Payment payment);

    /**
     * 결제 ID로 조회
     * @param id 결제 ID
     * @return Payment 객체, 없으면 null
     */
    Payment findById(int id);
}
