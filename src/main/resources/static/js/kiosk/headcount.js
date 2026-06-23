/* Lucide 아이콘 초기화 */
lucide.createIcons();

/* 선택 인원 상태 */
let selectedCount = 0;

/* 이용 인원 선택 */
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

/* 선택 인원 제출 */
function goToNext() {
    if (selectedCount > 0) {
        document.getElementById('partySize-input').value = selectedCount;
        document.getElementById('headcount-form').submit();
    }
}

/* 스크린세이버 이동 처리 */
let idleTimer;

/* 사용자 미입력 시간 초기화 */
function resetIdleTimer() {
    clearTimeout(idleTimer);
    // 30초 동안 활동이 없을 때 스크린세이버로 이동
    idleTimer = setTimeout(() => {
        localStorage.setItem('returnUrl', window.location.pathname);
        window.location.href = '/kiosk/screensaver';
    }, 30000);
}

/* 사용자 활동 감지 */
window.onload = resetIdleTimer;
window.onmousemove = resetIdleTimer;
window.onclick = resetIdleTimer;
window.onkeydown = resetIdleTimer;
window.ontouchstart = resetIdleTimer;
