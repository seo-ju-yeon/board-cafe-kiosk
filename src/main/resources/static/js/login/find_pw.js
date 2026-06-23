/* Lucide 아이콘 초기화 */
lucide.createIcons();

/* 상태 */
let otpSent = false;  // OTP 발송 여부
let timerInterval = null;  // 인증 타이머 제어용 interval ID

/* DOM */
const inputId = document.getElementById('inputId');
const idError = document.getElementById('idError');
const step1Btn = document.getElementById('step1Btn');

const confirmedId = document.getElementById('confirmedId');
const inputEmail = document.getElementById('inputEmail');
const emailError = document.getElementById('emailError');
const sendOtpBtn = document.getElementById('sendOtpBtn');
const authTimer = document.getElementById('authTimer');
const authTimeLeft = document.getElementById('authTimeLeft');
const otpGroup = document.getElementById('otpGroup');
const inputOtp = document.getElementById('inputOtp');
const otpError = document.getElementById('otpError');
const verifyOtpBtn = document.getElementById('verifyOtpBtn');

/* 비밀번호 찾기 단계 이동 */
function goStep(n) {
    document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
    document.getElementById('step' + n).classList.add('active');
    updateStepUI(n);
    clearMsg();
}

/* 상단 단계 표시 UI 갱신 */
function updateStepUI(current) {
    for (let i = 1; i <= 3; i++) {
        const c = document.getElementById('circle' + i);
        const l = document.getElementById('label' + i);
        c.className = 'step-circle' + (i < current ? ' done' : i === current ? ' active' : '');
        l.className = 'step-label' + (i < current ? ' done' : i === current ? ' active' : '');
    }

    for (let i = 1; i <= 2; i++) {
        document.getElementById('line' + i).className =
            'step-line' + (i < current ? ' done' : '');
    }
}

/* 전역 메시지 표시 */
function showMsg(text, type) {
    const el = document.getElementById('globalMsg');
    el.textContent = text;
    el.className = type;
    el.style.display = 'block';
}

/* 전역 메시지 숨김 */
function clearMsg() {
    const el = document.getElementById('globalMsg');
    el.style.display = 'none';
    el.textContent = '';
}

/* 입력 필드 에러 표시 */
function showFieldError(el, msg) {
    el.textContent = msg;
    el.style.display = 'block';
}

/* 입력 필드 에러 숨김 */
function hideFieldError(el) {
    el.style.display = 'none';
    el.textContent = '';
}

/* OTP 인증 타이머 시작 */
function startTimer() {
    clearInterval(timerInterval);
    let remaining = 180;  // OtpStore 유효시간 3분과 동기화
    authTimer.style.display = 'block';
    authTimer.classList.remove('expiring');
    updateTimer(remaining);

    timerInterval = setInterval(() => {
        remaining--;
        updateTimer(remaining);
        if (remaining <= 60) authTimer.classList.add('expiring');
        if (remaining <= 0) {
            clearInterval(timerInterval);
            authTimer.style.display = 'none';
            otpGroup.style.display = 'none';
            inputOtp.value = '';
            otpSent = false;
            inputEmail.readOnly = false;
            sendOtpBtn.textContent = '인증 요청';
            sendOtpBtn.disabled = false;
            showMsg('인증 시간이 만료되었습니다. 다시 인증번호를 요청해 주세요.', 'error');
        }
    }, 1000);
}

/* OTP 남은 시간 표시 갱신 */
function updateTimer(sec) {
    const m = String(Math.floor(sec / 60)).padStart(2, '0');
    const s = String(sec % 60).padStart(2, '0');
    authTimeLeft.textContent = `${m}:${s}`;
}

/* OTP 인증 타이머 중지 */
function stopTimer() {
    clearInterval(timerInterval);
    authTimer.style.display = 'none';
}

/* URL 인코딩 POST 요청 */
async function post(url, params) {
    const res = await fetch(url, {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: new URLSearchParams(params)
    });
    const text = await res.text();
    return {ok: res.ok, status: res.status, text};
}

/* STEP 1 아이디 존재 여부 확인 */
async function submitStep1() {
    hideFieldError(idError);
    clearMsg();

    const id = inputId.value.trim();
    if (!id) {
        showFieldError(idError, '아이디를 입력해 주세요.');
        return;
    }

    step1Btn.disabled = true;
    step1Btn.textContent = '확인 중...';

    try {
        // 서버에 아이디 확인 요청
        const {ok, status, text} = await post('/forgot-password/verify-id', {loginId: id});

        if (ok) {
            confirmedId.value = id;
            goStep(2);
            inputEmail.focus();
        } else if (status === 403) {
            // 비활성 계정 예외 처리
            showMsg(text, 'error');
            showFieldError(idError, '이 계정은 현재 사용이 제한되어 있습니다.');
        } else {
            showMsg(text, 'error');
        }
    } catch (error) {
        showMsg('네트워크 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.', 'error');
    } finally {
        step1Btn.disabled = false;
        step1Btn.textContent = '다음';
    }
}

