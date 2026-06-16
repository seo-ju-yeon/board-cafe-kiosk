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

let timerInterval = null;  // 타이머 멈춤/시작을 제어하기 위한 변수
let otpSent = false;   // 인증번호가 성공적으로 발송되었는지 추적하는 플래그

/* 성공(green) 또는 에러(red) 메시지를 상단에 띄워주는 함수 */
function showStatus(msg, type) {
    statusMsg.textContent = msg;
    statusMsg.className = type;  // 'success' 또는 'error' 클래스 부여
    statusMsg.style.display = 'block';
}

function hideStatus() {
    statusMsg.style.display = 'none';
}

/* 이메일 정규식 검사 및 UI 에러 표시 */
function validateEmail() {
    const v = emailInput.value.trim();
    // 이메일 형식이 맞는지 테스트
    const ok = v.length > 0 && /^[A-Za-z0-9+_.-]+@(.+)$/.test(v);
    emailInput.classList.toggle('input-error', !ok);  // 틀리면 빨간 테두리
    emailError.style.display = ok ? 'none' : 'block';  // 틀리면 에러 문구 노출
    return ok;
}

/* 타이머 (3분) */
function startTimer() {
    clearInterval(timerInterval);  // 기존에 돌던 타이머가 있다면 초기화
    let remaining = 180;  // OtpStore 유효시간(3분)과 동기화
    timerEl.style.display = 'block';
    updateTimer(remaining);

    timerInterval = setInterval(() => {
        remaining--;
        updateTimer(remaining);

        if (remaining <= 0) {  // 시간디 다 되면
            clearInterval(timerInterval);
            showStatus('인증 시간이 만료되었습니다. 인증번호를 다시 요청해 주세요.', 'error');
            resetOtpSection();  // 입력창 초기화 및 숨김
        }
    }, 1000);  // 1초마다 반복 실행
}

/* 초 단위 숫자를 '분:초' 형식으로 변환하여 화면에 출력 */
function updateTimer(sec) {
    const m = String(Math.floor(sec / 60)).padStart(2, '0');
    const s = String(sec % 60).padStart(2, '0');
    timeLeftEl.textContent = `${m}:${s}`;
}

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

/* 인증 요청 버튼 */
sendOtpBtn.addEventListener('click', async () => {
    if (!validateEmail()) {
        emailInput.focus();
        return;  // 이메일 형식이 틀리면 중단
    }

    sendOtpBtn.disabled = true;  // 중복 클릭 방지
    sendOtpBtn.textContent = '발송 중...';
    hideStatus();

    try {
        // POST /login/sendOtp — 이메일 대조 후 OTP 생성 및 발송
        const res = await fetch('/login/sendOtp', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: new URLSearchParams({email: emailInput.value.trim()})
        });
        const text = await res.text();  // 서버가 보낸 응답 메시지

        if (res.ok) {
            // 성공(200 ok) -> OTP 섹션 노출 + 타이머 시작
            showStatus(text, 'success');
            emailInput.readOnly = true;  // 이메일 수정 불가능하게 잠금
            otpSection.style.display = 'block';  // OTP 입력창 노출
            otpInput.focus();
            otpSent = true;
            startTimer();  // 타이머 시작
            sendOtpBtn.textContent = '재발송';
            sendOtpBtn.disabled = false;
        } else {
            // 실패: 400(이메일 불일치) / 401(세션 만료) / 500(메일 오류)
            showStatus(text, 'error');
            sendOtpBtn.textContent = '인증 요청';
            sendOtpBtn.disabled = false;

            // 세션 만료(401)이면 로그인 페이지로
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

/* 숫자만 입력 */
otpInput.addEventListener('input', function () {
    this.value = this.value.replace(/[^0-9]/g, '');
    if (this.value.length > 0) otpError.style.display = 'none';
});

/* 로그인 버튼 */
submitBtn.addEventListener('click', async () => {
    // 클라이언트 사전 검증
    if (!otpSent) {
        showStatus('먼저 이메일 인증을 요청해 주세요.', 'error');
        return;
    }
    if (otpInput.value.length !== 6) {  // 6자리 미입력 시 에러
        otpError.style.display = 'block';
        otpInput.focus();
        return;
    }

    submitBtn.disabled = true;  // 중복 클릭 방지
    submitBtn.textContent = '로그인 중...';
    hideStatus();

    try {
        // POST /login/verifyEmailOtp — 이메일과 입력한 OTP를 서버에 전달
        const res = await fetch('/login/verifyEmailOtp', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: new URLSearchParams({
                email: emailInput.value.trim(),
                otp: otpInput.value.trim()
            })
        });
        const text = await res.text();  // 성공 시 리다이렉트할 경로가 담겨져있음.

        if (res.ok) {
            // 서버가 내려준 경로로 이동 (현재: "/admin/dashboard")
            window.location.href = text;
        } else {
            // 실패: 오류 메시지 표시 + 버튼 복원
            showStatus(text, 'error');
            submitBtn.disabled = false;
            submitBtn.textContent = '로그인';

            // 세션 만료(401)이면 로그인 페이지로
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

/* 취소 버튼 */
cancelBtn.addEventListener('click', () => {
    if (confirm('로그인을 취소하시겠습니까?')) {
        clearInterval(timerInterval);
        window.location.href = '/common/login';
    }
});

emailInput.addEventListener('blur', validateEmail);