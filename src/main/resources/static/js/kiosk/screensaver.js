/* 스크린세이버 시작 영역 참조 */
const screen = document.getElementById('tap-to-start');

/* 스크린세이버 클릭 후 이전 화면 복귀 */
screen.addEventListener('click', function() {
    const returnUrl = localStorage.getItem('returnUrl');

    if (returnUrl) {
        localStorage.removeItem('returnUrl');
        window.location.href = returnUrl;
    } else {
        // 복귀 경로가 없을 때 인원 선택 화면으로 이동
        window.location.href = `/kiosk/headcount`;
    }
});
