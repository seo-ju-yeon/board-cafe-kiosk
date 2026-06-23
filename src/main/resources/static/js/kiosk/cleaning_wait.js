/* Lucide 아이콘 초기화 */
lucide.createIcons();

/* 테이블 빈자리 상태 확인 */
async function checkTableStatus() {
    try {
        const res = await fetch('/kiosk/table/status', {
            headers: {'Accept': 'application/json'},
            credentials: 'same-origin'
        });
        if (!res.ok) return;

        const contentType = res.headers.get('content-type') || '';
        if (!contentType.includes('application/json')) {
            // 인증 만료 또는 리다이렉트 응답 처리
            if (res.redirected && res.url) {
                window.location.href = res.url;
            }
            return;
        }

        const data = await res.json();
        if (data && data.success && data.status === 'EMPTY') {
            window.location.href = '/kiosk/screensaver';
        }
    } catch (e) {
        console.error('상태 확인 실패:', e);
    }
}

/* 테이블 상태 주기 확인 */
checkTableStatus();
setInterval(checkTableStatus, 3000);
