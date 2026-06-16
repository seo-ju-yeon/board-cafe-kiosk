/**
 * 관리자 대시보드 - 웹소켓 실시간 주문 동기화
 *
 * [제거된 항목]
 * - currentTableId 선언 → dashboard.html에서 선언
 * - openTableModal 래핑 → dashboard.html 원본 사용
 * - closeTableModal 래핑 → dashboard.html 원본 사용
 * - DOMContentLoaded 내 fetchPendingOrders 폴링 → dashboard.html에서 5초 폴링 수행
 */

let stompClient = null;
let tableOrderSubscription = null;
let commonChannelSubscribed = false;

// ===================================================
// 1. 웹소켓 연결 (자동 실행)
// ===================================================

function connectWebSocket() {
    if (stompClient && stompClient.connected) {
        console.log('이미 연결됨');
        return;
    }

    const socket = new SockJS('/ws/orders');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function(frame) {
        console.log('✅ WebSocket 연결됨');
        subscribeToChannels();
    }, function(error) {
        console.error('❌ WebSocket 연결 실패:', error);
        setTimeout(connectWebSocket, 3000);
    });
}

// ===================================================
// 2. 채널 구독
// ===================================================

function subscribeToChannels() {
    if (!stompClient || !stompClient.connected) return;
    if (commonChannelSubscribed) return;

    // 신규 일반 주문 알림
    stompClient.subscribe('/topic/new-orders', function(message) {
        const order = JSON.parse(message.body);
        console.log('🚨 신규 일반 주문:', order);
        onNewOrder(order);
    });

    // 신규 게임 요청 알림 (일반 주문 알림창과 분리)
    stompClient.subscribe('/topic/new-game-orders', function(message) {
        const order = JSON.parse(message.body);
        console.log('🎲 신규 게임 요청:', order);
        onNewGameOrder(order);
    });

    commonChannelSubscribed = true;
    console.log('✅ 채널 구독 완료');
}

// ===================================================
// 3. 테이블 선택 시 주문 구독
// ===================================================

function subscribeToTableOrders(tableId) {
    if (!stompClient || !stompClient.connected) {
        console.warn('WebSocket 미연결 - REST 폴백');
        if (typeof fetchActiveOrders === 'function') fetchActiveOrders();
        return;
    }

    // 기존 테이블 구독이 있으면 먼저 해제한다.
    unsubscribeFromOrders();

    // 테이블별 주문 구독
    tableOrderSubscription = stompClient.subscribe(`/topic/orders/${tableId}`, function(message) {
        const orders = JSON.parse(message.body);
        console.log(`📨 테이블 ${tableId} 주문:`, orders);
        onOrdersUpdated(orders, tableId);
    });

    // 초기 데이터 요청
    stompClient.send(`/app/subscribe/${tableId}`, {}, '');
    console.log(`✅ 테이블 ${tableId} 구독`);
}

// ===================================================
// 4. 구독 해제
// ===================================================

function unsubscribeFromOrders() {
    if (tableOrderSubscription) {
        try {
            tableOrderSubscription.unsubscribe();
            console.log('📴 주문 구독 해제');
        } catch (e) {
            console.warn('주문 구독 해제 실패:', e);
        } finally {
            tableOrderSubscription = null;
        }
    }
}

// ===================================================
// 5. 신규 주문 수신 처리
// ===================================================

function onNewOrder(order) {
    showNewOrderNotificationModal(order);
    playNotificationSound();
    if (typeof window.fetchPendingOrders === 'function') {
        window.fetchPendingOrders();
    }
    if (typeof refreshTableCardOrderBadges === 'function') {
        refreshTableCardOrderBadges();
    }
}

function onNewGameOrder(order) {
    // 게임 주문은 일반 주문 알림창과 분리: 목록만 갱신
    if (typeof window.fetchPendingOrders === 'function') {
        window.fetchPendingOrders();
    }
    if (typeof refreshTableCardOrderBadges === 'function') {
        refreshTableCardOrderBadges();
    }
}

