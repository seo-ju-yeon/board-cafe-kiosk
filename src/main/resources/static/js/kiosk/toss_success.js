/* Lucide 아이콘 초기화 */
lucide.createIcons({attrs: {'stroke-width': 2.5}});

/* 스크린세이버 복귀 경로 초기화 */
localStorage.removeItem('returnUrl');

/* 결제 성공 후 지정 화면 이동 */
setTimeout(() => {
    window.location.href = redirectUrl;
}, 3000);
