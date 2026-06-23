/* 결제 재시도 */
function retryPayment() {
    window.location.href = retryUrl || `/kiosk/checkout?tableNumber=${tableNumber}`;
}

/* 메뉴 화면 이동 */
function goMenu() {
    window.location.href = backUrl || `/kiosk/drinks?tableNumber=${tableNumber}`;
}
