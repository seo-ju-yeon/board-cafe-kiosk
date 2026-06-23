/* 실시간 시계 표시 */
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

/* 관리자 사이드바 활성 메뉴 표시 */
document.addEventListener("DOMContentLoaded", function() {
    const currentPath = window.location.pathname;

    // 현재 경로와 비교할 사이드바 메뉴 링크
    const productMenu = document.querySelector('a[href*="/admin/product"]');
    const staffMenu = document.querySelector('a[href*="/admin/staff"]');
    const profileMenu = document.querySelector('a[href*="/admin/staff/profile"]');
    const macroMenu = document.querySelector('a[href*="/admin/macro"]');
    const dashboardMenu = document.querySelector('a[href*="/admin/dashboard"]');

    // 테이블 현황 메뉴 활성화
    if (currentPath.includes('/admin/dashboard') && dashboardMenu) {
        dashboardMenu.classList.add('active');
    }

    // 상품 관리 및 카테고리 관리 메뉴 활성화
    if (currentPath.includes('/admin/product') && productMenu) {
        productMenu.classList.add('active');
    }
    if (currentPath.includes('/admin/category') && productMenu) {
        productMenu.classList.add('active');
    }

    // 매크로 메시지 메뉴 활성화
    if (currentPath.includes('/admin/macro') && macroMenu) {
        macroMenu.classList.add('active');
    }

    // 직원 관리와 내 정보 관리의 경로 중첩 처리
    if (currentPath.includes('/admin/staff')) {
        if (currentPath.includes('/admin/staff/profile')) {
            // 내 정보 관리 메뉴 활성화
            if (profileMenu) profileMenu.classList.add('active');
            if (staffMenu) staffMenu.classList.remove('active'); // 직원 관리 메뉴 비활성화
        } else {
            // 직원 관리 메뉴 활성화
            if (staffMenu) staffMenu.classList.add('active');
        }
    }
});
