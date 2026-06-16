let currentTableId = null;
let currentTableStatus = null;
let serialAssignContext = { orderId: null, gameName: '', quantity: 1 };
let rentalReturnContext = { orderId: null, gameName: '', quantity: 1 };
let pendingOrdersState = [];
let latestModalOrderItems = [];
const checkoutMetaCache = new Map();

function parsePendingOrderId(order) {
    const id = Number(order?.id ?? order?.orderId ?? 0);
    return Number.isFinite(id) && id > 0 ? id : 0;
}

function isGameOnlyPendingOrder(order) {
    const items = Array.isArray(order?.items) ? order.items
        : (Array.isArray(order?.orderItems) ? order.orderItems : []);
    if (items.length === 0) return false;
    return items.every(item => Number(item?.price ?? item?.menuPrice ?? 0) === 0);
}

function hideMessageBox() {
    const box = document.getElementById('unreadMsgBox');
    const btn = document.getElementById('readBtn');
    if (box) box.style.display = 'none';
    if (btn) btn.style.display = 'none';
}

function syncCardLiveAmount(tableId, amount) {
    if (!tableId) return;
    const card = document.querySelector(`.table-card[data-id="${tableId}"]`);
    if (!card) return;
    const liveNode = card.querySelector('[data-role="live-amount"]');
    if (liveNode) {
        liveNode.textContent = `₩${Number(amount || 0).toLocaleString()}`;
    }
}

async function renderModalLiveTotal(menuAmount) {
    const priceSpan = document.getElementById('totalPrice');
    if (!priceSpan) return;

    const baseMenuAmount = Number(menuAmount || 0);
    let packageAmount = 0;
    let overCharge = 0;

    if (currentTableId && currentTableStatus === 'OCCUPIED') {
        try {
            const meta = await fetchCheckoutMeta(currentTableId);
            packageAmount = Number(meta?.packageTotal || 0);
            overCharge = calcOverCharge(meta);
        } catch (e) {
            console.warn('모달 정산 메타 조회 실패(주문합계만 표시):', e);
        }
    }

    const total = baseMenuAmount + packageAmount + overCharge;
    priceSpan.innerText = `${total.toLocaleString()}원`;
    syncCardLiveAmount(currentTableId, total);
}

function renderOrders(orders, activeRentals = []) {
    const listDiv = document.getElementById('orderList');
    const priceSpan = document.getElementById('totalPrice');
    let sum = 0;
    let latestStatus = 'ORDERED';

    if (!listDiv || !priceSpan) return;

    let orderItems = orders;
    if (!Array.isArray(orders)) {
        if (orders?.items) { orderItems = orders.items; }
        else if (orders?.orderItems) { orderItems = orders.orderItems; }
        else { orderItems = []; }
    }

    // WebSocket에서 OrdersDTO[] 형태(주문 헤더 + items[])로 오는 경우를
    // 모달 표시용 OrderItem[] 형태로 평탄화한다.
    if (Array.isArray(orderItems) && orderItems.length > 0) {
        const sample = orderItems[0];
        const hasNestedItems = sample?.items || sample?.orderItems;
        const isFlatItem = sample?.menuName || sample?.menu_name;

        if (hasNestedItems && !isFlatItem) {
            const flattened = [];
            for (const order of orderItems) {
                const orderId = order.id || order.orderId;
                const orderStatus = order.status || 'ORDERED';
                const children = order.items || order.orderItems || [];

                if (!children.length) continue;
                for (const child of children) {
                    flattened.push({
                        id: child.id || orderId,
                        orderId: orderId,
                        menuId: child.menuId || child.menu_id,
                        menu_name: child.menu_name || child.menuName,
                        menuName: child.menuName || child.menu_name,
                        quantity: child.quantity || child.qty || 0,
                        price: child.price || 0,
                        status: child.status || orderStatus
                    });
                }
            }
            orderItems = flattened;
        }
    }

    if (!orderItems || orderItems.length === 0) {
        latestModalOrderItems = [];
        listDiv.innerHTML = '<div style="color:#999; display:flex; justify-content:center; padding: 20px; border:none;">진행 중인 주문이 없습니다.</div>';
        renderModalLiveTotal(0);
        return;
    }
    latestModalOrderItems = orderItems.slice();

    const activeRentalCountByGame = new Map();
    if (Array.isArray(activeRentals)) {
        activeRentals.forEach(rental => {
            const gameName = String(rental?.gameName || '').trim();
            if (!gameName) return;
            activeRentalCountByGame.set(
                gameName,
                (activeRentalCountByGame.get(gameName) || 0) + 1
            );
        });
    }

    const latest = computeLatestOrderStatus(orderItems);
    latestStatus = latest ? latest.status : null;

    const groupedOrders = new Map();
    for (const item of orderItems) {
        const orderId = Number(item?.orderId || item?.id || 0);
        if (!orderId) continue;

        if (!groupedOrders.has(orderId)) {
            groupedOrders.set(orderId, {
                orderId,
                status: item?.status || 'ORDERED',
                items: []
            });
        }
        groupedOrders.get(orderId).items.push(item);
    }

    listDiv.innerHTML = Array.from(groupedOrders.values()).map(group => {
        const orderId = group.orderId;
        const currentStatus = group.status || 'ORDERED';
        const items = Array.isArray(group.items) ? group.items : [];

        const isGameRequest = items.length > 0
            && items.every(v => Number(v?.price ?? v?.menuPrice ?? 0) === 0);

        const orderTotal = items.reduce((acc, v) => {
            const price = Number(v?.price ?? v?.menuPrice ?? 0) || 0;
            const qty = Number(v?.quantity || 0) || 0;
            return acc + (price * qty);
        }, 0);

        if (currentStatus !== 'CANCELLED') {
            sum += orderTotal;
        }

        let statusText = '';
        let badgeBg = '#f1f3f4';
        let badgeColor = '#5f6368';
        let actionHtml = '';

        const btnBase = "padding: 6px 12px; border-radius: 6px; border: none; font-size: 11px; cursor: pointer; font-weight: 600; transition: all 0.2s;";
        const btnNext = btnBase + " background: #4285f4; color: white;";
        const btnSerial = btnBase + " background: #0d6efd; color: white;";
        const btnReturn = btnBase + " background: #0ea5e9; color: white;";
        const btnCancel = btnBase + " background: #ef4444; color: white;";

        const gameQtyByName = new Map();
        items.forEach(v => {
            const n = String(v?.menu_name || v?.menuName || '').trim();
            if (!n) return;
            gameQtyByName.set(n, (gameQtyByName.get(n) || 0) + (Number(v?.quantity || 0) || 0));
        });

        if (isGameRequest) {
            if (currentStatus === 'ORDERED') {
                statusText = '게임 요청'; badgeBg = '#fff3cd'; badgeColor = '#856404';
                actionHtml = Array.from(gameQtyByName.entries()).map(([name, qty]) => `
                                <button style="${btnSerial}" onclick='openGameSerialModal(${orderId}, ${JSON.stringify(name)}, ${qty})'>${name} 일련번호 선택</button>
                            `).join('');
            } else if (currentStatus === 'CONFIRMED') {
                const hasAnyActiveRental = Array.from(gameQtyByName.keys()).some(name => (activeRentalCountByGame.get(name) || 0) > 0);
                if (!hasAnyActiveRental) return '';
                statusText = '대여 중'; badgeBg = '#d1ecf1'; badgeColor = '#0c5460';
                actionHtml = Array.from(gameQtyByName.entries()).map(([name, qty]) => {
                    const activeCount = activeRentalCountByGame.get(name) || 0;
                    if (activeCount <= 0) return '';
                    return `<button style="${btnReturn}" onclick='openRentalReturnModal(${orderId}, ${JSON.stringify(name)}, ${qty})'>${name} 반납</button>`;
                }).join('');
            } else {
                statusText = '대여 완료'; badgeBg = '#d1ecf1'; badgeColor = '#0c5460';
            }
        } else {
            switch(currentStatus) {
                case 'ORDERED':
                    statusText = '주문 완료'; badgeBg = '#fff3cd'; badgeColor = '#856404';
                    actionHtml = `
                                    <button style="${btnNext}" onclick="updateOrderStatus(${orderId}, 'CONFIRMED')">확인</button>
                                    <button style="${btnCancel}" onclick="updateOrderStatus(${orderId}, 'CANCELLED')">취소</button>`;
                    break;
                case 'CONFIRMED':
                    statusText = '주문 확인'; badgeBg = '#d1ecf1'; badgeColor = '#0c5460';
                    actionHtml = `
                                    <button style="${btnNext}" onclick="updateOrderStatus(${orderId}, 'COOKING')">조리</button>
                                    <button style="${btnCancel}" onclick="updateOrderStatus(${orderId}, 'CANCELLED')">취소</button>`;
                    break;
                case 'COOKING':
                    statusText = '조리 중'; badgeBg = '#fde8e8'; badgeColor = '#721c24';
                    actionHtml = `
                                    <button style="${btnNext}" onclick="updateOrderStatus(${orderId}, 'DELIVERING')">배달</button>`;
                    break;
                case 'DELIVERING':
                    statusText = '배달 준비'; badgeBg = '#d4edda'; badgeColor = '#155724';
                    actionHtml = `<button style="${btnNext}" onclick="updateOrderStatus(${orderId}, 'COMPLETED')">완료</button>`;
                    break;
                case 'COMPLETED':
                    statusText = '완료'; badgeBg = '#c8e6c9'; badgeColor = '#1b5e20';
                    break;
                case 'CANCELLED':
                    statusText = '취소됨'; badgeBg = '#ffcdd2'; badgeColor = '#c62828';
                    break;
                default: statusText = currentStatus;
            }
        }

        const isCancelled = currentStatus === 'CANCELLED';
        const textStyle = isCancelled ? 'text-decoration: line-through; color: #999;' : '';
        const itemRows = items.map(v => {
            const menuName = v?.menu_name || v?.menuName || '상품명 없음';
            const quantity = Number(v?.quantity || 0) || 0;
            const price = Number(v?.price ?? v?.menuPrice ?? 0) || 0;
            const itemTotal = price * quantity;
            return `
                            <div class="order-item-header">
                                <span class="order-item-name" style="${textStyle}">${menuName} x ${quantity}</span>
                                <span class="order-item-price" style="${textStyle}">${itemTotal.toLocaleString()}원</span>
                            </div>
                        `;
        }).join('');

        return `
                    <div class="order-item-wrapper" data-order-id="${orderId}" data-status="${currentStatus}" data-is-game="${isGameRequest ? 'true' : 'false'}">
                        ${itemRows}
                        <div class="order-item-actions">
                            <span class="order-status-label" style="background: ${badgeBg}; color: ${badgeColor}; ${isCancelled ? 'text-decoration: line-through;' : ''}">${statusText}</span>
                            <div class="order-action-buttons">${actionHtml}</div>
                        </div>
                    </div>`;
    }).join('');

    if (!listDiv.innerHTML.trim()) {
        listDiv.innerHTML = '<div style="color:#999; display:flex; justify-content:center; padding: 20px; border:none;">진행 중인 주문이 없습니다.</div>';
        renderModalLiveTotal(0);
        updateTableCardOrderStatus(null);
        updateOrderStatusButton();
        fetchRentedGameItems();
        return;
    }

    renderModalLiveTotal(sum);
    updateTableCardOrderStatus(latest);
    updateOrderStatusButton();
    fetchRentedGameItems();
}

