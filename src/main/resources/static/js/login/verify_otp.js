/* DOM 참조 */
const emailInput = document.getElementById('email');
const emailError = document.getElementById('emailError');
const sendOtpBtn = document.getElementById('sendOtpBtn');
const otpSection = document.getElementById('otpSection');
const otpInput = document.getElementById('otp');
const otpError = document.getElementById('otpError');
const timerEl = document.getElementById('timer');
const timeLeftEl = document.getElementById('timeLeft');
const submitBtn = document.getElementById('submitBtn');
const cancelBtn = document.getElementById('cancelBtn');
const statusMsg = document.getElementById('statusMsg');

let timerInterval = null;  // 인증 타이머 제어용 interval ID
let otpSent = false;  // OTP 발송 여부

/* 인증 상태 메시지 표시 */
function showStatus(msg, type) {
    statusMsg.textContent = msg;
    statusMsg.className = type;
    statusMsg.style.display = 'block';
}

/* 인증 상태 메시지 숨김 */
function hideStatus() {
    statusMsg.style.display = 'none';
}

/* 이메일 형식 검증 */
function validateEmail() {
    const v = emailInput.value.trim();
    const ok = v.length > 0 && /^[A-Za-z0-9+_.-]+@(.+)$/.test(v);
    emailInput.classList.toggle('input-error', !ok);
    emailError.style.display = ok ? 'none' : 'block';
    return ok;
}

/* OTP 인증 타이머 시작 */
function startTimer() {
    clearInterval(timerInterval);
    let remaining = 180;  // OtpStore 유효시간 3분과 동기화
    timerEl.style.display = 'block';
    updateTimer(remaining);

    timerInterval = setInterval(() => {
        remaining--;
        updateTimer(remaining);

        if (remaining <= 0) {
            clearInterval(timerInterval);
            showStatus('인증 시간이 만료되었습니다. 인증번호를 다시 요청해 주세요.', 'error');
            resetOtpSection();
        }
    }, 1000);
}

/* OTP 남은 시간 표시 갱신 */
function updateTimer(sec) {
    const m = String(Math.floor(sec / 60)).padStart(2, '0');
    const s = String(sec % 60).padStart(2, '0');
    timeLeftEl.textContent = `${m}:${s}`;
}

/* OTP 입력 영역 초기화 */
function resetOtpSection() {
    clearInterval(timerInterval);
    otpInput.value = '';
    otpSent = false;
    emailInput.readOnly = false;
    timerEl.style.display = 'none';
    otpSection.style.display = 'none';
    sendOtpBtn.textContent = '인증 요청';
    sendOtpBtn.disabled = false;
}

/* 로그인 OTP 발송 요청 */
sendOtpBtn.addEventListener('click', async () => {
    if (!validateEmail()) {
        emailInput.focus();
        return;
    }

    sendOtpBtn.disabled = true;
    sendOtpBtn.textContent = '발송 중...';
    hideStatus();

    try {
        // 로그인 2차 인증용 OTP 발송 요청
        const res = await fetch('/login/sendOtp', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: new URLSearchParams({email: emailInput.value.trim()})
        });
        const text = await res.text();

        if (res.ok) {
            showStatus(text, 'success');
            emailInput.readOnly = true;
            otpSection.style.display = 'block';
            otpInput.focus();
            otpSent = true;
            startTimer();
            sendOtpBtn.textContent = '재발송';
            sendOtpBtn.disabled = false;
        } else {
            showStatus(text, 'error');
            sendOtpBtn.textContent = '인증 요청';
            sendOtpBtn.disabled = false;

            if (res.status === 401) {
                setTimeout(() => {
                    window.location.href = '/common/login';
                }, 1500);
            }
        }
    } catch {
        showStatus('네트워크 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.', 'error');
        sendOtpBtn.textContent = '인증 요청';
        sendOtpBtn.disabled = false;
    }
});

/* OTP 숫자 입력 필터링 */
otpInput.addEventListener('input', function () {
    this.value = this.value.replace(/[^0-9]/g, '');
    if (this.value.length > 0) otpError.style.display = 'none';
});

/* OTP 검증 후 로그인 완료 */
submitBtn.addEventListener('click', async () => {
    if (!otpSent) {
        showStatus('먼저 이메일 인증을 요청해 주세요.', 'error');
        return;
    }
    if (otpInput.value.length !== 6) {
        otpError.style.display = 'block';
        otpInput.focus();
        return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = '로그인 중...';
    hideStatus();

    try {
        // 입력한 이메일과 OTP 검증 요청
        const res = await fetch('/login/verifyEmailOtp', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: new URLSearchParams({
                email: emailInput.value.trim(),
                otp: otpInput.value.trim()
            })
        });
        const text = await res.text();

        if (res.ok) {
            window.location.href = text;
        } else {
            showStatus(text, 'error');
            submitBtn.disabled = false;
            submitBtn.textContent = '로그인';

            if (res.status === 401) {
                setTimeout(() => {
                    window.location.href = '/common/login';
                }, 1500);
            }
        }
    } catch {
        showStatus('네트워크 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.', 'error');
        submitBtn.disabled = false;
        submitBtn.textContent = '로그인';
    }
});

/* 로그인 OTP 인증 취소 */
cancelBtn.addEventListener('click', () => {
    if (confirm('로그인을 취소하시겠습니까?')) {
        clearInterval(timerInterval);
        window.location.href = '/common/login';
    }
});

/* 이메일 입력 포커스 해제 시 검증 */
emailInput.addEventListener('blur', validateEmail);
