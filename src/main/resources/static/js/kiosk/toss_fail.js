function retryPayment() {
    window.location.href = retryUrl || `/kiosk/checkout?tableNumber=${tableNumber}`;
}

function goMenu() {
    window.location.href = backUrl || `/kiosk/drinks?tableNumber=${tableNumber}`;
}
