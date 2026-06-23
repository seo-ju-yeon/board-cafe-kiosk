/* 패키지 목록 페이징 상태 */
const PAGE_SIZE = 6;

let currentPage = 0;  // 현재 페이지 번호
let selectedPackageId = null;
let selectedPackageName = "";
let selectedPackagePrice = 0;

/* 패키지 페이지 렌더링 */
function renderPage() {
    const start = currentPage * PAGE_SIZE;
    const end = start + PAGE_SIZE;
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

    // 이전/다음 버튼 활성 상태 갱신
    document.getElementById('prev-btn').disabled = (currentPage === 0);
    document.getElementById('next-btn').disabled = (currentPage >= totalPages - 1);

    lucide.createIcons();
}

/* 패키지 이용 시간 표시값 변환 */
function getDisplayTime(minutes) {
    if (!minutes) return 'Free';
    if (minutes < 60) return minutes + '분';
    return (minutes / 60) + '시간';
}

/* 이전 패키지 페이지 이동 */
function prevPage() {
    if (currentPage > 0) {
        currentPage--;
        renderPage();
    }
}

/* 다음 패키지 페이지 이동 */
function nextPage() {
    const totalPages = Math.ceil(ALL_PACKAGES.length / PAGE_SIZE);
    if (currentPage < totalPages - 1) {
        currentPage++;
        renderPage();
    }
}

/* 이용 패키지 선택 */
function selectPackage(el) {
    document.querySelectorAll('.package-card').forEach(c => c.classList.remove('selected'));
    el.classList.add('selected');

    selectedPackageId = parseInt(el.dataset.id);
    selectedPackageName = el.dataset.name;
    selectedPackagePrice = parseInt(el.dataset.price);

    const btn = document.getElementById('next-button');
    btn.classList.add('active');
    btn.innerText = `[${selectedPackageName}] 선택 완료 - 메뉴로 이동`;
}

/* 선택 패키지 저장 후 메뉴 이동 */
function completeSelection() {
    if (!selectedPackageId) return;

    fetch('/kiosk/package/select', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({packageId: selectedPackageId})
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

/* 패키지 선택 화면 초기 렌더링 */
renderPage();
