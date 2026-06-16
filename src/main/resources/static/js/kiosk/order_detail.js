let currentStatus = orderData.status || 'PENDING';

document.addEventListener('DOMContentLoaded', function() {
    renderOrderItems();
    renderOrderTime();
    startTableStatusWatcher();
    updateStatusDisplay();
    updateCancelButtonVisibility();
    initAdminControls();
    pollOrderStatus();
});

function renderOrderItems() {
    const itemsContainer = document.getElementById('order-items');
    const items = Array.isArray(orderData.items)
        ? orderData.items
        : (Array.isArray(orderData.orderItems) ? orderData.orderItems : []);

    itemsContainer.innerHTML = items.map((item, index) => `
      <div class="order-item">
        <div class="item-info">
          <div class="item-name">${item.menuName || item.name || '상품명 없음'}</div>
          <div class="item-qty">수량: ${item.quantity}개 · <span class="item-status" data-index="${index}">주문 완료</span></div>
        </div>
        <div class="item-price">₩${((Number(item.price ?? item.menuPrice ?? 0)) * Number(item.quantity || 0)).toLocaleString()}</div>
      </div>
    `).join('');
}

function renderOrderTime() {
    const orderedAt = orderData.orderedAt;
    if (!orderedAt) {
        document.getElementById('order-time').innerText = '방금 전';
        return;
    }

    const date = new Date(orderedAt);
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    document.getElementById('order-time').innerText = `${hours}:${minutes}`;
}

function updateStatusDisplay() {
    const status = currentStatus;
    const statusIcon = document.getElementById('status-icon');
    const statusText = document.getElementById('status-text');
    const statusSubtext = document.getElementById('status-subtext');
    const progressFill = document.getElementById('progress-fill');
    const statusChip = document.getElementById('status-chip');
    const progressLabels = document.getElementById('progress-labels');

    const statusConfig = {
        'ORDERED': { icon: 'clock', text: '주문 완료! 관리자 확인 중...', short: '주문 완료', progress: 20 },
        'CONFIRMED': { icon: 'check-circle', text: '주문 확인! 조리 시작...', short: '주문 확인', progress: 40 },
        'COOKING': { icon: 'flame', text: '조리 중입니다...', short: '조리 중', progress: 65 },
        'DELIVERING': { icon: 'package-check', text: '배달 준비 중...', short: '서빙 준비', progress: 85 },
        'COMPLETED': { icon: 'check-circle-2', text: '완료!', short: '완료', progress: 100 },
        'CANCELLED': { icon: 'x-circle', text: '주문 취소됨', short: '취소', progress: 0 }
    };

    const config = statusConfig[status] || statusConfig['ORDERED'];

    statusIcon.innerHTML = `<i data-lucide="${config.icon}"></i>`;
    statusText.innerText = config.text;
    if (statusSubtext) statusSubtext.innerText = '매장 준비 상황에 따라 변경될 수 있어요';
    statusChip.innerText = config.short;
    progressFill.style.width = config.progress + '%';
    if (progressLabels) progressLabels.innerHTML = '<span>주문</span><span>확인</span><span>조리</span><span>서빙</span><span>완료</span>';
    updateCancelButtonVisibility();

    lucide.createIcons();
}

function pollOrderStatus() {
    const pollInterval = setInterval(() => {
        fetch(`/kiosk/order/api/${orderId}`)
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    const newStatus = data.status;
                    if (newStatus !== currentStatus) {
                        currentStatus = newStatus;
                        updateStatusDisplay();
                        showToast(`✓ 상태 변경: ${getStatusText(newStatus)}`);

                        if (newStatus === 'COMPLETED' || newStatus === 'CANCELLED') {
                            clearInterval(pollInterval);
                        }
                    }
                }
            })
            .catch(error => {
                console.error('상태 조회 실패:', error);
            });
    }, 2000);

    window.addEventListener('beforeunload', () => {
        clearInterval(pollInterval);
    });
}

