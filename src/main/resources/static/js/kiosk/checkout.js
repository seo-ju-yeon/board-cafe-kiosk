// URL 파라미터
const urlParams = new URLSearchParams(window.location.search);
const orderId   = urlParams.get('orderId');

// ===== 전역 상태 변수 =====
let subtotalPrice   = 0;
let menuSubtotal    = 0;
let appliedPoint    = 0;
let currentPointBalance = initialPointBalance;  // 중복 선언 제거, 이름 변경
let widgets         = null;
let orderId_toss    = null;
let orderName       = '';

// ===== 토스트 알림 =====
function showToast(msg) {
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.innerText = msg;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 2500);
}

// ===== 이용 시간 계산 =====
function normalizeEpochMillis(rawValue) {
    const n = Number(rawValue);
    if (!n || Number.isNaN(n)) return 0;
    // 혹시 초 단위 값이 들어오면 ms 단위로 보정
    if (n > 0 && n < 100000000000) return n * 1000;
    return n;
}

function updateElapsedTime() {
    const startMs = normalizeEpochMillis(sessionStartTime);
    if (!startMs || Number.isNaN(startMs)) {
        document.getElementById('elapsed-time').innerText = '0시간 0분';
        return;
    }
    const elapsedRaw = Math.floor((Date.now() - startMs) / 60000);
    const elapsed = Math.max(elapsedRaw, 0);

    // "추가 시간" 표시는 패키지 기본시간을 제외한 초과분만 보여준다.
    const extraMinutesForDisplay =
        (durationMinutes != null && durationMinutes > 0)
            ? Math.max(elapsed - durationMinutes, 0)
            : 0;
    const hours   = Math.floor(extraMinutesForDisplay / 60);
    const minutes = extraMinutesForDisplay % 60;
    document.getElementById('elapsed-time').innerText = `${hours}시간 ${minutes}분`;

    // 매번 기본 금액으로 초기화 후 초과 요금 반영
    subtotalPrice = packageTotal + menuSubtotal;
    document.getElementById('over-charge-row').style.display = 'none';

    // 초과 요금 계산
    if (durationMinutes > 0 && extraPricePerMin > 0) {
        const overMinutes = elapsed - durationMinutes;
        if (overMinutes > 0) {
            const overUnits  = Math.ceil(overMinutes / 10); // 10분 단위 올림
            const overCharge = overUnits * extraPricePerMin * partySize;
            subtotalPrice = packageTotal + menuSubtotal + overCharge;
            updateAmounts();

            document.getElementById('over-charge-row').style.display = 'flex';
            document.getElementById('over-charge-amount').innerText =
                `₩${overCharge.toLocaleString()} (+${overMinutes}분 초과)`;
        }
    }

    updateAmounts();
}

// ===== 금액 업데이트 =====
function updateAmounts() {
    const finalAmount = Math.max(subtotalPrice - appliedPoint, 0);
    document.getElementById('subtotal-amount').innerText = `₩${subtotalPrice.toLocaleString()}`;
    document.getElementById('final-total').innerText     = `₩${finalAmount.toLocaleString()}`;

    const discountRow = document.getElementById('point-discount-row');
    if (appliedPoint > 0) {
        discountRow.style.display = 'flex';
        document.getElementById('point-discount-amount').innerText = `-₩${appliedPoint.toLocaleString()}`;
    } else {
        discountRow.style.display = 'none';
    }
}

// ===== 위젯 금액 동기화 =====
async function updateWidgetAmount() {
    if (widgets) {
        await widgets.setAmount({
            currency: "KRW",
            value: Math.max(subtotalPrice - appliedPoint, 0)
        });
    }
}

// ===== 정산 데이터 로드 (세션 주문 기반) =====
async function loadCheckout() {
    try {
        const response = await fetch(`/kiosk/order/active`);
        if (!response.ok) {
            renderCheckout([]);
            return;
        }
        const contentType = response.headers.get('content-type') || '';
        if (!contentType.includes('application/json')) {
            if (response.redirected && response.url) {
                window.location.href = response.url;
            }
            renderCheckout([]);
            return;
        }
        const orders = await response.json();
        renderCheckout(orders);
    } catch (err) {
        console.error('Checkout 로드 실패:', err);
        renderCheckout([]);
    }
}

// ===== 포인트 잔액 조회 =====
async function fetchPointBalance() {
    try {
        if (!customerPhone) return;

        const response = await fetch(`/kiosk/point/lookup?phone=${encodeURIComponent(customerPhone)}`);
        if (!response.ok) return;
        const contentType = response.headers.get('content-type') || '';
        if (!contentType.includes('application/json')) return;
        const data     = await response.json();
        currentPointBalance = data.balance || 0;

        // 포인트 잔액 뱃지 업데이트 (있으면)
        const badge = document.querySelector('.point-balance-badge');
        if (badge) {
            badge.innerText = `보유 ${currentPointBalance.toLocaleString()}P`;
        }
    } catch (err) {
        console.error('포인트 조회 실패:', err);
    }
}

