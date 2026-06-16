const emailInput = document.getElementById('email');
const submitBtn = document.getElementById('submitBtn');
const cancelBtn = document.getElementById('cancelBtn');
const emailForm = document.getElementById('emailForm');
const globalError = document.getElementById('globalError');

// 페이지 로드 시 정보 출력
window.addEventListener('load', function () {
    // console.log('=== 2차 인증 페이지(HTML 버전) 로드 완료 ===');  -> 디버깅용

    // URL 파라미터로 에러 감지
    // (예: ?error=email)을 분석하기 위한 객체 생성
    const params = new URLSearchParams(window.location.search);

    // 주소창에 error=email이라는 값이 있다면 (서버에서 인증 실패로 리다이렉트 시킨 경우)
    if (params.get('error') === 'email') {
        globalError.textContent = '등록된 이메일 주소와 일치하지 않습니다. 다시 확인해 주세요.';
        globalError.style.display = 'block';  // 숨김되어있던 에러 박스 띄움
        emailInput.classList.add('error');  // 입력창 테두리를 빨간색으로 변경
        emailInput.focus();  // 바로 입력할 수 있게 커서 배치
    }
});

// 이메일 유효성 검사 (입력란에서 포커스가 빠질 때)
emailInput.addEventListener('blur', function () {
    validateEmail();
});

function validateEmail() {
    const value = emailInput.value.trim();  // 앞뒤 공백 제거
    const emailPattern = /^[A-Za-z0-9+_.-]+@(.+)$/;  // 이메일 형식 체크 정규표현식
    const errorDiv = document.getElementById('emailError');

    // 경우 1: 아무것도 입력하지 않았을 때
    if (value.length === 0) {
        emailInput.classList.add('error');
        errorDiv.textContent = '이메일을 입력해주세요.';
        errorDiv.style.display = 'block';
        return false;
    }
    // 경우 2: 이메일 형식이 아닐 때
    else if (!emailPattern.test(value)) {
        emailInput.classList.add('error');
        errorDiv.textContent = '올바른 이메일 형식이 아닙니다.';
        errorDiv.style.display = 'block';
        return false;
    }
    // 경우 3: 정상 입력 시
    else {
        emailInput.classList.remove('error');  // 빨간 테두리 제거
        errorDiv.style.display = 'none';  // 에러 메시지 숨김
        return true;
    }
}

// 사용자가 '확인' 버튼을 누르거나 Enter를 쳤을 때
emailForm.addEventListener('submit', function (e) {
    // 유효성 검사를 통과하지 못하였을 때,
    if (!validateEmail()) {
        e.preventDefault();  // 서버로 폼 데이터가 전송 되는 것을 막음
        emailInput.focus();
        return false;
    }

    // 중복 클릭 방지
    // 서버 응답이 오기 전에 사용자가 버튼을 여러 번 누르는 것을 막기 위해 비활성화
    submitBtn.disabled = true;
    submitBtn.textContent = '확인 중...';

    console.log('폼 제출 진행: ', emailInput.value);
    // 이후 HTML에 정의된 <form action="/login/verifyEmail" method="post"> 경로로 데이터 전송
});

// 취소 버튼 (관리자 로그인 페이지로 이동)
cancelBtn.addEventListener('click', function () {
    // 사용자에게 한 번 더 의사를 물어보는 팝업창
    if (confirm('로그인을 취소하시겠습니까?')) {
        // 확인을 누르면 지정된 경로(로그인 페이지)로 이동
        window.location.href = '/admin/login';
    }
});