function updateTableCardOrderStatus(latest) {
    const selectedCard = document.querySelector(`.table-card[data-id="${currentTableId}"]`);
    if (!selectedCard) return;

    const container = selectedCard.querySelector('.order-status-container');
    if (!container) return;
    const miniBadge = container.querySelector('[data-role="order-mini-badge"]');
    const miniText = miniBadge ? miniBadge.querySelector('.mini-text') : null;
    if (!miniBadge || !miniText) return;

    if (!latest || !latest.status || latest.status === 'CANCELLED') {
        miniBadge.classList.remove('visible');
        return;
    }
    miniText.textContent = getMiniBadgeLabel(latest.status, latest.gameFlow);
    miniBadge.classList.add('visible');
}

async function changeAllOrdersStatus() {
    const orders = document.querySelectorAll('.order-item-wrapper');
    const targets = Array.from(orders)
        .filter(orderItem => orderItem.getAttribute('data-is-game') !== 'true')
        .map(orderItem => {
            const orderId = Number(orderItem.getAttribute('data-order-id'));
            const currentStatus = orderItem.getAttribute('data-status') || '';
            const nextStatus = getNextOrderStatus(currentStatus);
            return { orderId, currentStatus, nextStatus };
        })
        .filter(t => t.orderId > 0 && t.nextStatus);

    if (targets.length === 0) {
        alert('변경 가능한 주문이 없습니다.');
        return;
    }

    const target = targets[0];
    if (!confirm(`주문 1건(ID: ${target.orderId})을 다음 단계로 변경할까요?`)) {
        return;
    }

    const ok = await updateOrderStatusSilent(target.orderId, target.nextStatus);

    await fetchActiveOrders();
    await refreshTableCardOrderBadges();

    if (ok) alert(`주문 1건 상태 변경 완료 (ID: ${target.orderId})`);
    else alert(`주문 상태 변경 실패 (ID: ${target.orderId})`);
}

function getNextOrderStatus(currentStatus) {
    if (currentStatus === 'ORDERED') return 'CONFIRMED';
    if (currentStatus === 'CONFIRMED') return 'COOKING';
    if (currentStatus === 'COOKING') return 'DELIVERING';
    if (currentStatus === 'DELIVERING') return 'COMPLETED';
    return null;
}

async function updateOrderStatusSilent(orderId, nextStatus) {
    try {
        const headers = getJsonHeaders();

        const response = await fetch(`/admin/api/dashboard/orders/${orderId}/status`, {
            method: 'PATCH',
            headers,
            body: JSON.stringify({ status: nextStatus })
        });
        return response.ok;
    } catch (e) {
        console.error('일괄 상태 변경 실패:', { orderId, nextStatus, error: e });
        return false;
    }
}