function showNewOrderNotificationModal(order) {
    const modal = document.getElementById('newOrderNotificationModal');
    if (!modal) {
        // 대시보드 템플릿에 모달이 없는 버전도 있으므로 예외 없이 종료
        console.warn('⚠️ newOrderNotificationModal 없음 - 모달 표시 생략');
        return;
    }

    const tableId = order.tableId || order.table_id || '?';
    const totalAmount = order.totalAmount || order.total_amount || 0;

    document.getElementById('notificationOrderText').innerText =
        `Table ${tableId}번 - ₩${totalAmount.toLocaleString()}`;

    const items = order.items || order.orderItems || [];
    const itemSummary = items
        .map(i => {
            const menuName = i.menu_name || i.menuName || '상품';
            const qty = i.quantity || i.qty || 0;
            return `${menuName} x${qty}`;
        })
        .join(', ');

    document.getElementById('notificationItemsText').innerText = itemSummary || '항목 없음';

    const detailsHtml = items
        .map(i => {
            const menuName = i.menu_name || i.menuName || '상품명 없음';
            const qty = i.quantity || i.qty || 0;
            const price = i.price || 0;
            const total = price * qty;
            return `<div>• ${menuName} x${qty} = ₩${total.toLocaleString()}</div>`;
        })
        .join('');

    document.getElementById('notificationDetailsText').innerHTML = detailsHtml || '<div>항목 없음</div>';

    modal.style.display = 'flex';

    const content = modal.querySelector('.modal-content');
    content.style.transform = 'scale(0.8)';
    content.style.opacity = '0';
    setTimeout(() => {
        content.style.transform = 'scale(1)';
        content.style.opacity = '1';
        content.style.transition = 'all 0.3s cubic-bezier(0.36, 0, 0.66, 1)';
    }, 10);

    try {
        const utterance = new SpeechSynthesisUtterance(`테이블 ${tableId}번 신규 주문입니다`);
        utterance.lang = 'ko-KR';
        utterance.rate = 1;
        utterance.pitch = 1;
        speechSynthesis.speak(utterance);
    } catch (e) {
        console.log('음성 알림 불가:', e);
    }

    console.log('✅ 신규 주문 알림 모달 표시');
}

function closeNewOrderNotification() {
    const modal = document.getElementById('newOrderNotificationModal');
    if (modal) {
        const content = modal.querySelector('.modal-content');
        if (content) {
            content.style.transform = 'scale(0.8)';
            content.style.opacity = '0';
        }
        setTimeout(() => {
            modal.style.display = 'none';
            document.querySelectorAll('.table-card').forEach(card => {
                card.style.backgroundColor = '';
                card.style.border = '';
                card.style.boxShadow = '';
            });
        }, 300);
    }
    console.log('✅ 신규 주문 알림 모달 닫음');
}

function playNotificationSound() {
    try {
        const audioContext = new (window.AudioContext || window.webkitAudioContext)();
        const now = audioContext.currentTime;

        const osc = audioContext.createOscillator();
        const gain = audioContext.createGain();
        osc.connect(gain);
        gain.connect(audioContext.destination);

        osc.frequency.value = 1000;
        gain.gain.setValueAtTime(0.3, now);
        gain.gain.exponentialRampToValueAtTime(0.01, now + 0.2);
        osc.start(now);
        osc.stop(now + 0.2);

        setTimeout(() => {
            const osc2 = audioContext.createOscillator();
            const gain2 = audioContext.createGain();
            osc2.connect(gain2);
            gain2.connect(audioContext.destination);

            osc2.frequency.value = 1200;
            const now2 = audioContext.currentTime;
            gain2.gain.setValueAtTime(0.3, now2);
            gain2.gain.exponentialRampToValueAtTime(0.01, now2 + 0.2);
            osc2.start(now2);
            osc2.stop(now2 + 0.2);
        }, 250);

        console.log('🔊 사운드 알림 재생');
    } catch (e) {
        console.log('⚠️ 사운드 알림 불가:', e.message);
    }
}

