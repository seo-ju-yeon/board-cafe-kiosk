lucide.createIcons();

let selectedCount = 0;

function selectPeople(count, element) {
    const cards = document.querySelectorAll('.number-card');
    cards.forEach(card => card.classList.remove('selected'));

    element.classList.add('selected');
    selectedCount = count;

    const nextBtn = document.getElementById('next-button');
    nextBtn.classList.add('active');
    nextBtn.innerText = `${count}명 선택 완료 (다음으로)`;
    nextBtn.style.cursor = 'pointer';
}

function goToNext() {
    if (selectedCount > 0) {
        document.getElementById('partySize-input').value = selectedCount;
        document.getElementById('headcount-form').submit();
    }
}

/* ================= 스크린세이버 로직 추가 ================= */
let idleTimer;

function resetIdleTimer() {
    clearTimeout(idleTimer);
    // 30초 동안 활동이 없으면 screensaver로 이동
    idleTimer = setTimeout(() => {
        localStorage.setItem('returnUrl', window.location.pathname);
        window.location.href = '/kiosk/screensaver';
    }, 30000);
}

// 활동 감지 이벤트 리스너 등록
window.onload = resetIdleTimer;
window.onmousemove = resetIdleTimer;
window.onclick = resetIdleTimer;
window.onkeydown = resetIdleTimer;
window.ontouchstart = resetIdleTimer;
/* ======================================================= */