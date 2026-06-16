function navigateMenu(menu) {
    window.location.href = `/kiosk/${menu}`;
}

function navigateCart() {
    window.location.href = `/kiosk/cart`;
}

function navigateCheckout() {
    if (confirm('정산하시겠습니까?')) window.location.href = `/kiosk/checkout`;
}

function openServiceModal() {
    document.getElementById('serviceModal').classList.add('active');
}

function closeServiceModal() {
    document.getElementById('serviceModal').classList.remove('active');
}

/* ================= 스크린세이버 로직 추가 ================= */
let idleTimer;

function resetIdleTimer() {
    clearTimeout(idleTimer);
    // 30초 동안 활동이 없으면 screensaver로 이동
    idleTimer = setTimeout(() => {
        localStorage.setItem('returnUrl', window.location.pathname);
        window.location.href = '/kiosk/screensaver';
    }, 30000);
}

// 활동 감지 이벤트 리스너 등록
window.onload = resetIdleTimer;
window.onmousemove = resetIdleTimer;
window.onclick = resetIdleTimer;
window.onkeydown = resetIdleTimer;
window.ontouchstart = resetIdleTimer;

/* ======================================================= */

async function callService(serviceType) {
    try {
        const res = await fetch('/kiosk/service-request', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({serviceType, tableNumber: TABLE_NUMBER})
        });
        const data = await res.json().catch(() => ({success: false}));
        if (!res.ok || !data.success) {
            throw new Error(data.message || ('서버 오류 (' + res.status + ')'));
        }
        closeServiceModal();
        showToast(data.message || (serviceType + ' 요청이 전송되었습니다.'));
    } catch (e) {
        showToast('요청 실패: ' + e.message);
    }
}

function startTimer() {
    const timerText = document.getElementById('timer-text');
    const badge = document.getElementById('timer-badge');
    const formatHoursMinutes = (ms) => {
        const totalMinutes = Math.floor(ms / 60000);
        const hours = Math.floor(totalMinutes / 60);
        const minutes = totalMinutes % 60;
        return `${hours}시간 ${minutes}분`;
    };

    if (!SESSION_START_TIME || !DURATION_MINUTES) {
        if (timerText) timerText.innerText = "자유이용";
        return;
    }

    const endTime = SESSION_START_TIME + (DURATION_MINUTES * 60 * 1000);

    function tick() {
        const now = Date.now();
        const remaining = endTime - now;
        if (remaining > 0) {
            timerText.innerText = formatHoursMinutes(remaining);
            badge.classList.remove('overtime');
        } else {
            const overtime = Math.abs(remaining);
            timerText.innerText = `+${formatHoursMinutes(overtime)}`;
            badge.classList.add('overtime');
        }
    }

    tick();
    setInterval(tick, 1000);
}

function showToast(message, durationMs = 2000) {
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.innerText = message;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), durationMs);
}

async function addItemToCartDirect(btn) {
    const item = btn.closest('.menu-item');
    const menuName = item.dataset.name;
    const menuPrice = parseInt(item.dataset.price, 10);

    try {
        const res = await fetch('/kiosk/cart/add', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({menuName, menuPrice, quantity: 1})
        });
        const cart = await res.json().catch(() => ({}));
        if (!res.ok || cart.success === false) {
            throw new Error(cart.message || ('서버 오류 (' + res.status + ')'));
        }

        const badge = document.getElementById('cart-badge');
        const count = cart.cartCount ?? (cart.cartItems ? cart.cartItems.length : 0);
        badge.textContent = count;
        badge.style.display = count > 0 ? 'flex' : 'none';

        showToast(menuName + ' 담겼습니다');
    } catch (e) {
        showToast('담기 실패: ' + e.message);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    startTimer();
    startTableStatusWatcher();
    startOrderStatusWatcher();
    startStaffMacroWatcher();
});

function startTableStatusWatcher() {
    async function checkTableStatus() {
        try {
            const res = await fetch('/kiosk/table/status', {
                headers: {'Accept': 'application/json'},
                credentials: 'same-origin'
            });
            if (!res.ok) return;

            const contentType = res.headers.get('content-type') || '';
            if (!contentType.includes('application/json')) {
                if (res.redirected && res.url) window.location.href = res.url;
                return;
            }

            const data = await res.json();
            if (data?.success && data.status === 'CLEANING') {
                window.location.href = '/kiosk/cleaning_wait';
            }
        } catch (err) {
            console.error('테이블 상태 확인 실패:', err);
        }
    }

    checkTableStatus();
    setInterval(checkTableStatus, 3000);
}