/* STEP 2 이메일 OTP 발송 */
async function sendOtp() {
    hideFieldError(emailError);
    clearMsg();

    const email = inputEmail.value.trim();
    if (!email || !/^[A-Za-z0-9+_.-]+@(.+)$/.test(email)) {
        showFieldError(emailError, '올바른 이메일 형식을 입력해 주세요.');
        return;
    }

    sendOtpBtn.disabled = true;
    sendOtpBtn.textContent = '발송 중...';

    try {
        // 비밀번호 찾기용 OTP 발송 요청
        const {ok, status, text} = await post('/forgot-password/send-otp', {email});

        if (ok) {
            showMsg(text, 'success');
            inputEmail.readOnly = true;
            otpGroup.style.display = 'block';
            inputOtp.focus();
            otpSent = true;
            startTimer();
            sendOtpBtn.textContent = '재발송';
            sendOtpBtn.disabled = false;
        } else {
            showMsg(text, 'error');
            sendOtpBtn.textContent = '인증 요청';
            sendOtpBtn.disabled = false;
            if (status === 401) {
                setTimeout(() => goStep(1), 1500);
            }
        }
    } catch {
        showMsg('네트워크 오류가 발생했습니다.', 'error');
        sendOtpBtn.textContent = '인증 요청';
        sendOtpBtn.disabled = false;
    }
}

/* STEP 2 OTP 검증 */
async function verifyOtp() {
    hideFieldError(otpError);
    clearMsg();

    const otp = inputOtp.value.trim();
    if (!otp || otp.length !== 6) {
        showFieldError(otpError, '6자리 인증번호를 입력해 주세요.');
        return;
    }

    verifyOtpBtn.disabled = true;
    verifyOtpBtn.textContent = '확인 중...';

    try {
        // 입력한 OTP와 이메일 검증 요청
        const {ok, status, text} = await post('/forgot-password/verify-otp', {
            email: inputEmail.value.trim(),
            otp
        });

        if (ok) {
            stopTimer();
            goStep(3);
        } else {
            showMsg(text, 'error');
            verifyOtpBtn.disabled = false;
            verifyOtpBtn.textContent = '확인';
            if (status === 401) {
                setTimeout(() => goStep(1), 1500);
            }
        }
    } catch {
        showMsg('네트워크 오류가 발생했습니다.', 'error');
        verifyOtpBtn.disabled = false;
        verifyOtpBtn.textContent = '확인';
    }
}

/* 버튼 클릭 이벤트 바인딩 */
step1Btn.addEventListener('click', submitStep1);
sendOtpBtn.addEventListener('click', sendOtp);
verifyOtpBtn.addEventListener('click', verifyOtp);

/* 로그인 화면 이동 */
document.getElementById('backToLoginBtn').addEventListener('click', () => {
    window.location.href = '/common/login';
});

/* 비밀번호 찾기 첫 단계로 이동 */
document.getElementById('backToStep1Btn').addEventListener('click', () => {
    stopTimer();
    otpSent = false;
    inputEmail.value = '';
    inputEmail.readOnly = false;
    inputOtp.value = '';
    otpGroup.style.display = 'none';
    sendOtpBtn.textContent = '인증 요청';
    sendOtpBtn.disabled = false;
    goStep(1);
});

/* 완료 후 로그인 화면 이동 */
document.getElementById('goLoginBtn').addEventListener('click', () => {
    window.location.href = '/common/login';
});

/* Enter 키 입력 처리 */
inputId.addEventListener('keydown', e => {
    if (e.key === 'Enter') submitStep1();
});
inputEmail.addEventListener('keydown', e => {
    if (e.key === 'Enter') sendOtp();
});
inputOtp.addEventListener('keydown', e => {
    if (e.key === 'Enter') verifyOtp();
});

/* OTP 숫자 입력 필터링 */
inputOtp.addEventListener('input', function () {
    this.value = this.value.replace(/[^0-9]/g, '');
});

/* 이메일 수정 시 OTP 상태 초기화 */
inputEmail.addEventListener('input', function () {
    if (otpSent) {
        otpSent = false;
        this.readOnly = false;
        inputOtp.value = '';
        otpGroup.style.display = 'none';
        sendOtpBtn.textContent = '인증 요청';
        sendOtpBtn.disabled = false;
        stopTimer();
    }
});
