/* 프로필 수정 요청 */
function updateProfile() {
    const name = document.getElementById('userName').value.trim();
    const password = document.getElementById('newPassword').value;
    const confirmPw = document.getElementById('confirmPassword').value;

    if (!name) {
        alert("이름은 필수 입력 항목입니다.");
        return;
    }

    // 비밀번호 변경 요청이 있을 때만 검증
    if (password || confirmPw) {
        if (password.length < 4) {
            alert("비밀번호는 4자 이상이어야 합니다.");
            return;
        }
        if (password !== confirmPw) {
            alert("비밀번호가 일치하지 않습니다.");
            return;
        }
    }

    document.getElementById('otpSection').style.display = 'block';
    document.getElementById('initialSaveBtn').style.display = 'none';

    alert("등록된 이메일로 인증번호를 발송합니다.");
    sendOtp();
}

/* OTP 발송 */
function sendOtp() {
    fetch('/admin/staff/profile/send-otp', { method: 'POST' })
        .then(res => {
            if (!res.ok) {
                alert("메일 발송에 실패했습니다.");
            } else {
                alert("인증번호가 재발송되었습니다.");
            }
        })
        .catch(err => {
            console.error(err);
            alert("통신 오류가 발생했습니다.");
        });
}

/* OTP 인증 후 저장 */
function submitWithOtp() {
    const otp = document.getElementById('otpInput').value.trim();
    if (otp.length !== 6) {
        alert("인증번호 6자리를 입력하세요.");
        return;
    }

    const updateData = {
        name: document.getElementById('userName').value.trim(),
        password: document.getElementById('newPassword').value,
        otp: otp
    };

    fetch('/admin/staff/profile/update', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(updateData)
    })
        .then(res => {
            return res.text().then(msg => {
                if (res.ok) {
                    alert("성공적으로 수정되었습니다.");
                    location.reload();
                } else {
                    alert(msg || "수정 중 오류가 발생했습니다.");
                }
            });
        })
        .catch(err => {
            console.error(err);
            alert("통신 오류가 발생했습니다.");
        });
}