function startStaffMacroWatcher() {
    async function checkStaffMessages() {
        try {
            const res = await fetch('/kiosk/messages/staff/unread', {
                headers: {'Accept': 'application/json'},
                credentials: 'same-origin'
            });
            if (!res.ok) return;
            const contentType = res.headers.get('content-type') || '';
            if (!contentType.includes('application/json')) {
                if (res.redirected && res.url) window.location.href = res.url;
                return;
            }
            const data = await res.json();
            const messages = Array.isArray(data?.messages) ? data.messages : [];
            if (!messages.length) return;

            for (const msg of messages) {
                if (msg?.content) {
                    showToast('📨 ' + msg.content, 4000);
                }
                if (msg?.id) {
                    await fetch(`/kiosk/messages/${msg.id}/read`, {method: 'PATCH'});
                }
            }
        } catch (err) {
            console.error('메크로 메시지 확인 실패:', err);
        }
    }

    checkStaffMessages();
    setInterval(checkStaffMessages, 5000);
}

// 주문 상태 감시 함수
function startOrderStatusWatcher() {
    const statusBox = document.getElementById('order-status-box');
    const statusText = document.getElementById('order-status-text');
    const gameStatusBox = document.getElementById('game-status-box');
    const gameStatusText = document.getElementById('game-status-text');
    let lastOrderCount = 0;

    const isGameOnlyOrder = (order) => {
        const items = Array.isArray(order?.items) ? order.items : (Array.isArray(order?.orderItems) ? order.orderItems : []);
        if (!items.length) return false;
        return items.every(item => Number(item?.price ?? item?.menuPrice ?? 0) === 0);
    };

    const toStatusLabel = (status, gameFlow) => {
        const normalLabels = {
            ORDERED: '주문 완료',
            CONFIRMED: '주문 확인중...',
            COOKING: '조리 중...',
            DELIVERING: '배달 시작',
            COMPLETED: '완료',
            CANCELLED: '취소'
        };
        const gameLabels = {
            ORDERED: '게임 요청',
            CONFIRMED: '대여 확정',
            CANCELLED: '취소'
        };
        return (gameFlow ? gameLabels : normalLabels)[status] || status;
    };

    const goToOrderDetail = () => {
        const orderId = statusBox.dataset.orderId;
        if (!orderId) return;
        window.location.href = `/kiosk/order/${orderId}`;
    };
    const goToGameDetail = () => {
        const orderId = gameStatusBox.dataset.orderId;
        if (!orderId) return;
        window.location.href = `/kiosk/order/game/${orderId}`;
    };

    statusBox.addEventListener('click', goToOrderDetail);
    statusBox.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            goToOrderDetail();
        }
    });
    gameStatusBox.addEventListener('click', goToGameDetail);
    gameStatusBox.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            goToGameDetail();
        }
    });

    function checkOrderStatus() {
        fetch('/kiosk/order/active')
            .then(async response => {
                if (!response.ok) return [];
                const contentType = response.headers.get('content-type') || '';
                if (!contentType.includes('application/json')) return [];
                return response.json();
            })
            .then(orders => {
                if (!Array.isArray(orders) || orders.length === 0) {
                    statusBox.classList.add('hidden');
                    statusBox.dataset.orderId = '';
                    gameStatusBox.classList.add('hidden');
                    gameStatusBox.dataset.orderId = '';
                    lastOrderCount = 0;
                    return;
                }

                // 완료되지 않은 주문만 필터링
                const activeOrders = orders.filter(o => o.status !== 'COMPLETED' && o.status !== 'CANCELLED');
                const normalOrders = activeOrders.filter(o => !isGameOnlyOrder(o));
                const gameOrders = activeOrders.filter(o => isGameOnlyOrder(o));

                if (activeOrders.length === 0) {
                    statusBox.classList.add('hidden');
                    statusBox.dataset.orderId = '';
                    gameStatusBox.classList.add('hidden');
                    gameStatusBox.dataset.orderId = '';
                    lastOrderCount = 0;
                    return;
                }

                if (normalOrders.length > 0) {
                    const latestNormal = normalOrders
                        .slice()
                        .sort((a, b) => Number(b?.id || 0) - Number(a?.id || 0))[0];
                    const statusLabel = toStatusLabel(latestNormal.status, false);
                    statusText.textContent = statusLabel;
                    statusBox.dataset.orderId = latestNormal.id ? String(latestNormal.id) : '';
                    statusBox.classList.remove('hidden');
                } else {
                    statusBox.classList.add('hidden');
                    statusBox.dataset.orderId = '';
                }

                if (gameOrders.length > 0) {
                    const latestGame = gameOrders
                        .slice()
                        .sort((a, b) => Number(b?.id || 0) - Number(a?.id || 0))[0];
                    const statusLabel = toStatusLabel(latestGame.status, true);
                    gameStatusText.textContent = statusLabel;
                    gameStatusBox.dataset.orderId = latestGame.id ? String(latestGame.id) : '';
                    gameStatusBox.classList.remove('hidden');
                } else {
                    gameStatusBox.classList.add('hidden');
                    gameStatusBox.dataset.orderId = '';
                }

                lastOrderCount = activeOrders.length;
            })
            .catch(err => {
                console.error('주문 상태 확인 실패:', err);
            });
    }

    // 초기 실행 및 5초마다 체크
    checkOrderStatus();
    setInterval(checkOrderStatus, 5000);
}