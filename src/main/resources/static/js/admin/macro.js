/*
         * [핵심] Thymeleaf가 서버 사이드에서 권한을 평가하여 JS 변수로 주입
         * renderTable()에서 동적으로 버튼을 생성할 때 이 변수를 참조
         */
// const IS_ADMIN_OR_SUPER = /*[[${#authorization.expression('hasAnyRole(''ADMIN'', ''SUPER'')')}]]*/ false;
const IS_ADMIN_OR_SUPER = document.getElementById('isAdminOrSuper') !== null;

let currentDirection = 'STAFF_TO_TABLE'; // 기본값을 고정

function openTab(evt) {
    const btn = evt.currentTarget;
    currentDirection = btn.getAttribute('data-dir');
    const tabId = btn.getAttribute('data-tid');

    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');

    document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
    document.getElementById(tabId).style.display = 'block';

    /*
     * [수정] btnAddMacro는 ADMIN/SUPER에게만 렌더링되므로 null 체크 필수
     * STAFF에게는 btnAddMacroDisabled가 렌더링됨
     */
    // STAFF는 btnAddMacro가 업으므로 null 체크
    const addBtn = document.getElementById('btnAddMacro');
    if (addBtn) addBtn.style.display = 'block';

    loadMacroList(currentDirection, 1);
}

function loadMacroList(direction, page) {
    fetch(`/admin/macro/list?direction=${direction}&page=${page}&size=10`)
        .then(res => res.json())
        .then(data => {
            renderTable(data.dtoList, direction);
            renderPagination(data, direction);
        });
}

function renderTable(list, direction) {
    const tbody = document.getElementById('tbody-' + direction);
    if (!tbody) return;

    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="2" style="text-align:center; padding:40px; color:#aaa;">등록된 메시지가 없습니다.</td></tr>';
        return;
    }

    tbody.innerHTML = list.map(item => {
        // [핵심] 서버 데이터에서 ID 역할을 하는 값을 강제로 찾습니다.
        // item.id가 없으면 item.mno, 그것도 없으면 item.macroId를 순차적으로 대입
        const valId = item.id || item.mno || item.macroId || (item.dto && item.dto.id);

        /*
         * [핵심 수정] IS_ADMIN_OR_SUPER 변수로 삭제 버튼 분기
         * renderTable()이 동적으로 DOM을 덮어써도 권한 체크가 유지됨
         */
        // IS_ADMIN_OR_SUPER로 동적 렌더링 시에도 권한 분기
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

function renderPagination(data, direction) {
    const container = document.getElementById('macroPagination');
    let html = '';
    for (let i = data.start; i <= data.end; i++) {
        html += `<button class="page-btn ${data.page === i ? 'active' : ''}" onclick="loadMacroList('${direction}', ${i})">${i}</button>`;
    }
    container.innerHTML = html;
}

function deleteMacro(btn) {
    // 1. data-id 값을 가져옴
    let id = btn.getAttribute('data-id');

    // 2. 검증 (undefined 문자열 방어)
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

function openAddModal() {
    document.getElementById('addMacroModal').style.display = 'flex';
}

function closeAddModal() {
    document.getElementById('addMacroModal').style.display = 'none';
}

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

document.addEventListener('DOMContentLoaded', () => {
    const firstBtn = document.querySelector('.tab-btn');
    if (firstBtn) firstBtn.click();
});