async function updateOrderStatus(orderId, nextStatus) {
    const id = Number(orderId || 0);
    if (id <= 0) {
        alert('주문 번호를 확인할 수 없습니다.');
        return;
    }

    let confirmMsg = '주문 상태를 변경하시겠습니까?';
    if (nextStatus === 'CONFIRMED') confirmMsg = '주문을 확인 처리할까요?';
    else if (nextStatus === 'COOKING') confirmMsg = '조리 시작 처리할까요?';
    else if (nextStatus === 'DELIVERING') confirmMsg = '배달 시작 처리할까요?';
    else if (nextStatus === 'COMPLETED') confirmMsg = '완료 처리할까요?';
    else if (nextStatus === 'CANCELLED') confirmMsg = '해당 주문을 취소 처리할까요?';

    if (!confirm(confirmMsg)) return;

    const ok = await updateOrderStatusSilent(id, nextStatus);
    await fetchActiveOrders();
    await refreshTableCardOrderBadges();

    if (!ok) {
        alert('주문 상태 변경에 실패했습니다.');
    }
}

function updateOrderStatusButton() {
    const btn = document.getElementById('orderStatusBtn');
    const orders = document.querySelectorAll('.order-item-wrapper');
    let hasActiveOrder = false;

    for (let order of orders) {
        if (order.getAttribute('data-is-game') === 'true') continue;
        const statusLabel = order.querySelector('.order-status-label');
        if (statusLabel) {
            const status = statusLabel.textContent.trim();
            if (status !== '완료' && status !== '취소됨') {
                hasActiveOrder = true;
                break;
            }
        }
    }
    if (btn) btn.style.display = hasActiveOrder ? 'inline-flex' : 'none';
}

async function openTableModal(element) {
    document.querySelectorAll('.table-card').forEach(c => c.classList.remove('selected'));
    element.classList.add('selected');
    currentTableId = element.getAttribute('data-id');
    currentTableStatus = element.getAttribute('data-status');
    const tableNum = element.getAttribute('data-num');
    const hasMsg = element.classList.contains('has-msg');

    const modalTitle = document.getElementById('modalTitle');
    if (modalTitle) modalTitle.innerText = 'Table ' + tableNum;
    hideMessageBox();

    const modal = document.getElementById('tableModal');
    if (modal) {
        modal.style.display = 'flex';
        setTimeout(() => {
            const content = modal.querySelector('.modal-content');
            if (content) content.style.transform = 'translateY(0)';
        }, 10);
    }

    if (currentTableStatus === 'OCCUPIED') {
        // 모달 오픈 즉시 REST로 1회 로드해서 첫 화면 공백을 방지하고,
        // 이후 WebSocket 업데이트를 계속 수신한다.
        await fetchActiveOrders();
        await fetchRentedGameItems();
        subscribeToTableOrders(parseInt(currentTableId));
    } else {
        renderOrders([]);
    }

    if (hasMsg) { await fetchUnreadMessages(); }
}

function closeTableModal() {
    const modal = document.getElementById('tableModal');
    if (modal) {
        modal.style.display = 'none';
        const content = modal.querySelector('.modal-content');
        if (content) content.style.transform = 'translateY(-20px)';
    }
    unsubscribeFromOrders();
    closeGameSerialModal();
    closePaymentGameCheckModal();
    closeRentalReturnModal();
    document.querySelectorAll('.table-card').forEach(c => c.classList.remove('selected'));
    currentTableId = null;
    currentTableStatus = null;
}

async function fetchUnreadMessages() {
    try {
        const response = await fetch(`/admin/dashboard/messages/${currentTableId}`);
        if (response.ok) {
            const messages = await response.json();
            const listDiv = document.getElementById('unreadMsgList');
            if (messages && messages.length > 0 && listDiv) {
                listDiv.innerHTML = messages.map(msg => `<li>${msg}</li>`).join('');
                document.getElementById('unreadMsgBox').style.display = 'block';
                document.getElementById('readBtn').style.display = 'block';
            }
        }
    } catch (err) { console.error(err); }
}

async function handleReadMessage() {
    if (!currentTableId) return;
    if (!confirm("요청 내용을 모두 확인하셨습니까?")) return;
    try {
        const response = await fetch(`/admin/dashboard/messages/${currentTableId}/read`, { method: 'PATCH' });
        if (response.ok) { location.reload(); }
    } catch (err) { console.error(err); }
}

async function fetchActiveOrders() {
    try {
        const [orderResponse, rentalResponse] = await Promise.all([
            fetch(`/admin/dashboard/${currentTableId}/orders`),
            fetch(`/admin/product/game-items/rentals/active?tableId=${currentTableId}`)
        ]);

        const orders = orderResponse.ok ? await orderResponse.json() : [];
        const activeRentals = rentalResponse.ok ? await rentalResponse.json() : [];
        renderOrders(orders, activeRentals);
    } catch (err) {
        console.error('❌ fetchActiveOrders 에러:', err);
        renderOrders([], []);
    }
}

function isGameOrderItem(item) {
    const price = Number(item?.price ?? item?.menuPrice ?? 0);
    return price === 0;
}

function computeLatestOrderStatus(orderItems) {
    if (!Array.isArray(orderItems) || orderItems.length === 0) return null;

    const validGameItems = orderItems.filter(item => isGameOrderItem(item) && item?.status !== 'CANCELLED');
    const hasNormalActive = orderItems.some(item => {
        const s = item?.status;
        if (!s || s === 'CANCELLED') return false;
        return !isGameOrderItem(item);
    });

    // 게임 주문만 남아있으면 ORDERED / 대여 완료(그 외) 2단계만 표시한다.
    if (validGameItems.length > 0 && !hasNormalActive) {
        const hasGameRequested = validGameItems.some(item => item?.status === 'ORDERED');
        return {
            status: hasGameRequested ? 'ORDERED' : 'CONFIRMED',
            gameFlow: true
        };
    }

    const rank = {
        ORDERED: 1,
        CONFIRMED: 2,
        COOKING: 3,
        DELIVERING: 4,
        COMPLETED: 5
    };

    let latest = null;
    let max = 0;
    for (const item of orderItems) {
        const s = item?.status;
        if (!s || s === 'CANCELLED') continue;
        if (isGameOrderItem(item)) continue;
        const r = rank[s] || 0;
        if (r >= max) {
            max = r;
            latest = s;
        }
    }
    if (!latest) return null;
    return {
        status: latest,
        gameFlow: false
    };
}

function getMiniBadgeLabel(status, gameFlow) {
    const normalLabels = {
        ORDERED: '주문 접수',
        CONFIRMED: '주문 확인',
        COOKING: '조리 중',
        DELIVERING: '배달 중',
        COMPLETED: '완료'
    };
    const gameLabels = {
        ORDERED: '게임 요청',
        CONFIRMED: '대여 완료',
        COOKING: '대여 완료',
        DELIVERING: '대여 완료',
        COMPLETED: '대여 완료',
        CANCELLED: '대여 완료'
    };
    const labels = gameFlow ? gameLabels : normalLabels;
    return labels[status] || (gameFlow ? '대여 완료' : '주문 진행중');
}

