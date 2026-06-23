/* Lucide 아이콘 초기화 */
lucide.createIcons({
    attrs: {'stroke-width': 2.5}
});

/* 장바구니 화면 상태 */
const tableNumber = /*[[${tableNumber}]]*/ 1;
let remainingTime = 300;
const timerDisplay = document.getElementById('timer-display');
const warningModal = document.getElementById('warning-modal');
let timerInterval;

let currentCartItems = [];
let currentTotalPrice = 0;

/* 장바구니 화면 초기화 */
document.addEventListener('DOMContentLoaded', function () {
    loadCart();
    loadActiveOrders();
    startTableStatusWatcher();
    startTimer();
});

/* 테이블 청소 상태 감시 시작 */
function startTableStatusWatcher() {
    /* 테이블 상태 조회 및 청소 대기 화면 이동 */
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

/* 장바구니 데이터 조회 */
function loadCart() {
    fetch('/kiosk/cart/items')
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                currentCartItems = data.cartItems;
                currentTotalPrice = data.totalPrice;
                renderCart(data.cartItems, data.totalPrice);
            }
        })
        .catch(error => {
            console.error('Error loading cart:', error);
            showToast('장바구니를 불러올 수 없습니다.');
        });
}

/* 장바구니 목록 렌더링 */
function renderCart(cartItems, totalPrice) {
    const cartContent = document.getElementById('cart-content');

    if (cartItems.length === 0) {
        cartContent.innerHTML = `
        <div class="empty-cart">
          <i data-lucide="shopping-cart"></i>
          <h2>장바구니가 비어있습니다</h2>
          <p>메뉴에서 상품을 추가해주세요.</p>
        </div>
      `;
        document.getElementById('checkout-btn').disabled = true;
        lucide.createIcons();
        return;
    }

    document.getElementById('checkout-btn').disabled = false;

    cartContent.innerHTML = cartItems.map(item => `
      <div class="cart-item" data-name="${item.menuName}" data-price="${item.menuPrice}">
        <div class="item-info">
          <h3>${item.menuName}</h3>
          <div class="item-price">${item.menuPrice === 0 ? 'FREE' : '₩' + item.menuPrice.toLocaleString()}</div>
          <div class="item-subtotal">수량: ${item.quantity}개 → ${item.menuPrice === 0 ? 'FREE' : '₩' + (item.menuPrice * item.quantity).toLocaleString()}</div>
        </div>
        <div class="quantity-control">
          <button class="qty-btn minus" onclick="updateQuantity('${item.menuName}', ${item.menuPrice}, -1)">
            <i data-lucide="minus"></i>
          </button>
          <span class="qty-number">${item.quantity}</span>
          <button class="qty-btn plus" onclick="updateQuantity('${item.menuName}', ${item.menuPrice}, 1)">
            <i data-lucide="plus"></i>
          </button>
        </div>
        <button class="delete-btn" onclick="deleteItem('${item.menuName}', ${item.menuPrice})" title="삭제">
          <i data-lucide="trash-2"></i>
        </button>
      </div>
    `).join('');

    document.getElementById('total-amount').innerText = `₩${totalPrice.toLocaleString()}`;
    lucide.createIcons();
}

/* 장바구니 상품 수량 변경 */
function updateQuantity(menuName, menuPrice, delta) {
    const cartItem = document.querySelector(
        `.cart-item[data-name="${menuName}"][data-price="${menuPrice}"]`
    );
    const currentQty = parseInt(cartItem.querySelector('.qty-number').innerText);
    const newQty = currentQty + delta;

    fetch('/kiosk/cart/update', {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({menuName: menuName, menuPrice: menuPrice, quantity: newQty})
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                loadCart();
                showToast('수량이 업데이트되었습니다.');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('수량 업데이트에 실패했습니다.');
        });
}

/* 장바구니 상품 삭제 */
function deleteItem(menuName, menuPrice) {
    if (!confirm(`${menuName}을(를) 삭제하시겠습니까?`)) return;

    fetch('/kiosk/cart/update', {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({menuName: menuName, menuPrice: menuPrice, quantity: 0})
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                loadCart();
                showToast('상품이 삭제되었습니다.');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('삭제에 실패했습니다.');
        });
}

