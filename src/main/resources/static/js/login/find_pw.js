lucide.createIcons();  // Lucide 아이콘 라이브러리를 실행하여 <i> 태그를 SVG(벡터 그래픽) 아이콘 코드으로 변환

/* 상태 */
let otpSent = false;  // OTP(인증번호)가 발송된 상태인지 체크
let timerInterval = null;  // setInterval 함수를 담아두었다가 중단(clear)하기 위한 변수

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

/* 단계 이동 */
// 특정(n)의 패널만 보여주고 나머지는 숨김
function goStep(n) {
    // 모든 패널에서 'active' 클래스를 제거하여 숨김
    document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
    // 선택한 단계의 패널에 'active' 추가하여 표시
    document.getElementById('step' + n).classList.add('active');
    updateStepUI(n);  // 상단 진행 표시줄 (1, 2, 3단계) 업데이트
    clearMsg();  // 이전 단계에서 떴던 에러 메시지 초기화
}

// 상단 스텝 바의 색상을 '진행중/완료/대기' 상태에 맞게 변경
function updateStepUI(current) {
    for (let i = 1; i <= 3; i++) {
        const c = document.getElementById('circle' + i);
        const l = document.getElementById('label' + i);
        // i가 현재 단계보다 작으면 'done'(초록), 현재면 'active'(빨강), 크면 기본
        c.className = 'step-circle' + (i < current ? ' done' : i === current ? ' active' : '');
        l.className = 'step-label' + (i < current ? ' done' : i === current ? ' active' : '');
    }
    // 단계 사이의 연결 선(Line) 상태 업데이트
    for (let i = 1; i <= 2; i++) {
        document.getElementById('line' + i).className =
            'step-line' + (i < current ? ' done' : '');
    }
}

/* 메시지 */
// 전역 상단 메시지 표시 (성공/실패 알림)
function showMsg(text, type) {
    const el = document.getElementById('globalMsg');
    el.textContent = text;
    el.className = type;
    el.style.display = 'block';
}

function clearMsg() {
    const el = document.getElementById('globalMsg');
    el.style.display = 'none';
    el.textContent = '';
}

// 특정 입력창 하단에 빨간색 에러 문구 표시
function showFieldError(el, msg) {
    el.textContent = msg;
    el.style.display = 'block';
}

function hideFieldError(el) {
    el.style.display = 'none';
    el.textContent = '';
}

/* 타이머 (3분 — OtpStore 유효시간과 동기화) */
function startTimer() {
    clearInterval(timerInterval);  // 기존에 돌고 있던 타이머가 있다면 중단 (중복 방지)
    let remaining = 180;  // 3분 설정
    authTimer.style.display = 'block';
    authTimer.classList.remove('expiring');  // 깜빡임 효과 초기화
    updateTimer(remaining);

    timerInterval = setInterval(() => {
        remaining--;
        updateTimer(remaining);
        if (remaining <= 60) authTimer.classList.add('expiring');  // 1분 남으면 빨간쌕 깜빡임
        if (remaining <= 0) {
            clearInterval(timerInterval);  // 시간 종료 시 정지
            // 시간 초과 시 입력란 초기화 및 사용자 알림
            authTimer.style.display = 'none';
            otpGroup.style.display = 'none';
            inputOtp.value = '';
            otpSent = false;
            inputEmail.readOnly = false;
            sendOtpBtn.textContent = '인증 요청';
            sendOtpBtn.disabled = false;
            showMsg('인증 시간이 만료되었습니다. 다시 인증번호를 요청해 주세요.', 'error');
        }
    }, 1000);  // 1초마다 실행
}

function updateTimer(sec) {
    const m = String(Math.floor(sec / 60)).padStart(2, '0');  // '분' 계산
    const s = String(sec % 60).padStart(2, '0');  // '초' 계산
    authTimeLeft.textContent = `${m}:${s}`;
}

function stopTimer() {
    clearInterval(timerInterval);
    authTimer.style.display = 'none';
}

