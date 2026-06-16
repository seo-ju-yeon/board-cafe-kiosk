lucide.createIcons();

async function checkTableStatus() {
    try {
        const res = await fetch('/kiosk/table/status', {
            headers: { 'Accept': 'application/json' },
            credentials: 'same-origin'
        });
        if (!res.ok) return;

        const contentType = res.headers.get('content-type') || '';
        if (!contentType.includes('application/json')) {
            // 인증 만료/리다이렉트 등으로 HTML이 내려온 경우 JSON 파싱 시도하지 않음
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

checkTableStatus();
setInterval(checkTableStatus, 3000);