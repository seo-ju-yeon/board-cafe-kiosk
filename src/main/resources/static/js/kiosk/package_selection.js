const PAGE_SIZE     = 6;

let currentPage       = 0; // 0-based
let selectedPackageId   = null;
let selectedPackageName = "";
let selectedPackagePrice = 0;

// ===== 페이지 렌더링 =====
function renderPage() {
    const start   = currentPage * PAGE_SIZE;
    const end     = start + PAGE_SIZE;
    const pageItems = ALL_PACKAGES.slice(start, end);
    const totalPages = Math.ceil(ALL_PACKAGES.length / PAGE_SIZE);

    const grid = document.getElementById('package-grid');

    if (ALL_PACKAGES.length === 0) {
        grid.innerHTML = `<div class="empty-msg" style="grid-column:1/-1;">등록된 패키지가 없습니다.</div>`;
    } else {
        grid.innerHTML = pageItems.map(pkg => `
        <div class="package-card ${selectedPackageId === pkg.id ? 'selected' : ''}"
             data-id="${pkg.id}"
             data-name="${pkg.name}"
             data-price="${pkg.basePrice}"
             onclick="selectPackage(this)">
          <div class="pkg-name">${pkg.name}</div>
          <div class="pkg-time">${pkg.displayTime ?? getDisplayTime(pkg.durationMinutes)}</div>
          <div class="pkg-type">${pkg.type}</div>
          <div class="pkg-desc">${pkg.extraPricePerMin > 0 ? '초과 시 10분당 ' + pkg.extraPricePerMin + '원' : '초과 요금 없음'}</div>
          <div class="pkg-price">
            ${Number(pkg.basePrice).toLocaleString()}원
            <span class="pkg-unit">(1인)</span>
          </div>
        </div>`).join('');
    }

    // 화살표 버튼 상태 업데이트
    document.getElementById('prev-btn').disabled = (currentPage === 0);
    document.getElementById('next-btn').disabled = (currentPage >= totalPages - 1);

    lucide.createIcons();
}

function getDisplayTime(minutes) {
    if (!minutes) return 'Free';
    if (minutes < 60) return minutes + '분';
    return (minutes / 60) + '시간';
}

function prevPage() {
    if (currentPage > 0) {
        currentPage--;
        renderPage();
    }
}

function nextPage() {
    const totalPages = Math.ceil(ALL_PACKAGES.length / PAGE_SIZE);
    if (currentPage < totalPages - 1) {
        currentPage++;
        renderPage();
    }
}

// ===== 패키지 선택 =====
function selectPackage(el) {
    document.querySelectorAll('.package-card').forEach(c => c.classList.remove('selected'));
    el.classList.add('selected');

    selectedPackageId    = parseInt(el.dataset.id);
    selectedPackageName  = el.dataset.name;
    selectedPackagePrice = parseInt(el.dataset.price);

    const btn = document.getElementById('next-button');
    btn.classList.add('active');
    btn.innerText = `[${selectedPackageName}] 선택 완료 - 메뉴로 이동`;
}

function completeSelection() {
    if (!selectedPackageId) return;

    fetch('/kiosk/package/select', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ packageId: selectedPackageId })
    })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                location.href = `/kiosk/drinks?tableNumber=${TABLE_NUMBER}`;
            } else {
                alert('패키지 선택에 실패했습니다. 다시 시도해주세요.');
            }
        })
        .catch(() => {
            alert('오류가 발생했습니다. 다시 시도해주세요.');
        });
}

// 초기 렌더링
renderPage();