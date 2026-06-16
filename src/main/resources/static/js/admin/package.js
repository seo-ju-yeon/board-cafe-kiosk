// CSRF 토큰을 메타 태그에서 읽어오는 헬퍼
function getCsrfHeaders() {
    const token  = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
    return {
        'Content-Type': 'application/json',
        [header]: token   // 예: 'X-CSRF-TOKEN': 'abc123...'
    };
}

function openPolicyModal() { document.getElementById('policyModal').style.display = 'flex'; }
function closePolicyModal(e) {
    if (e && e.target !== document.getElementById('policyModal')) return;
    document.getElementById('policyModal').style.display = 'none';
    document.getElementById('policyForm').reset();
    document.getElementById('baseTime').value = 60;
    document.getElementById('basePrice').value = 5000;
    document.getElementById('extraPrice').value = 0;
}

function stepValue(inputId, delta) {
    const input = document.getElementById(inputId);
    let val = parseInt(input.value) || 0;
    val += delta;
    if (val < 0) val = 0;
    input.value = val;
}

// ===== 패키지 등록 =====
function savePolicy() {
    const name = document.getElementById('policyName').value.trim();
    const type     = document.querySelector('input[name="policyType"]:checked').value;
    const price    = parseInt(document.getElementById('basePrice').value);

    // FREE 타입이면 duration, extra는 null
    const duration = type === 'FREE' ? null : parseInt(document.getElementById('baseTime').value);
    const extra    = type === 'FREE' ? null : (parseFloat(document.getElementById('extraPrice').value) || null);

    if(!name) { alert("정책 명칭을 입력해주세요."); return; }

    fetch('/admin/policy/insert', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({
            name: name,
            type: type,
            durationMinutes: duration,
            basePrice: price,
            extraPricePerMin: extra
        })
    })
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                showToast('O ' + data.message);
                closePolicyModal();
                setTimeout(() => {
                    window.location.href = `/admin/policy?page=1&filter=${currentFilter}`;  // ← location.reload() 대신
                }, 800);
            } else {
                showToast('X ' + data.message);
            }
        })
        .catch(() => showToast('X 오류가 발생했습니다.'));
}

// ===== 활성/비활성 토글 =====
function toggleStatus(btn, active) {
    const id = parseInt(btn.getAttribute('data-id'));

    fetch('/admin/policy/status', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({id: id, active: active})
    })
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                showToast('O' + data.message);
                setTimeout(() => {
                    window.location.href = `/admin/policy?page=${currentPage}&filter=${currentFilter}`;  // ← location.reload() 대신
                }, 800);
            } else {
                showToast('X ' + data.message);
            }
        })
        .catch(() => showToast('X 오류가 발생했습니다.'));
}

function filterTable(filter) {
    window.location.href = `/admin/policy?page=1&filter=${filter}`;
}

function onTypeChange(type) {
    const timeRow  = document.getElementById('time-row');
    const extraRow = document.getElementById('extra-row');
    if (type === 'FREE') {
        timeRow.style.display  = 'none';
        extraRow.style.display = 'none';
    } else {
        timeRow.style.display  = '';
        extraRow.style.display = '';
    }
}

// ===== 토스트 =====
function showToast(message) {
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.innerText = message;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 2500);
}