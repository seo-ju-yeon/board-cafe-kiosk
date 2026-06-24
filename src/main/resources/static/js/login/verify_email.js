/* Lucide 아이콘 초기화 */
lucide.createIcons();

/* DOM 참조 */
const emailInput = document.getElementById('email');
const submitBtn = document.getElementById('submitBtn');
const cancelBtn = document.getElementById('cancelBtn');
const emailForm = document.getElementById('emailForm');
const globalError = document.getElementById('globalError');

/* 이메일 인증 페이지 초기화 */
window.addEventListener('load', function () {
    const params = new URLSearchParams(window.location.search);

    // 서버 인증 실패 후 리다이렉트된 경우 에러 표시
    if (params.get('error') === 'email') {
        globalError.textContent = '등록된 이메일 주소와 일치하지 않습니다. 다시 확인해 주세요.';
        globalError.style.display = 'block';
        emailInput.classList.add('error');
        emailInput.focus();
    }
});

/* 이메일 입력 포커스 해제 시 검증 */
emailInput.addEventListener('blur', function () {
    validateEmail();
});

/* 이메일 형식 검증 */
function validateEmail() {
    const value = emailInput.value.trim();
    const emailPattern = /^[A-Za-z0-9+_.-]+@(.+)$/;
    const errorDiv = document.getElementById('emailError');

    if (value.length === 0) {
        emailInput.classList.add('error');
        errorDiv.textContent = '이메일을 입력해주세요.';
        errorDiv.style.display = 'block';
        return false;
    }
    else if (!emailPattern.test(value)) {
        emailInput.classList.add('error');
        errorDiv.textContent = '올바른 이메일 형식이 아닙니다.';
        errorDiv.style.display = 'block';
        return false;
    }
    else {
        emailInput.classList.remove('error');
        errorDiv.style.display = 'none';
        return true;
    }
}

/* 이메일 인증 폼 제출 */
emailForm.addEventListener('submit', function (e) {
    if (!validateEmail()) {
        e.preventDefault();
        emailInput.focus();
        return false;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = '확인 중...';
});

/* 이메일 인증 취소 */
cancelBtn.addEventListener('click', function () {
    if (confirm('로그인을 취소하시겠습니까?')) {
        window.location.href = '/admin/login';
    }
});