async function refreshTableCardOrderBadges() {
    const cards = Array.from(document.querySelectorAll('.table-card.status-occupied'));
    if (!cards.length) return;

    await Promise.all(cards.map(async (card) => {
        const tableId = card.getAttribute('data-id');
        const container = card.querySelector('.order-status-container');
        if (!tableId || !container) return;

        try {
            const res = await fetch(`/admin/dashboard/${tableId}/orders`, {
                headers: { 'Accept': 'application/json' },
                credentials: 'same-origin'
            });
            if (!res.ok) return;

            const contentType = res.headers.get('content-type') || '';
            if (!contentType.includes('application/json')) return;

            const orderItems = await res.json();
            const latest = computeLatestOrderStatus(orderItems);
            const miniBadge = container.querySelector('[data-role="order-mini-badge"]');
            const miniText = miniBadge ? miniBadge.querySelector('.mini-text') : null;

            if (!latest || latest.status === 'CANCELLED') {
                if (miniBadge) miniBadge.classList.remove('visible');
                return;
            }

            if (miniBadge && miniText) {
                miniText.textContent = getMiniBadgeLabel(latest.status, latest.gameFlow);
                miniBadge.classList.add('visible');
            }
        } catch (e) {
            console.error('테이블 카드 주문 상태 갱신 실패:', e);
        }
    }));
}

function toOrderItems(input) {
    if (!Array.isArray(input) || input.length === 0) return [];

    const sample = input[0];
    const hasNestedItems = sample?.items || sample?.orderItems;
    const isFlatItem = sample?.menuName || sample?.menu_name;
    if (!hasNestedItems || isFlatItem) {
        return input;
    }

    const flattened = [];
    for (const order of input) {
        const orderStatus = order?.status || 'ORDERED';
        const children = order?.items || order?.orderItems || [];
        for (const child of children) {
            flattened.push({
                status: child?.status || orderStatus,
                price: child?.price || child?.menuPrice || 0,
                quantity: child?.quantity || child?.qty || 0
            });
        }
    }
    return flattened;
}

function calcLiveAmount(orderItems) {
    const items = toOrderItems(orderItems);
    return items.reduce((sum, item) => {
        const status = String(item?.status || '').toUpperCase();
        if (status === 'CANCELLED') return sum;
        const price = Number(item?.price || 0) || 0;
        const qty = Number(item?.quantity || 0) || 0;
        return sum + (price * qty);
    }, 0);
}

async function fetchCheckoutMeta(tableId) {
    const cacheKey = String(tableId);
    const now = Date.now();
    const cached = checkoutMetaCache.get(cacheKey);
    if (cached && (now - cached.fetchedAt) < 30000) {
        return cached;
    }

    const res = await fetch(`/admin/dashboard/${tableId}/checkout-meta`, {
        headers: { 'Accept': 'application/json' },
        credentials: 'same-origin'
    });
    if (!res.ok) {
        throw new Error(`checkout meta fetch failed: ${res.status}`);
    }
    const data = await res.json();

    const meta = {
        packageTotal: Number(data?.packageTotal || 0),
        sessionStartTime: Number(data?.sessionStartTime || 0),
        durationMinutes: Number(data?.durationMinutes || 0),
        extraPricePerMin: Number(data?.extraPricePerMin || 0),
        partySize: Number(data?.partySize || 1) || 1,
        fetchedAt: now
    };
    checkoutMetaCache.set(cacheKey, meta);
    return meta;
}

function normalizeEpochMillis(rawValue) {
    const n = Number(rawValue);
    if (!n || Number.isNaN(n)) return 0;
    if (n > 0 && n < 100000000000) return n * 1000;
    return n;
}

function calcOverCharge(meta) {
    const durationMinutes = Number(meta?.durationMinutes || 0);
    const extraPricePerMin = Number(meta?.extraPricePerMin || 0);
    if (durationMinutes <= 0 || extraPricePerMin <= 0) return 0;

    const startMs = normalizeEpochMillis(meta?.sessionStartTime || 0);
    if (!startMs) return 0;
    const elapsedMs = Date.now() - startMs;
    const packageDurationMs = durationMinutes * 60 * 1000;
    const overMs = elapsedMs - packageDurationMs;
    if (overMs <= 0) return 0;

    const overUnits = Math.ceil(overMs / (10 * 60 * 1000));
    const partySize = Number(meta?.partySize || 1) || 1;
    return overUnits * extraPricePerMin * partySize;
}

async function refreshTableCardLiveAmounts() {
    const cards = Array.from(document.querySelectorAll('.table-card.status-occupied'));
    if (!cards.length) return;

    for (const card of cards) {
        const tableId = card.getAttribute('data-id');
        if (!tableId) continue;

        try {
            const res = await fetch(`/admin/dashboard/${tableId}/orders`, {
                headers: { 'Accept': 'application/json' },
                credentials: 'same-origin'
            });
            if (!res.ok) continue;

            const contentType = res.headers.get('content-type') || '';
            if (!contentType.includes('application/json')) continue;

            const orderItems = await res.json();
            const menuAmount = calcLiveAmount(orderItems);
            let packageAmount = 0;
            let overCharge = 0;
            try {
                const meta = await fetchCheckoutMeta(tableId);
                packageAmount = Number(meta?.packageTotal || 0);
                overCharge = calcOverCharge(meta);
            } catch (metaErr) {
                console.warn('정산 메타 조회 실패(주문합계만 표시):', metaErr);
            }

            const liveAmount = menuAmount + packageAmount + overCharge;
            syncCardLiveAmount(tableId, liveAmount);
        } catch (e) {
            console.error('테이블 카드 실시간 금액 갱신 실패:', e);
        }
    }
}

async function callUpdateStatus(status) {
    try {
        const response = await fetch(`/admin/dashboard/${currentTableId}/status`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: status })
        });
        const result = await response.json();
        if (response.ok) { location.reload(); }
        else { alert("❌ " + (result.error || "상태 변경에 실패했습니다.")); }
    } catch (err) {
        console.error(err);
        alert("서버와 통신 중 오류가 발생했습니다.");
    }
}

function handlePayment() {
    if (!currentTableId) {
        alert('선택된 테이블이 없습니다.');
        return;
    }
    if (currentTableStatus !== 'OCCUPIED') {
        alert("이용 중인 테이블만 결제가 가능합니다.");
        return;
    }
    // 결제완료 버튼은 항상 정산 페이지(checkout)로 즉시 이동한다.
    moveToCheckoutPage();
}

function moveToCheckoutPage() {
    if (!currentTableId) {
        alert('테이블 정보를 확인할 수 없어 정산 페이지로 이동할 수 없습니다.');
        return;
    }
    window.location.href = `/admin/dashboard/${currentTableId}/checkout`;
}