// ===================================================
// 6. 주문 목록 업데이트 처리
// ===================================================

function onOrdersUpdated(orders, tableId) {
    // 대기 주문 영역도 함께 갱신해서 신규 주문 누락을 방지
    if (typeof window.fetchPendingOrders === 'function') {
        window.fetchPendingOrders();
    }
    // 모달은 REST 재조회로 렌더링(활성 대여 목록 포함)
    if (typeof fetchActiveOrders === 'function') {
        fetchActiveOrders();
    }
    // 대시보드 카드의 상단 주문 상태 배지도 최신화
    if (typeof refreshTableCardOrderBadges === 'function') {
        refreshTableCardOrderBadges();
    }
}

// ===================================================
// 7. 주문 상태 변경 API
// ===================================================

async function updateOrderStatusViaApi(orderId, nextStatus) {
    if (!orderId) {
        alert('주문 번호를 찾을 수 없습니다.');
        return;
    }

    let confirmMsg = '주문 상태를 변경하시겠습니까?';
    if (nextStatus === 'CANCELLED') confirmMsg = '정말 이 주문을 취소하시겠습니까? (복구 불가)';
    else if (nextStatus === 'CONFIRMED') confirmMsg = '주문 내역을 주방에서 확인하셨습니까?';
    else if (nextStatus === 'COOKING') confirmMsg = '조리를 시작하시겠습니까?';
    else if (nextStatus === 'DELIVERING') confirmMsg = '서빙을 시작하시겠습니까?';
    else if (nextStatus === 'COMPLETED') confirmMsg = '서빙 완료 처리하시겠습니까?';

    if (!confirm(confirmMsg)) return;

    try {
        console.log('📝 주문 상태 변경 요청:', { orderId, nextStatus });

        // 버튼에서 요청한 상태를 그대로 서버에 전달한다.
        // (대여 주문은 CANCELLED 같은 분기 상태가 있어 next-status 강제 보정과 충돌함)
        const statusToSend = nextStatus;

        // 관리자 대시보드 전용 API로 상태 변경
        const response = await fetch(`/admin/api/dashboard/orders/${orderId}/status`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ status: statusToSend })
        });

        if (response.ok) {
            if (typeof fetchActiveOrders === 'function') await fetchActiveOrders();
            alert('주문 상태가 변경되었습니다.');
            return;
        }

        const errorData = await response.json().catch(() => ({ message: response.statusText }));
        const errorMessage = errorData.message || errorData.error || "주문 상태 변경에 실패했습니다.";

        // 동시성 충돌(이미 같은 상태/다른 상태로 변경됨) 시 최신값으로 재조회 후 안내
        if (typeof errorMessage === 'string' && errorMessage.includes('허용되지 않는 상태 전이')) {
            console.warn('⚠️ 상태 전이 충돌 감지 - 최신 상태 재조회', { orderId, statusToSend, errorMessage });
            if (typeof fetchActiveOrders === 'function') await fetchActiveOrders();
            alert('다른 화면에서 먼저 상태가 변경되었습니다. 최신 상태로 갱신했습니다.');
            return;
        }
        console.error('❌ 상태 변경 실패:', response.status, errorData);
        alert("❌ " + errorMessage);
    } catch (err) {
        console.error('❌ updateOrderStatus 에러:', err);
        alert("서버와 통신 중 오류가 발생했습니다: " + err.message);
    }
}

if (typeof window.updateOrderStatus !== 'function') {
    window.updateOrderStatus = updateOrderStatusViaApi;
}

// ===================================================
// 9. 초기화 (페이지 로드 시) - WebSocket 연결만 담당
// ===================================================

document.addEventListener('DOMContentLoaded', () => {
    connectWebSocket();
});