/* AJAX / Fetch */
// POST
async function post(url, params) {
    const res = await fetch(url, {
        method: 'POST',
        // Spring의 @RequestParam이나 @ModelAttribute로 받기 위해 폼 데이터 형식으로 전송
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: new URLSearchParams(params)
    });
    const text = await res.text();  // 서버 응답을 텍스트(String)로 변환
    return {ok: res.ok, status: res.status, text};
}

/* STEP 1: 아이디 존재 여부 확인 */
async function submitStep1() {
    hideFieldError(idError);  // 기존 에러 문구 숨김
    clearMsg();  // 전역 메시지 초기화

    const id = inputId.value.trim();
    if (!id) {
        showFieldError(idError, '아이디를 입력해 주세요.');
        return;
    }

    step1Btn.disabled = true;  // 서버 응답 전까지 버튼 비활성화 (더블 클릭 방지)
    step1Btn.textContent = '확인 중...';

    try {
        // POST /forgot-password/verify-id
        // 서버에 아이디 확인 요청
        const {ok, status, text} = await post('/forgot-password/verify-id', {loginId: id});

        if (ok) {
            confirmedId.value = id;  // 다음 단계에서 사용할 수 있게 아이디 복사
            goStep(2);  // 2단계 이동
            inputEmail.focus();
        } else if (status === 403) {
            // 예외 케이스: 비활성화된 계정일 때
            showMsg(text, 'error');  // 서버에서 온 "비활성화된 계정입니다" 메시지 출력
            showFieldError(idError, '이 계정은 현재 사용이 제한되어 있습니다.');
        } else {
            // 404: 존재하지 않는 아이디
            showMsg(text, 'error');
        }
    } catch (error) {
        showMsg('네트워크 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.', 'error');
    } finally {
        step1Btn.disabled = false;
        step1Btn.textContent = '다음';
    }
}

/* STEP 2-a: 이메일로 OTP 발송 */
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
        // POST /forgot-password/send-otp
        const {ok, status, text} = await post('/forgot-password/send-otp', {email});

        if (ok) {
            showMsg(text, 'success');
            inputEmail.readOnly = true;  // 발송 성공 시 이메일 수정 불가하게 고정
            otpGroup.style.display = 'block';  // 인증번호 입력창 등장
            inputOtp.focus();
            otpSent = true;
            startTimer();  // 3분 타이머 시작
            sendOtpBtn.textContent = '재발송';
            sendOtpBtn.disabled = false;
        } else {
            showMsg(text, 'error');
            sendOtpBtn.textContent = '인증 요청';
            sendOtpBtn.disabled = false;
            // 세션 만료(401) → 처음부터
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

/* STEP 2-b: OTP 검증 */
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
        // POST /forgot-password/verify-otp
        const {ok, status, text} = await post('/forgot-password/verify-otp', {
            email: inputEmail.value.trim(),
            otp
        });

        if (ok) {
            stopTimer();
            goStep(3);  // 최종 성공 페이지 이동
        } else {
            showMsg(text, 'error');
            verifyOtpBtn.disabled = false;
            verifyOtpBtn.textContent = '확인';
            // 세션 만료(401) → 처음부터
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

/* 이벤트 바인딩 */
// click
step1Btn.addEventListener('click', submitStep1);
sendOtpBtn.addEventListener('click', sendOtp);
verifyOtpBtn.addEventListener('click', verifyOtp);

document.getElementById('backToLoginBtn').addEventListener('click', () => {
    window.location.href = '/common/login';
});
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
document.getElementById('goLoginBtn').addEventListener('click', () => {
    window.location.href = '/common/login';
});

// Enter 키를 눌렀을 때도 버튼 클릭과 동일하게 작동하도록 설정
inputId.addEventListener('keydown', e => {
    if (e.key === 'Enter') submitStep1();
});
inputEmail.addEventListener('keydown', e => {
    if (e.key === 'Enter') sendOtp();
});
inputOtp.addEventListener('keydown', e => {
    if (e.key === 'Enter') verifyOtp();
});

// 인증번호 칸에 OTP 숫자만 입력되도록 실시간 필터링
inputOtp.addEventListener('input', function () {
    this.value = this.value.replace(/[^0-9]/g, '');
});

// 사용자가 이메일 수정하면 기존에 보냈던 OTP 상태 초기화
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