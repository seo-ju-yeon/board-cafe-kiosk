const tableBody = document.getElementById('staffTableBody');
let isIdValid = false;
let idCheckTimeout = null;

/** [모달 제어] */
function openStaffModal() {
    document.getElementById('staffModal').style.display = 'flex';
    document.body.style.overflow = 'hidden';
}

function closeStaffModal() {
    document.getElementById('staffModal').style.display = 'none';
    document.body.style.overflow = 'auto';
    document.getElementById('staffForm').reset();
    document.getElementById('idStatusMsg').textContent = "";
    isIdValid = false;
}

/** [실시간 아이디 중복 체크] */
document.getElementById('staffId').addEventListener('input', function() {
    const loginId = this.value.trim();
    const msgElement = document.getElementById('idStatusMsg');

    clearTimeout(idCheckTimeout);
    isIdValid = false;

    if (loginId.length < 3) {
        msgElement.textContent = "아이디는 3자 이상 입력해주세요.";
        msgElement.style.color = "#8E8E93";
        return;
    }

    msgElement.textContent = "확인 중...";
    msgElement.style.color = "#007AFF";

    idCheckTimeout = setTimeout(() => {
        fetch(`/admin/staff/check-id?loginId=${loginId}`)
            .then(res => res.json())
            .then(isDuplicate => { // 변수명을 이해하기 쉽게 isDuplicate로 생각하세요!
                if (!isDuplicate) { // 중복이 아니라면 (false라면)
                    msgElement.textContent = "사용 가능한 아이디입니다. ✅";
                    msgElement.style.color = "#34C759";
                    isIdValid = true;
                } else { // 중복이라면 (true라면)
                    msgElement.textContent = "이미 사용 중인 아이디입니다. ❌";
                    msgElement.style.color = "#FF3B30";
                    isIdValid = false;
                }
            })
            .catch(() => {
                msgElement.textContent = "서버 통신 오류";
                msgElement.style.color = "#FF3B30";
            });
    }, 350);
});

/** [직원 등록 저장 - ManagerRequest 필드와 매핑] */
function saveStaff() {
    const name = document.getElementById('staffName').value.trim();
    const loginId = document.getElementById('staffId').value.trim();
    const email = document.getElementById('staffEmail').value.trim(); // 이메일 값 추출
    const role = document.getElementById('staffRole').value;
    const password = document.getElementById('tempPw').value;

    if (!isIdValid) {
        alert("사용 가능한 아이디를 확인해주세요.");
        return;
    }

    // 이메일 유효성 검사
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email || !emailRegex.test(email)) {
        alert("유효한 이메일 주소를 입력해주세요.");
        return;
    }

    if (!name || !password) {
        alert("모든 정보를 입력해주세요.");
        return;
    }

    // DTO 구조와 일치하도록 데이터 구성
    const staffData = {
        name: name,
        loginId: loginId,
        email: email,
        role: role,
        password: password
    };

    fetch('/admin/staff', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(staffData)
    })
        .then(res => {
            if (res.ok) {
                alert("성공적으로 등록되었습니다.");
                location.reload();
            } else {
                res.text().then(msg => alert(msg || "등록에 실패했습니다."));
            }
        })
        .catch(err => {
            console.error(err);
            alert("서버 통신 중 오류가 발생했습니다.");
        });
}

/** [상태 토글] */
// toggleStatus 함수에서 reload 후 filter 유지
function toggleStatus(btn, id) {
    const row = btn.closest('tr');
    const isCurrentlyActive = !row.classList.contains('is-disabled');
    const targetActiveState = !isCurrentlyActive;
    const currentFilter = '[[${filter}]]';

    if (!confirm(targetActiveState ? "활성화하시겠습니까?" : "비활성화하시겠습니까?")) return;

    const params = new URLSearchParams();
    params.append('id', id);
    params.append('active', targetActiveState);

    fetch('/admin/staff/toggle-status', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params
    })
        .then(res => {
            if (res.ok) location.href = `/admin/staff?page=1&filter=${currentFilter}`;
            else alert("변경 실패");
        });
}

/** [유틸리티: 필터, 정렬, 카운트] */
// 탭 클릭 시 서버로 filter 파라미터 전달
function filterTab(filter) {
    location.href = `/admin/staff?page=1&filter=${filter}`;
}

function updateCounts() {
    const rows = tableBody.querySelectorAll('tr');
    const total = rows.length;
    const inactive = tableBody.querySelectorAll('tr.is-disabled').length;
    document.getElementById('count-all').textContent = total;
    document.getElementById('count-active').textContent = total - inactive;
    document.getElementById('count-inactive').textContent = inactive;
}

window.onload = () => {
    // 초기 정렬 (비활성 아래로)
    const rows = Array.from(tableBody.querySelectorAll('tr'));
    rows.sort((a, b) => a.classList.contains('is-disabled') - b.classList.contains('is-disabled'));
    rows.forEach(row => tableBody.appendChild(row));
    // updateCounts()는 서버에서 이미 정확한 카운트를 내려주므로 호출 불필요
};