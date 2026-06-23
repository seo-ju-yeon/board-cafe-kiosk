/* 권한 상태 */
const IS_ADMIN_OR_SUPER = document.getElementById('isAdminOrSuper') !== null;

let currentDirection = 'STAFF_TO_TABLE';

/* 탭 전환 */
function openTab(evt) {
    const btn = evt.currentTarget;
    currentDirection = btn.getAttribute('data-dir');
    const tabId = btn.getAttribute('data-tid');

    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');

    document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
    document.getElementById(tabId).style.display = 'block';

    const addBtn = document.getElementById('btnAddMacro');
    if (addBtn) addBtn.style.display = 'block';

    loadMacroList(currentDirection, 1);
}

/* 메시지 목록 조회 */
function loadMacroList(direction, page) {
    fetch(`/admin/macro/list?direction=${direction}&page=${page}&size=10`)
        .then(res => res.json())
        .then(data => {
            renderTable(data.dtoList, direction);
            renderPagination(data, direction);
        });
}

/* 메시지 목록 렌더링 */
function renderTable(list, direction) {
    const tbody = document.getElementById('tbody-' + direction);
    if (!tbody) return;

    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="2" style="text-align:center; padding:40px; color:#aaa;">등록된 메시지가 없습니다.</td></tr>';
        return;
    }

    tbody.innerHTML = list.map(item => {
        // 응답 DTO 필드명이 달라도 삭제 대상 ID를 찾을 수 있도록 보정
        const valId = item.id || item.mno || item.macroId || (item.dto && item.dto.id);

        const deleteBtn = IS_ADMIN_OR_SUPER
            ? `<button type="button" class="btn-delete"
                               data-id="${valId}"
                               onclick="deleteMacro(this)">삭제</button>`
            : `<button type="button" class="btn-delete"
                               style="opacity: 0.4; cursor: not-allowed;"
                               onclick="alert('관리자 권한이 필요합니다.'); return false;">삭제</button>`;

        return `
                    <tr class="macro-row">
                        <td class="messageText">${item.messageText || ''}</td>
                        <td style="text-align: right;">${deleteBtn}</td>
                    </tr>
                `;
    }).join('');
}

/* 페이지네이션 렌더링 */
function renderPagination(data, direction) {
    const container = document.getElementById('macroPagination');
    let html = '';
    for (let i = data.start; i <= data.end; i++) {
        html += `<button class="page-btn ${data.page === i ? 'active' : ''}" onclick="loadMacroList('${direction}', ${i})">${i}</button>`;
    }
    container.innerHTML = html;
}

/* 메시지 삭제 */
function deleteMacro(btn) {
    let id = btn.getAttribute('data-id');

    if (!id || id === 'undefined' || id === 'null') {
        alert("데이터 오류: 서버에서 ID를 보내주지 않았거나 필드명이 다릅니다.\nF12 -> Console 탭의 '데이터 원본'을 확인해주세요.");
        return;
    }

    if (!confirm("정말 삭제하시겠습니까?")) return;

    fetch(`/admin/macro/api/delete/${id}`, {
        method: 'DELETE'
    })
        .then(res => {
            if (res.ok) {
                alert("삭제되었습니다.");
                location.reload();
            } else {
                alert("삭제 실패 (코드: " + res.status + ")");
            }
        })
        .catch(err => console.error("통신 오류:", err));
}

/* 등록 모달 */
function openAddModal() {
    document.getElementById('addMacroModal').style.display = 'flex';
}

/* 등록 모달 닫기 */
function closeAddModal() {
    document.getElementById('addMacroModal').style.display = 'none';
}

/* 메시지 등록 */
function submitNewMacro() {
    const direction = document.getElementById('newDirection').value;
    const messageText = document.getElementById('newMessageText').value.trim();
    if (!messageText) return alert("내용 입력!");
    fetch('/admin/macro/api/create', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({direction, messageText})
    }).then(() => location.reload());
}

/* 초기화 */
document.addEventListener('DOMContentLoaded', () => {
    const firstBtn = document.querySelector('.tab-btn');
    if (firstBtn) firstBtn.click();
});
