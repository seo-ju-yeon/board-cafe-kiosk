const screen = document.getElementById('tap-to-start');

screen.addEventListener('click', function() {
    const returnUrl = localStorage.getItem('returnUrl');

    if (returnUrl) {
        localStorage.removeItem('returnUrl');
        window.location.href = returnUrl;
    } else {
        // 결제 완료 후 or 첫 진입 → 메뉴 화면으로
        window.location.href = `/kiosk/headcount`;
    }
});