/* 장바구니 전체 비우기 */
function clearCart() {
    if (!confirm('장바구니를 비우시겠습니까?')) return;

    fetch('/kiosk/cart/clear', {method: 'DELETE'})
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                loadCart();
                showToast('장바구니가 비워졌습니다.');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('비우기에 실패했습니다.');
        });
}

/* 진행 중인 일반 주문 목록 조회 */
function loadActiveOrders() {
    fetch('/kiosk/order/active')
        .then(async response => {
            if (!response.ok) return [];
            const contentType = response.headers.get('content-type') || '';
            if (!contentType.includes('application/json')) {
                if (response.redirected && response.url) {
                    window.location.href = response.url;
                }
                return [];
            }
            return response.json();
        })
        .then(orders => {
            const section = document.getElementById('active-orders-section');

            /* 게임 대여 주문 여부 확인 */
            const isGameOrder = (order) => {
                const items = Array.isArray(order?.items) ? order.items : [];
                return items.length > 0
                    && items.every(item => Number(item?.price ?? item?.menuPrice ?? 0) === 0);
            };

            const visibleOrders = Array.isArray(orders)
                ? orders.filter(order =>
                    order
                    && !isGameOrder(order))
                : [];

            if (visibleOrders.length === 0) {
                section.style.display = 'none';
                return;
            }

            const statusText = {
                'ORDERED': '주문 완료',
                'CONFIRMED': '주문 확인중...',
                'COOKING': '조리 중...',
                'DELIVERING': '배달 시작',
                'COMPLETED': '완료',
                'CANCELLED': '취소'
            };

            const statusSteps = {
                'ORDERED': 0,
                'CONFIRMED': 1,
                'COOKING': 2,
                'DELIVERING': 3,
                'COMPLETED': 4,
                'CANCELLED': 4
            };

            const steps = ['주문', '주문확인', '조리시작', '서빙시작', '완료'];

            const cards = visibleOrders.map(order => {
                const currentStep = statusSteps[order.status] || 0;
                const progressWidth = (currentStep / 4) * 100;
                const itemsList = order.items && order.items.length > 0
                    ? order.items.map(item => `${item.menuName} x${item.quantity}`).join(', ')
                    : '';

                const detailPath = isGameOrder(order)
                    ? `/kiosk/order/game/${order.id}`
                    : `/kiosk/order/${order.id}`;

                return `
                        <div class="order-status-card" onclick="window.location.href='${detailPath}'">
                            <div class="card-header">
                                <span class="order-number">주문 #${order.id}</span>
                                <span class="status-badge status-${order.status.toLowerCase()}">${statusText[order.status] || order.status}</span>
                            </div>

                            <div class="progress-container">
                                <div class="progress-steps">
                                    <div class="progress-bar-background"></div>
                                    <div class="progress-bar-fill" style="width: ${progressWidth}%"></div>
                                    ${steps.map((label, idx) => `
                                        <div class="progress-step">
                                            <div class="step-dot ${idx <= currentStep ? (idx === currentStep ? 'active' : 'completed') : ''}">
                                                ${idx < currentStep ? '✓' : idx + 1}
                                            </div>
                                            <div class="step-label ${idx <= currentStep ? (idx === currentStep ? 'active' : 'completed') : ''}">${label}</div>
                                        </div>
                                    `).join('')}
                                </div>
                            </div>

                            ${itemsList ? `
                                <div class="card-items">
                                    <ul class="card-items-list">
                                        ${order.items.map(item => `
                                            <li>• ${item.menuName} x${item.quantity}</li>
                                        `).join('')}
                                    </ul>
                                </div>
                            ` : ''}
                        </div>
                    `;
            }).join('');

            section.innerHTML = `
                    <div class="active-orders-label">
                        <i data-lucide="package"></i>
                        진행 중인 주문
                    </div>
                    <div class="orders-grid">
                        ${cards}
                    </div>
                `;
            section.style.display = 'flex';
            lucide.createIcons();
        })
        .catch(err => console.error('주문 조회 실패:', err));
}

