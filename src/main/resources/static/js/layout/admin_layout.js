// [실시간 시계 기능]
function updateClock() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const date = String(now.getDate()).padStart(2, '0');
    const dayNames = ['일', '월', '화', '수', '목', '금', '토'];
    const day = dayNames[now.getDay()];

    document.getElementById('current-date').textContent = `${year}. ${month}. ${date} (${day})`;

    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');
    document.getElementById('current-time').textContent = `${hours}:${minutes}:${seconds}`;
}

setInterval(updateClock, 1000);
updateClock();

// [상품 메뉴 활성화 보정]
document.addEventListener("DOMContentLoaded", function() {
    const currentPath = window.location.pathname;

    // 모든 메뉴 링크를 가져옵니다.
    const productMenu = document.querySelector('a[href*="/admin/product"]');
    const staffMenu = document.querySelector('a[href*="/admin/staff"]');
    const profileMenu = document.querySelector('a[href*="/admin/staff/profile"]');
    const macroMenu = document.querySelector('a[href*="/admin/macro"]');
    const dashboardMenu = document.querySelector('a[href*="/admin/dashboard"]');

    // --- 1. 테이블 현황 (대시보드) 활성화 ---
    if (currentPath.includes('/admin/dashboard') && dashboardMenu) {
        dashboardMenu.classList.add('active');
    }

    // --- 2. 상품 관리 활성화 ---
    if (currentPath.includes('/admin/product') && productMenu) {
        productMenu.classList.add('active');
    }
    if (currentPath.includes('/admin/category') && productMenu) {
        productMenu.classList.add('active');
    }

    // --- 3. 매크로 메시지 활성화 ---
    if (currentPath.includes('/admin/macro') && macroMenu) {
        macroMenu.classList.add('active');
    }

    // --- 4. 직원 관리 및 내 정보 관리 (경로 중첩 처리) ---
    if (currentPath.includes('/admin/staff')) {
        if (currentPath.includes('/admin/staff/profile')) {
            // '내 정보 관리'인 경우
            if (profileMenu) profileMenu.classList.add('active');
            if (staffMenu) staffMenu.classList.remove('active'); // 직원 관리 불 끄기
        } else {
            // '직원 관리' 본체인 경우
            if (staffMenu) staffMenu.classList.add('active');
        }
    }
});