function getJsonHeaders() {
    const headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
    return headers;
}

async function openGameSerialModal(orderId, gameName, quantity) {
    if (!currentTableId) return;
    serialAssignContext = { orderId, gameName, quantity: Math.max(1, Number(quantity) || 1) };

    const modal = document.getElementById('serialAssignModal');
    const title = document.getElementById('serialAssignTitle');
    const list = document.getElementById('serialAssignList');
    title.textContent = `${gameName} / 수량 ${serialAssignContext.quantity}개`;

    try {
        const res = await fetch(`/admin/product/game-items/available?tableId=${currentTableId}&gameName=${encodeURIComponent(gameName)}`);
        if (!res.ok) throw new Error('일련번호 조회 실패');
        const items = await res.json();

        if (!Array.isArray(items) || items.length === 0) {
            list.innerHTML = '<div style="font-size:13px; color:#ef4444;">대여 가능한 일련번호가 없습니다.</div>';
        } else {
            list.innerHTML = items.map(item => `
                            <label class="serial-item-row">
                                <span>${item.serialNumber}</span>
                                <input type="checkbox" class="serial-checkbox" value="${item.id}">
                            </label>
                        `).join('');
        }

        modal.style.display = 'flex';
        setTimeout(() => {
            const content = modal.querySelector('.modal-content');
            if (content) content.style.transform = 'translateY(0)';
        }, 10);
    } catch (e) {
        alert('일련번호 조회에 실패했습니다: ' + e.message);
    }
}

function closeGameSerialModal() {
    const modal = document.getElementById('serialAssignModal');
    if (!modal) return;
    modal.style.display = 'none';
    const content = modal.querySelector('.modal-content');
    if (content) content.style.transform = 'translateY(-20px)';
}

async function submitGameSerialAssignment() {
    if (!currentTableId || !serialAssignContext.gameName) return;
    const selected = Array.from(document.querySelectorAll('.serial-checkbox:checked'))
        .map(el => Number(el.value))
        .filter(Boolean);

    if (selected.length === 0) {
        alert('일련번호를 선택해 주세요.');
        return;
    }
    if (selected.length > serialAssignContext.quantity) {
        alert(`최대 ${serialAssignContext.quantity}개까지만 선택할 수 있습니다.`);
        return;
    }

    try {
        const res = await fetch(`/admin/product/game-items/assign`, {
            method: 'POST',
            headers: getJsonHeaders(),
            body: JSON.stringify({
                tableId: Number(currentTableId),
                orderId: serialAssignContext.orderId,
                gameName: serialAssignContext.gameName,
                gameItemIds: selected
            })
        });
        const data = await res.json().catch(() => ({}));
        if (!res.ok || data.success === false) {
            throw new Error(data.message || '일련번호 적용 실패');
        }

        closeGameSerialModal();
        await fetchRentedGameItems();
        alert('일련번호가 대여중으로 반영되었습니다.');
    } catch (e) {
        alert('일련번호 적용 실패: ' + e.message);
    }
}

async function openRentalReturnModal(orderId, gameName, quantity) {
    if (!currentTableId) return;
    rentalReturnContext = {
        orderId: Number(orderId || 0),
        gameName: String(gameName || '').trim(),
        quantity: Math.max(1, Number(quantity) || 1)
    };

    const modal = document.getElementById('rentalReturnModal');
    const title = document.getElementById('rentalReturnTitle');
    const desc = document.getElementById('rentalReturnDesc');
    const list = document.getElementById('rentalReturnList');

    title.textContent = `↩️ ${rentalReturnContext.gameName || '게임'} 반납`;
    desc.textContent = '반납 처리할 일련번호를 선택하세요.';

    try {
        const res = await fetch(`/admin/product/game-items/rentals/active?tableId=${currentTableId}`);
        if (!res.ok) throw new Error('대여 목록 조회 실패');
        const rentals = await res.json();

        const matched = Array.isArray(rentals)
            ? rentals.filter(item =>
                (item?.gameName || '').trim() === rentalReturnContext.gameName)
            : [];

        if (matched.length === 0) {
            list.innerHTML = '<div style="font-size:13px; color:#ef4444;">해당 게임의 대여 중 일련번호가 없습니다.</div>';
        } else {
            list.innerHTML = matched.map(item => `
                            <label class="serial-item-row">
                                <span>${item.serialNumber}</span>
                                <input type="checkbox"
                                       class="rental-return-checkbox"
                                       data-history-id="${item.historyId}"
                                       data-game-item-id="${item.gameItemId}">
                            </label>
                        `).join('');
        }

        modal.style.display = 'flex';
        setTimeout(() => {
            const content = modal.querySelector('.modal-content');
            if (content) content.style.transform = 'translateY(0)';
        }, 10);
    } catch (e) {
        alert('반납 목록 조회 실패: ' + e.message);
    }
}

function closeRentalReturnModal() {
    const modal = document.getElementById('rentalReturnModal');
    if (!modal) return;
    modal.style.display = 'none';
    const content = modal.querySelector('.modal-content');
    if (content) content.style.transform = 'translateY(-20px)';
}

async function submitRentalReturn() {
    if (!currentTableId) return;

    const selected = Array.from(document.querySelectorAll('.rental-return-checkbox:checked'))
        .map(el => ({
            historyId: Number(el.dataset.historyId),
            gameItemId: Number(el.dataset.gameItemId),
            status: 'NORMAL'
        }))
        .filter(v => Number.isFinite(v.historyId) && v.historyId > 0
            && Number.isFinite(v.gameItemId) && v.gameItemId > 0);

    if (selected.length === 0) {
        alert('반납할 일련번호를 선택해 주세요.');
        return;
    }

    if (selected.length > rentalReturnContext.quantity) {
        alert(`최대 ${rentalReturnContext.quantity}개까지만 선택할 수 있습니다.`);
        return;
    }

    try {
        const res = await fetch(`/admin/product/game-items/rentals/settle`, {
            method: 'PATCH',
            headers: getJsonHeaders(),
            body: JSON.stringify({
                tableId: Number(currentTableId),
                updates: selected
            })
        });
        const data = await res.json().catch(() => ({}));
        if (!res.ok || data.success === false) {
            throw new Error(data.message || '반납 처리 실패');
        }

        closeRentalReturnModal();
        await fetchActiveOrders();
        await fetchRentedGameItems();
        alert('선택한 일련번호가 반납 처리되었습니다.');
    } catch (e) {
        alert('반납 처리 실패: ' + e.message);
    }
}

