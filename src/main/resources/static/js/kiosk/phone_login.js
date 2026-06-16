let rawNumber = "";

function appendNumber(num) {
    if (rawNumber.length < 11) {
        rawNumber += num;
        renderPhone();
    }
}

function deleteLast() {
    rawNumber = rawNumber.slice(0, -1);
    renderPhone();
}

function clearAll() {
    rawNumber = "";
    renderPhone();
}

function renderPhone() {
    const display = document.getElementById('phone-display');
    const nextBtn = document.getElementById('next-button');

    let formatted = rawNumber;
    if (rawNumber.length > 3 && rawNumber.length <= 7) {
        formatted = rawNumber.slice(0, 3) + "-" + rawNumber.slice(3);
    } else if (rawNumber.length > 7) {
        formatted = rawNumber.slice(0, 3) + "-" + rawNumber.slice(3, 7) + "-" + rawNumber.slice(7);
    }

    display.innerText = formatted;

    if (rawNumber.length >= 10) {
        nextBtn.classList.add('active');
    } else {
        nextBtn.classList.remove('active');
    }
}

function identifyMember() {
    if (rawNumber.length < 10) return;

    const btn = document.getElementById('next-button');
    btn.disabled = true;
    btn.innerHTML = '<i data-lucide="loader" style="width:20px;"></i> 조회 중...';
    lucide.createIcons();

    // 전화번호 포맷 (010-XXXX-XXXX)
    const formatted = rawNumber.slice(0,3) + '-' + rawNumber.slice(3,7) + '-' + rawNumber.slice(7);

    fetch(`/kiosk/point/lookup?phone=${encodeURIComponent(formatted)}`)
        .then(res => res.json())
        .then(data => {
            if (data.exists) {
                showToast(`환영합니다! 현재 포인트: ${data.balance.toLocaleString()}P`, 'success');
                setTimeout(() => location.href = `/kiosk/package_selection?tableNumber=${TABLE_NUMBER}&size=${PARTY_SIZE}`, 1800);
            } else {
                showToast('신규 회원으로 등록되었습니다! 포인트 적립이 시작됩니다.', 'info');
                setTimeout(() => location.href = `/kiosk/package_selection?tableNumber=${TABLE_NUMBER}&size=${PARTY_SIZE}`, 1800);
            }
        })
        .catch(() => {
            showToast('오류가 발생했습니다. 다시 시도해주세요.', 'error');
            btn.disabled = false;
            btn.innerHTML = '<i data-lucide="check-circle" style="width:20px;"></i> 확인 및 조회';
            lucide.createIcons();
        });
}

function skipStep() {
    // 포인트 조회 없이 패키지 선택으로 바로 이동
    location.href = `/kiosk/package_selection?tableNumber=${TABLE_NUMBER}&size=${PARTY_SIZE}`;
}

function getTableNumber() {
    return TABLE_NUMBER;
}

function getPartySize() {
    return PARTY_SIZE;
}

function showToast(message, type) {
    const colors = { success: '#2e7d32', info: '#1565c0', error: '#c62828' };
    const toast = document.createElement('div');
    toast.style.cssText = `
      position:fixed; bottom:40px; left:50%; transform:translateX(-50%);
      background:${colors[type] || '#333'}; color:white;
      padding:16px 28px; border-radius:14px; font-size:1rem; font-weight:600;
      z-index:9999; box-shadow:0 8px 24px rgba(0,0,0,0.2);
      animation: fadeIn 0.3s ease;
    `;
    toast.innerText = message;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 2500);
}