lucide.createIcons();

/* 시연 계정 정보 입력 */
function quickLogin(id, pw) {
    document.getElementById('username').value = id;
    document.getElementById('password').value = pw;
    document.getElementById('username').focus();
}