async function fetchRentedGameItems() {
    if (!currentTableId) return;
    const box = document.getElementById('rentedGameList');
    if (!box) return;

    try {
        const [activeRes, historyRes] = await Promise.all([
            fetch(`/admin/product/game-items/rentals/active?tableId=${currentTableId}`),
            fetch(`/admin/product/game-items/rentals/history?tableId=${currentTableId}`)
        ]);
        if (!activeRes.ok || !historyRes.ok) throw new Error('대여 목록 조회 실패');

        const activeItems = await activeRes.json();
        const historyItems = await historyRes.json();

        const rentedItems = Array.isArray(activeItems)
            ? activeItems.filter(item => String(item?.rentalStatus || '').toUpperCase() === 'RENTED')
            : [];
        const returnedItems = Array.isArray(historyItems)
            ? historyItems.filter(item => String(item?.rentalStatus || '').toUpperCase() !== 'RENTED')
            : [];

        const pendingGroups = (() => {
            const result = [];
            const byOrder = new Map();
            const source = Array.isArray(latestModalOrderItems) ? latestModalOrderItems : [];
            for (const item of source) {
                const price = Number(item?.price ?? item?.menuPrice ?? 0);
                const status = String(item?.status || '').toUpperCase();
                if (price !== 0 || status !== 'ORDERED') continue;

                const orderId = Number(item?.orderId || item?.id || 0);
                const gameName = String(item?.menu_name || item?.menuName || '').trim();
                const qty = Number(item?.quantity || 0) || 0;
                if (!orderId || !gameName || qty <= 0) continue;

                const key = `${orderId}::${gameName}`;
                const cur = byOrder.get(key) || { orderId, gameName, quantity: 0 };
                cur.quantity += qty;
                byOrder.set(key, cur);
            }
            byOrder.forEach(v => result.push(v));
            return result;
        })();

        const pendingHtml = pendingGroups.length > 0
            ? `
                            <div style="font-weight:700; color:#0d6efd; margin-bottom:6px;">요청 대기 게임</div>
                            ${pendingGroups.map(item => `
                                <div class="serial-item-row">
                                    <div>
                                        <div style="font-weight:600;">${item.gameName}</div>
                                        <div style="font-size:12px; color:#64748b;">수량 ${item.quantity}개</div>
                                    </div>
                                    <button class="inline-small-btn"
                                            onclick='openGameSerialModal(${item.orderId}, ${JSON.stringify(item.gameName)}, ${item.quantity})'>
                                        일련번호 선택
                                    </button>
                                </div>
                            `).join('')}
                        `
            : `
                            <div style="font-weight:700; color:#0d6efd; margin-bottom:6px;">요청 대기 게임</div>
                            <div style="font-size:12px; color:#64748b; margin-bottom:10px;">없음</div>
                        `;

        const rentedHtml = rentedItems.length > 0
            ? `
                            <div style="font-weight:700; color:#1d4ed8; margin-top:10px; margin-bottom:6px;">대여중 일련번호</div>
                            ${rentedItems.map(item => `
                                <label class="serial-item-row">
                                    <div>
                                        <div style="font-weight:600;">${item.gameName}</div>
                                        <div style="font-size:12px; color:#64748b;">${item.serialNumber}</div>
                                    </div>
                                    <input type="checkbox"
                                           class="inline-rental-checkbox"
                                           data-history-id="${item.historyId}"
                                           data-game-item-id="${item.gameItemId}">
                                </label>
                            `).join('')}
                            <div style="display:flex; justify-content:flex-end; margin-top:8px;">
                                <button class="inline-small-btn return"
                                        onclick="settleSelectedRentedFromList()">반납 선택</button>
                            </div>
                        `
            : `
                            <div style="font-weight:700; color:#1d4ed8; margin-top:10px; margin-bottom:6px;">대여중인 게임/일련번호 </div>
                            <div style="font-size:12px; color:#64748b; margin-bottom:10px;">없음</div>
                        `;

        const returnedHtml = returnedItems.length > 0
            ? `
                            <div style="font-weight:700; color:#64748b; margin-top:10px; margin-bottom:6px;">반납 이력</div>
                            ${returnedItems.map(item => `
                                <div class="rented-game-row">${item.gameName} / ${item.serialNumber}</div>
                            `).join('')}
                        `
            : `
                            <div style="font-weight:700; color:#64748b; margin-top:10px; margin-bottom:6px;">반납 이력</div>
                            <div style="font-size:12px; color:#64748b;">없음</div>
                        `;

        box.innerHTML = `${pendingHtml}${rentedHtml}${returnedHtml}`;
    } catch (e) {
        box.innerHTML = '<span style="color:#ef4444;">조회 실패</span>';
    }
}

async function settleSelectedRentedFromList() {
    if (!currentTableId) return;

    const updates = Array.from(document.querySelectorAll('.inline-rental-checkbox:checked'))
        .map(el => ({
            historyId: Number(el.dataset.historyId),
            gameItemId: Number(el.dataset.gameItemId),
            status: 'NORMAL'
        }))
        .filter(v => Number.isFinite(v.historyId) && v.historyId > 0
            && Number.isFinite(v.gameItemId) && v.gameItemId > 0);

    if (updates.length === 0) {
        alert('반납 처리할 일련번호를 선택해 주세요.');
        return;
    }

    try {
        const res = await fetch(`/admin/product/game-items/rentals/settle`, {
            method: 'PATCH',
            headers: getJsonHeaders(),
            body: JSON.stringify({ tableId: Number(currentTableId), updates })
        });
        const data = await res.json().catch(() => ({}));
        if (!res.ok || data.success === false) {
            throw new Error(data.message || '반납 처리 실패');
        }

        await fetchActiveOrders();
        await refreshTableCardOrderBadges();
        await fetchRentedGameItems();
        alert('선택한 일련번호를 정상 반납 처리했습니다.');
    } catch (e) {
        alert('반납 처리 실패: ' + e.message);
    }
}

async function openPaymentGameCheckModal() {
    if (!currentTableId) return;

    try {
        const res = await fetch(`/admin/product/game-items/rentals/active?tableId=${currentTableId}`);
        if (!res.ok) throw new Error('대여 목록 조회 실패');
        const items = await res.json();

        const title = document.getElementById('paymentGameCheckTitle');
        const desc = document.getElementById('paymentGameCheckDesc');
        const confirmBtn = document.getElementById('paymentGameCheckConfirmBtn');
        if (title) title.textContent = '🧹 빈테이블 전환 전 게임 상태 확인';
        if (desc) desc.textContent = '대여 중인 일련번호 상태를 확인한 뒤 빈테이블로 전환합니다.';
        if (confirmBtn) confirmBtn.textContent = '상태 반영 후 빈테이블 전환';

        if (!Array.isArray(items) || items.length === 0) {
            await callUpdateStatus('EMPTY');
            return;
        }

        const modal = document.getElementById('paymentGameCheckModal');
        const list = document.getElementById('paymentGameCheckList');
        list.innerHTML = items.map(item => `
                        <div class="serial-item-row">
                            <div>
                                <div style="font-weight:600;">${item.gameName}</div>
                                <div style="font-size:12px; color:#64748b;">${item.serialNumber}</div>
                            </div>
                            <select class="payment-game-status" data-history-id="${item.historyId}" data-game-item-id="${item.gameItemId}" style="padding:8px; border-radius:8px; border:1px solid #ddd;">
                                <option value="NORMAL">정상 반납</option>
                                <option value="DAMAGED">손상</option>
                                <option value="LOST">분실</option>
                            </select>
                        </div>
                    `).join('');

        modal.style.display = 'flex';
        setTimeout(() => {
            const content = modal.querySelector('.modal-content');
            if (content) content.style.transform = 'translateY(0)';
        }, 10);
    } catch (e) {
        alert('결제 전 확인 정보를 불러오지 못했습니다: ' + e.message);
    }
}