/* 진행 중인 주문 상태 주기 갱신 */
setInterval(() => {
    loadActiveOrders();
}, 5000);

/* 주문 확인 모달 열기 */
function openOrderConfirmModal() {
    if (currentCartItems.length === 0) {
        showToast('장바구니가 비어있습니다.');
        return;
    }

    // 모달에 주문 상품 목록 표시
    const modalItemsList = document.getElementById('modal-items-list');
    modalItemsList.innerHTML = currentCartItems.map(item => `
      <div class="modal-item">
        <div class="modal-item-info">
          <div class="modal-item-name">${item.menuName}</div>
          <div class="modal-item-qty">수량: ${item.quantity}개</div>
        </div>
        <div class="modal-item-price">
          ${item.menuPrice === 0 ? 'FREE' : '₩' + (item.menuPrice * item.quantity).toLocaleString()}
        </div>
      </div>
    `).join('');

    document.getElementById('modal-total').innerText = `₩${currentTotalPrice.toLocaleString()}`;

    document.getElementById('order-confirm-modal').style.display = 'flex';
    lucide.createIcons();
}

/* 주문 확인 모달 닫기 */
function closeOrderConfirmModal() {
    document.getElementById('order-confirm-modal').style.display = 'none';
}

/* 장바구니 주문 생성 */
async function confirmOrder() {
    const confirmBtn = document.getElementById('confirm-btn');
    const confirmBtnText = document.getElementById('confirm-btn-text');

    confirmBtn.disabled = true;
    confirmBtnText.innerHTML = '<span class="spinner"></span>주문 처리 중...';

    try {
        const response = await fetch('/kiosk/order/create', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                tableNumber: tableNumber,
                totalAmount: currentTotalPrice,
                customerPhone: null
            })
        });

        const data = await response.json();

        if (data.success) {
            // console.log('주문 생성 성공 - orderId:', data.id);
            showToast(`✓ 주문이 생성되었습니다. (주문번호: ${data.id})`);

            const createdItems = Array.isArray(data.items) ? data.items : [];
            const gameOnlyOrder = createdItems.length > 0
                && createdItems.every(item => Number(item?.price ?? item?.menuPrice ?? 0) === 0);

            // 주문 생성 안내 후 상세 페이지로 이동
            setTimeout(() => {
                window.location.href = gameOnlyOrder
                    ? `/kiosk/order/game/${data.id}`
                    : `/kiosk/order/${data.id}`;
            }, 1500);
        } else {
            showToast(`✗ 주문 생성 실패: ${data.message}`);
            confirmBtn.disabled = false;
            confirmBtnText.innerText = '주문하기';
        }
    } catch (error) {
        console.error('주문 생성 중 오류:', error);
        showToast('주문 처리 중 오류가 발생했습니다.');
        confirmBtn.disabled = false;
        confirmBtnText.innerText = '주문하기';
    }
}

/* 장바구니 화면 타이머 시작 */
function startTimer() {
    timerInterval = setInterval(updateTimer, 1000);
}

/* 장바구니 화면 남은 시간 갱신 */
function updateTimer() {
    remainingTime--;
    const m = Math.floor(remainingTime / 60);
    const s = remainingTime % 60;
    timerDisplay.innerText = `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;

    if (remainingTime === 30) {
        warningModal.style.display = 'flex';
    }
    if (remainingTime <= 0) {
        clearInterval(timerInterval);
        window.location.href = '/kiosk/screensaver';
    }
}

/* 사용자 활동 타이머 초기화 */
function resetActivityTimer() {
    remainingTime = 300;
    if (warningModal.style.display === 'flex') {
        hideWarningModal();
    }
}

/* 시간 만료 경고 모달 숨김 */
function hideWarningModal() {
    warningModal.style.display = 'none';
}

/* 토스트 메시지 표시 */
function showToast(message) {
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.innerText = message;
    document.body.appendChild(toast);

    setTimeout(() => toast.remove(), 2500);
}
