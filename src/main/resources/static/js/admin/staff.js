const tableBody = document.getElementById('staffTableBody');
let isIdValid = false;
let idCheckTimeout = null;

/* 직원 등록 모달 */
function openStaffModal() {
    document.getElementById('staffModal').style.display = 'flex';
    document.body.style.overflow = 'hidden';
}

/* 직원 등록 모달 닫기 */
function closeStaffModal() {
    document.getElementById('staffModal').style.display = 'none';
    document.body.style.overflow = 'auto';
    document.getElementById('staffForm').reset();
    document.getElementById('idStatusMsg').textContent = "";
    isIdValid = false;
}

/* 아이디 중복 확인 */
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
            .then(isDuplicate => {
                if (!isDuplicate) {
                    msgElement.textContent = "사용 가능한 아이디입니다.";
                    msgElement.style.color = "#34C759";
                    isIdValid = true;
                } else {
                    msgElement.textContent = "이미 사용 중인 아이디입니다.";
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

/* 직원 등록 */
function saveStaff() {
    const name = document.getElementById('staffName').value.trim();
    const loginId = document.getElementById('staffId').value.trim();
    const email = document.getElementById('staffEmail').value.trim();
    const role = document.getElementById('staffRole').value;
    const password = document.getElementById('tempPw').value;

    if (!isIdValid) {
        alert("사용 가능한 아이디를 확인해주세요.");
        return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email || !emailRegex.test(email)) {
        alert("유효한 이메일 주소를 입력해주세요.");
        return;
    }

    if (!name || !password) {
        alert("모든 정보를 입력해주세요.");
        return;
    }

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

/* 직원 상태 변경 */
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

/* 직원 필터 */
function filterTab(filter) {
    location.href = `/admin/staff?page=1&filter=${filter}`;
}

/* 직원 수 갱신 */
function updateCounts() {
    const rows = tableBody.querySelectorAll('tr');
    const total = rows.length;
    const inactive = tableBody.querySelectorAll('tr.is-disabled').length;
    document.getElementById('count-all').textContent = total;
    document.getElementById('count-active').textContent = total - inactive;
    document.getElementById('count-inactive').textContent = inactive;
}

/* 초기 정렬 */
window.onload = () => {
    const rows = Array.from(tableBody.querySelectorAll('tr'));
    rows.sort((a, b) => a.classList.contains('is-disabled') - b.classList.contains('is-disabled'));
    rows.forEach(row => tableBody.appendChild(row));
};