function startTableStatusWatcher() {
    async function checkTableStatus() {
        try {
            const res = await fetch('/kiosk/table/status', {
                headers: { 'Accept': 'application/json' },
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
            if (data?.success && data.status === 'CLEANING') {
                window.location.href = '/kiosk/cleaning_wait';
            }
        } catch (error) {
            console.error('테이블 상태 확인 실패:', error);
        }
    }

    checkTableStatus();
    setInterval(checkTableStatus, 3000);
}

function getStatusText(status) {
    const texts = {
        'ORDERED': '주문 완료',
        'CONFIRMED': '주문 확인',
        'COOKING': '조리 중',
        'DELIVERING': '배달 준비',
        'COMPLETED': '완료',
        'CANCELLED': '취소'
    };
    return texts[status] || '상태 확인';
}

function goToMenu() {
    window.location.href = `/kiosk/drinks?tableNumber=${tableNumber}`;
}

function goToCart() {
    window.location.href = '/kiosk/cart';
}

function canCancelOrder(status) {
    return status === 'ORDERED' || status === 'CONFIRMED';
}

function updateCancelButtonVisibility() {
    const cancelBtn = document.getElementById('cancel-order-btn');
    if (!cancelBtn) return;
    cancelBtn.style.display = canCancelOrder(currentStatus) ? 'inline-flex' : 'none';
}

async function cancelOrder() {
    if (!canCancelOrder(currentStatus)) {
        showToast('조리 시작 이후에는 취소할 수 없습니다.');
        return;
    }

    if (!confirm('주문을 취소하시겠습니까?')) {
        return;
    }

    try {
        const response = await fetch(`/kiosk/order/${orderId}`, { method: 'DELETE' });
        const data = await response.json().catch(() => ({}));

        if (!response.ok || data.success === false) {
            throw new Error(data.message || '주문 취소 실패');
        }

        currentStatus = 'CANCELLED';
        updateStatusDisplay();
        showToast('주문이 취소되었습니다.');
    } catch (error) {
        showToast(`주문 취소 실패: ${error.message}`);
    }
}

function showToast(message) {
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.innerText = message;
    document.body.appendChild(toast);

    setTimeout(() => toast.remove(), 2500);
}

function initAdminControls() {
    const isAdmin = new URLSearchParams(window.location.search).get('admin') === 'true';
    const adminControlsDiv = document.getElementById('admin-controls');

    if (!isAdmin) {
        return;
    }

    adminControlsDiv.style.display = 'block';

    const statuses = [
        { key: 'ORDERED', label: '주문 완료' },
        { key: 'CONFIRMED', label: '주문 확인' },
        { key: 'COOKING', label: '조리 중' },
        { key: 'DELIVERING', label: '배달 준비' },
        { key: 'COMPLETED', label: '완료' }
    ];

    const controlsContainer = document.getElementById('order-status-controls');
    controlsContainer.innerHTML = statuses.map(status => {
        const isActive = status.key === currentStatus;
        const buttons = statuses
            .filter(s => s.key !== status.key)
            .map(s => `
          <button class="status-btn status-btn-${s.key.toLowerCase()}" onclick="changeOrderStatus('${s.key}')">
            → ${s.label}
          </button>
        `).join('');

        return `
        <div class="status-control-item">
          <div class="status-control-label">
            <div class="status-control-name">${status.label}</div>
            <div class="status-control-current">${isActive ? '● 현재 상태' : ''}</div>
          </div>
          <div class="status-control-buttons">${buttons}</div>
        </div>
      `;
    }).join('');
}

async function changeOrderStatus(newStatus) {
    try {
        const response = await fetch(`/kiosk/order/${orderId}/status`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: newStatus })
        });

        const data = await response.json();

        if (data.success) {
            currentStatus = newStatus;
            updateStatusDisplay();
            initAdminControls();
            showToast(`✓ 상태 변경: ${getStatusText(newStatus)}`);
        } else {
            showToast(`✗ 상태 변경 실패: ${data.message}`);
        }
    } catch (error) {
        console.error('상태 변경 오류:', error);
        showToast('상태 변경 중 오류가 발생했습니다.');
    }
}