function closePaymentGameCheckModal() {
    const modal = document.getElementById('paymentGameCheckModal');
    if (!modal) return;
    modal.style.display = 'none';
    const content = modal.querySelector('.modal-content');
    if (content) content.style.transform = 'translateY(-20px)';
}

async function confirmPaymentWithGameItemCheck() {
    if (!currentTableId) return;
    const updates = Array.from(document.querySelectorAll('.payment-game-status')).map(el => ({
        historyId: Number(el.dataset.historyId),
        gameItemId: Number(el.dataset.gameItemId),
        status: el.value === 'NORMAL' ? 'NORMAL' : el.value
    }));

    try {
        if (updates.length > 0) {
            const res = await fetch(`/admin/product/game-items/rentals/settle`, {
                method: 'PATCH',
                headers: getJsonHeaders(),
                body: JSON.stringify({ tableId: Number(currentTableId), updates })
            });
            const data = await res.json().catch(() => ({}));
            if (!res.ok || data.success === false) {
                throw new Error(data.message || '일련번호 상태 반영 실패');
            }
        }

        closePaymentGameCheckModal();
        await callUpdateStatus('EMPTY');
    } catch (e) {
        alert('결제 전 상태 반영 실패: ' + e.message);
    }
}

async function handleStatusUpdate() {
    if (!currentTableId || !currentTableStatus) return;

    const selectedTable = document.querySelector(`.table-card[data-id="${currentTableId}"]`);
    const token = selectedTable ? selectedTable.getAttribute('data-token') : null;

    if (currentTableStatus === 'EMPTY') {
        if (!token || token.trim() === "" || token === 'null') {
            alert("⚠️ 인증 토큰이 없는 테이블입니다.\n토큰 발급 후에만 입실(OCCUPIED)이 가능합니다.");
            return;
        }
    }

    let nextStatus = '';
    let confirmMsg = '';
    if (currentTableStatus === 'EMPTY') { nextStatus = 'OCCUPIED'; confirmMsg = "손님 입실 처리를 하시겠습니까?"; }
    else if (currentTableStatus === 'OCCUPIED') { nextStatus = 'CLEANING'; confirmMsg = "퇴실 및 청소 중 상태로 변경하시겠습니까?"; }
    else if (currentTableStatus === 'CLEANING') {
        openPaymentGameCheckModal();
        return;
    }

    if (confirm(confirmMsg)) { await callUpdateStatus(nextStatus); }
}

async function handleTotalReset() {
    if (!confirm("⚠️ 주의: 모든 데이터가 초기화됩니다. 진행하시겠습니까?")) return;
    try {
        const response = await fetch('/admin/dashboard/reset', { method: 'DELETE' });
        if (response.ok) { location.reload(); }
    } catch (err) { console.error(err); }
}

function openMessageModal() {
    const targetSelect = document.getElementById('targetTableSelect');
    targetSelect.innerHTML = '<option value="">테이블을 선택하세요</option>';
    targetSelect.innerHTML += '<option value="ALL" style="font-weight:bold; color:#4285f4;">📢 이용 중인 전체 테이블</option>';

    document.querySelectorAll('.table-card.status-occupied').forEach(card => {
        const tableId = card.getAttribute('data-id');
        const tableNum = card.getAttribute('data-num');
        targetSelect.innerHTML += `<option value="${tableId}">Table ${tableNum}</option>`;
    });

    fetchMacroMessagesForAdmin();

    const modal = document.getElementById('messageModal');
    if (modal) {
        modal.style.display = 'flex';
        setTimeout(() => {
            const content = modal.querySelector('.modal-content');
            if (content) content.style.transform = 'translateY(0)';
        }, 10);
    }
}

function closeMessageModal() {
    const modal = document.getElementById('messageModal');
    if (modal) {
        modal.style.display = 'none';
        const content = modal.querySelector('.modal-content');
        if (content) content.style.transform = 'translateY(-20px)';
    }
    document.getElementById('targetTableSelect').value = '';
    document.getElementById('macroMessageSelect').value = '';
}

async function fetchMacroMessagesForAdmin() {
    try {
        const response = await fetch('/admin/macro/api?direction=STAFF_TO_TABLE');
        if (!response.ok) throw new Error("HTTP error! status: " + response.status);

        const dataList = await response.json();
        const msgSelect = document.getElementById('macroMessageSelect');
        if (!msgSelect) return;

        msgSelect.innerHTML = '<option value="">전송할 메세지를 선택하세요</option>';

        if (dataList && dataList.length > 0) {
            dataList.forEach(msg => {
                const realId = msg.id || msg.macroMessageId;
                const text = msg.messageText || "내용 없음";
                if (realId) {
                    msgSelect.innerHTML += `<option value="${realId}">${text}</option>`;
                }
            });
        }
    } catch (err) { console.error("매크로 메세지 로딩 실패:", err); }
}

async function submitMessage() {
    const tableSelect = document.getElementById('targetTableSelect');
    const messageSelect = document.getElementById('macroMessageSelect');
    const tableId = tableSelect.value;
    const messageId = messageSelect.value;

    if (!tableId || !messageId) {
        return alert('테이블과 메세지를 선택해주세요.');
    }

    const sendData = {
        macroMessageId: Number(messageId),
        tableId: (tableId === "ALL") ? "ALL" : Number(tableId)
    };

    try {
        const response = await fetch('/admin/macro/api/send', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(sendData)
        });

        if (response.ok) {
            alert(tableId === "ALL" ? '📢 전체 메세지가 전송되었습니다.' : '✅ 메세지가 전송되었습니다.');
            closeMessageModal();
            if(tableId === "ALL") location.reload();
        } else {
            const errorData = await response.text();
            alert('❌ 전송 실패: ' + errorData);
        }
    } catch (err) {
        console.error(err);
        alert('서버와 통신 중 오류가 발생했습니다.');
    }
}

async function fetchPendingOrders() {
    try {
        const response = await fetch('/admin/api/dashboard/orders/latest');

        if (response.ok) {
            const data = await response.json();
            const orders = Array.isArray(data) ? data : (data.orders || []);
            renderPendingOrders(orders);
        } else {
            console.error('❌ 신규 주문 API 응답 실패:', response.status);
            renderPendingOrders([]);
        }
    } catch (err) {
        console.error('❌ fetchPendingOrders 에러:', err);
        renderPendingOrders([]);
    }
}

