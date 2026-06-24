lucide.createIcons();

/* 추천 테이블 정보 입력 */
function quickSetup(num, pw) {
    document.getElementById('tableNumber').value = num;
    document.getElementById('password').value = pw;
    document.getElementById('tableNumber').focus();
}

/* 선택한 테이블 정보 입력 */
function handleQuickSelect(select) {
    if (select.value) {
        const [num, pw] = select.value.split('|');
        quickSetup(num, pw);
    }
}