// ===== 정산 내역 렌더링 =====
function renderCheckout(orders) {
    const normalizedOrders = Array.isArray(orders) ? orders : [];
    const activeOrders = normalizedOrders.filter(o => o && o.status !== 'CANCELLED');
    menuSubtotal  = activeOrders.reduce((sum, o) => sum + o.totalAmount, 0);
    subtotalPrice = packageTotal + menuSubtotal;

    if (activeOrders.length === 0) {
        document.getElementById('order-summary').innerHTML = `
                <div class="card-header"><i data-lucide="shopping-bag"></i> 주문 내역</div>
                <div style="text-align:center; color:#888; padding: 16px 0;">주문 내역이 없습니다.</div>`;
        updateAmounts();
        lucide.createIcons();
        return;
    }

    const rows = activeOrders.flatMap(order => {
        const items = Array.isArray(order.items) ? order.items : [];
        return items.map(item => `
                <div class="order-item">
                    <div>
                        <span>${item.menuName}</span>
                        <span class="order-qty">x${item.quantity}</span>
                    </div>
                    <div class="price">₩${(item.price * item.quantity).toLocaleString()}</div>
                </div>`);
    }).join('');

    document.getElementById('order-summary').innerHTML = `
            <div class="card-header"><i data-lucide="shopping-bag"></i> 주문 내역</div>
            ${rows}
            <div class="summary-divider">
                <span>메뉴 소계</span>
                <span>₩${menuSubtotal.toLocaleString()}</span>
            </div>`;

    updateAmounts();
    updateElapsedTime();
    lucide.createIcons();
}

// ===== 포인트 적용 (전역 함수 — onclick에서 호출) =====
async function applyPoint() {
    const input = document.getElementById('point-input');
    const val   = parseInt(input.value) || 0;

    if (val <= 0) {
        showToast('1P 이상 입력해주세요.');
        return;
    }
    if (val > currentPointBalance) {
        showToast('보유 포인트를 초과할 수 없습니다.');
        return;
    }
    if (val > subtotalPrice) {
        showToast('결제 금액을 초과할 수 없습니다.');
        return;
    }

    appliedPoint = val;
    updateAmounts();
    await updateWidgetAmount();

    document.getElementById('point-applied-amount').innerText = val.toLocaleString();
    document.getElementById('point-applied-msg').style.display = 'block';
    input.disabled = true;
    showToast(`${val.toLocaleString()}P 적용되었습니다.`);
}

// ===== 포인트 취소 (전역 함수 — onclick에서 호출) =====
async function resetPoint() {
    appliedPoint = 0;
    const input  = document.getElementById('point-input');
    if (input) {
        input.value    = '';
        input.disabled = false;
    }
    document.getElementById('point-applied-msg').style.display = 'none';
    updateAmounts();
    await updateWidgetAmount();
}

// ===== 토스페이먼츠 위젯 초기화 =====
async function initializeTossWidget() {
    try {
        const prepRes = await fetch(`/kiosk/payment/prepare?tableNumber=${tableNumber}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ pointUsed: appliedPoint })
        });

        const prepData = await prepRes.json();
        if (!prepData.success) {
            console.error('결제 준비 실패:', prepData.message);
            showToast('결제 준비에 실패했습니다.');
            return;
        }

        orderId_toss    = prepData.orderIdToss;
        orderName       = prepData.orderName;
        const cKey      = prepData.clientKey;
        const customerKey = prepData.customerKey;

        const tossPayments = TossPayments(cKey);
        widgets = tossPayments.widgets({ customerKey });

        await widgets.setAmount({
            currency: "KRW",
            value: Math.max(subtotalPrice - appliedPoint, 0)
        });

        await Promise.all([
            widgets.renderPaymentMethods({ selector: "#payment-widget", variantKey: "DEFAULT" }),
            widgets.renderAgreement({ selector: "#agreement", variantKey: "DEFAULT" })
        ]);

    } catch (err) {
        console.error('위젯 초기화 오류:', err);
        showToast('결제 준비 중 오류가 발생했습니다.');
    }
}

// ===== 결제 처리 (전역 함수 — onclick에서 호출) =====
async function processPayment() {
    if (subtotalPrice === 0) {
        showToast('결제할 금액이 없습니다.');
        return;
    }
    if (!widgets || !orderId_toss) {
        showToast('결제 준비가 완료되지 않았습니다.');
        return;
    }

    const payBtn = document.getElementById('pay-btn');
    payBtn.disabled = true;
    document.getElementById('pay-btn-label').innerText = '결제 요청 중...';

    try {
        await widgets.requestPayment({
            orderId: orderId_toss,
            orderName: orderName,
            successUrl: window.location.origin + `/kiosk/toss/success?pointUsed=${appliedPoint}`,
            failUrl: window.location.origin + `/kiosk/toss/fail?source=${encodeURIComponent(checkoutSource || 'kiosk')}`,
        });
    } catch (err) {
        payBtn.disabled = false;
        document.getElementById('pay-btn-label').innerText = '결제하기';
        if (err.code !== 'USER_CANCEL') {
            showToast(err.message || '결제 요청 중 오류가 발생했습니다.');
        }
    }
}

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
                if (res.redirected && res.url) {
                    window.location.href = res.url;
                }
                return;
            }

            const data = await res.json();
            if (data && data.success && data.status === 'CLEANING') {
                window.location.href = '/kiosk/cleaning_wait';
            }
        } catch (err) {
            console.error('테이블 상태 확인 실패:', err);
        }
    }

    checkTableStatus();
    setInterval(checkTableStatus, 3000);
}

// ===== 페이지 로드 시 초기화 =====
document.addEventListener('DOMContentLoaded', async function () {
    startTableStatusWatcher();

    // 주문 데이터 + 포인트 잔액 동시 로드
    await Promise.all([
        loadCheckout(),
        fetchPointBalance()
    ]);

    // 이용 시간 표시
    updateElapsedTime();

    // 토스 위젯 초기화
    await initializeTossWidget();
});