function renderPendingOrders(orders) {
    const section = document.getElementById('pending-orders-section');
    const list = document.getElementById('pending-orders-list');
    const badge = document.getElementById('pending-count');
    const bulkBtn = document.getElementById('pending-confirm-all-btn');
    if (!section || !list || !badge) return;
    pendingOrdersState = Array.isArray(orders) ? orders : [];
    const visibleOrders = pendingOrdersState.filter(order => parsePendingOrderId(order) > 0);

    if (!visibleOrders || visibleOrders.length === 0) {
        section.classList.remove('has-orders');
        badge.innerText = '0';
        list.innerHTML = '';
        if (bulkBtn) bulkBtn.style.display = 'none';
        return;
    }

    section.classList.add('has-orders');
    badge.innerText = visibleOrders.length;
    if (bulkBtn) bulkBtn.style.display = 'inline-block';

    list.innerHTML = visibleOrders.map((order) => {
        const items = order.items || order.orderItems || [];
        const itemSummary = items
            .map(i => {
                const menuName = i.menu_name || i.menuName || '상품';
                const qty = i.quantity || i.qty || 0;
                return `${menuName} x${qty}`;
            })
            .join(', ');

        const tableId = order.tableId || order.table_id || '?';
        const totalAmount = order.totalAmount || order.total_amount || 0;
        const orderId = parsePendingOrderId(order);
        const isGameOrder = isGameOnlyPendingOrder(order);
        const actionLabel = isGameOrder ? '일련번호 선택' : '주문 확인';
        const actionButton = `<button class="btn-pending-confirm" onclick="confirmPendingOrder(${orderId})">${actionLabel}</button>`;
        const typeLabel = isGameOrder ? '대여 주문' : '일반 주문';

        return `
                    <div class="pending-order-card">
                        <div class="pending-order-info">
                            <div class="pending-order-title">
                                Table #${tableId} &nbsp;·&nbsp; ${typeLabel} &nbsp;·&nbsp; ₩${totalAmount.toLocaleString()}
                            </div>
                            <div class="pending-order-items">${itemSummary || '항목 없음'}</div>
                        </div>
                        <div class="pending-order-actions">
                            ${actionButton}
                        </div>
                    </div>`;
    }).join('');
}

async function confirmAllPendingOrders() {
    const visible = pendingOrdersState.filter(order => {
        const id = parsePendingOrderId(order);
        return id > 0;
    });

    if (visible.length === 0) {
        alert('확인할 신규 주문이 없습니다.');
        return;
    }

    if (!confirm(`신규 주문 ${visible.length}건을 확인 처리할까요?`)) {
        return;
    }

    let success = 0;
    let fail = 0;
    let gameOrders = 0;
    for (const order of visible) {
        const orderId = parsePendingOrderId(order);
        if (isGameOnlyPendingOrder(order)) {
            gameOrders++;
            continue;
        }
        const ok = await updateOrderStatusSilent(orderId, 'CONFIRMED');
        if (ok) success++;
        else fail++;
    }

    await fetchPendingOrders();
    await refreshTableCardOrderBadges();

    if (gameOrders > 0) {
        if (fail === 0) alert(`일반 주문 ${success}건 확인 완료\n게임 주문 ${gameOrders}건은 일련번호 배정 후 확인됩니다.`);
        else alert(`일반 주문 완료 ${success}건 / 실패 ${fail}건\n게임 주문 ${gameOrders}건은 일련번호 배정 후 확인됩니다.`);
    } else {
        if (fail === 0) alert(`신규 주문 ${success}건 확인 완료`);
        else alert(`완료 ${success}건 / 실패 ${fail}건`);
    }
}

async function confirmPendingOrder(orderId) {
    const id = Number(orderId || 0);
    if (id <= 0) return;

    const target = pendingOrdersState.find(o => parsePendingOrderId(o) === id);
    if (target && isGameOnlyPendingOrder(target)) {
        const tableId = Number(target.tableId || target.table_id || 0);
        await openPendingGameOrder(tableId);
        return;
    }

    const ok = await updateOrderStatusSilent(id, 'CONFIRMED');
    if (!ok) {
        alert(`주문 확인 처리 실패 (ID: ${id})`);
        return;
    }

    await fetchPendingOrders();
    await refreshTableCardOrderBadges();
    alert(`일반 신규 주문 1건을 확인했습니다. (ID: ${id})`);
}

async function openPendingGameOrder(tableId) {
    const numericTableId = Number(tableId || 0);
    if (!numericTableId) {
        alert('테이블 정보를 확인할 수 없습니다.');
        return;
    }
    let card = document.querySelector(`.table-card[data-id="${numericTableId}"]`);
    if (!card) {
        // 백엔드가 tableId 대신 tableNumber를 내려준 경우를 대비한 폴백
        card = document.querySelector(`.table-card[data-num="${numericTableId}"]`);
    }
    if (!card) {
        alert(`Table #${numericTableId} 카드를 찾을 수 없습니다.`);
        return;
    }
    await openTableModal(card);
}

document.addEventListener('DOMContentLoaded', () => {
    fetchPendingOrders();
    refreshTableCardOrderBadges();
    refreshTableCardLiveAmounts();
    const pendingOrdersInterval = setInterval(() => {
        fetchPendingOrders();
    }, 5000);
    const tableOrderBadgeInterval = setInterval(() => {
        refreshTableCardOrderBadges();
    }, 5000);
    const tableLiveAmountInterval = setInterval(() => {
        refreshTableCardLiveAmounts();
    }, 5000);

    async function pollTableSnapshot() {
        try {
            const res = await fetch('/admin/dashboard/tables', {
                headers: { 'Accept': 'application/json' },
                credentials: 'same-origin'
            });
            if (!res.ok) return;

            const contentType = res.headers.get('content-type') || '';
            if (!contentType.includes('application/json')) {
                // 세션 만료 등으로 로그인 HTML이 반환된 경우
                if (res.redirected && res.url) {
                    window.location.href = res.url;
                }
                return;
            }

            const tables = await res.json();
            if (!Array.isArray(tables)) return;

            let changed = false;
            for (const t of tables) {
                const card = document.querySelector(`.table-card[data-id="${t.id}"]`);
                if (!card) continue;

                const currentStatus = card.getAttribute('data-status');
                const currentHasMsg = card.classList.contains('has-msg');
                const serverHasMsg = !!t.hasUnreadMessage;

                // 상태 변경 감지
                if (currentStatus !== t.status) {
                    changed = true;
                    break;
                }

                // 메시지 알림 점은 페이지 리로드 없이 즉시 반영
                if (currentHasMsg !== serverHasMsg) {
                    if (serverHasMsg) card.classList.add('has-msg');
                    else card.classList.remove('has-msg');
                }
            }

            // 모달이 열려있으면 사용자 작업 방해를 피하기 위해 갱신 보류
            const tableModal = document.getElementById('tableModal');
            const isModalOpen = tableModal && tableModal.style.display === 'flex';
            if (changed && !isModalOpen) {
                location.reload();
            }
        } catch (e) {
            console.error('❌ 테이블 상태 폴링 실패:', e);
        }
    }

    const tableSnapshotInterval = setInterval(pollTableSnapshot, 3000);

    window.addEventListener('beforeunload', () => {
        clearInterval(pendingOrdersInterval);
        clearInterval(tableOrderBadgeInterval);
        clearInterval(tableLiveAmountInterval);
        clearInterval(tableSnapshotInterval);
    });
});