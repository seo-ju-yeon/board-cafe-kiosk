/* 전화번호 검색 */
function doSearch() {
    const keyword = document.getElementById('searchInput').value.trim();
    const size = 10;
    const url = keyword
        ? `/admin/points/list?page=1&size=${size}&keyword=${encodeURIComponent(keyword)}`
        : `/admin/points/list?page=1&size=${size}`;
    location.href = url;
}

/* 검색 초기화 */
function clearSearch() {
    location.href = '/admin/points/list?page=1&size=10';
}

/* 포인트 이력 모달 */
function openHistoryModal(pointId, phone) {
    document.getElementById('modalPhone').textContent = phone;
    document.getElementById('modalBody').innerHTML =
        '<div class="modal-loading">불러오는 중...</div>';
    document.getElementById('historyModal').classList.add('active');

    fetch(`/admin/points/${pointId}/history`)
        .then(res => res.json())
        .then(list => renderHistoryModal(list))
        .catch(() => {
            document.getElementById('modalBody').innerHTML =
                '<div class="modal-empty">이력을 불러오지 못했습니다.</div>';
        });
}

/* 포인트 이력 렌더링 */
function renderHistoryModal(list) {
    if (!list || list.length === 0) {
        document.getElementById('modalBody').innerHTML =
            '<div class="modal-empty">포인트 이력이 없습니다.</div>';
        return;
    }

    const rows = list.map(h => {
        const badge  = h.type === 'EARN'
            ? `<span class="badge-earn">적립</span>`
            : `<span class="badge-use">사용</span>`;
        const amount = h.type === 'EARN'
            ? `<span style="color:#2e7d32;font-weight:700;">+${h.amount.toLocaleString()} P</span>`
            : `<span style="color:#c62828;font-weight:700;">-${h.amount.toLocaleString()} P</span>`;
        const date   = h.createdAt
            ? new Date(h.createdAt).toLocaleString('ko-KR')
            : '-';
        return `
                <tr>
                    <td>${badge}</td>
                    <td>${amount}</td>
                    <td style="color:#666;">${h.balanceAfter.toLocaleString()} P</td>
                    <td style="color:#999;font-size:12px;">${date}</td>
                </tr>`;
    }).join('');

    document.getElementById('modalBody').innerHTML = `
            <table class="history-table">
                <thead>
                    <tr>
                        <th>구분</th>
                        <th>변동</th>
                        <th>처리 후 잔액</th>
                        <th>일시</th>
                    </tr>
                </thead>
                <tbody>${rows}</tbody>
            </table>`;
}

/* 포인트 이력 모달 닫기 */
function closeHistoryModal() {
    document.getElementById('historyModal').classList.remove('active');
}

/* 모달 외부 클릭 */
function closeModalOnOverlay(e) {
    if (e.target === document.getElementById('historyModal')) closeHistoryModal();
}

/* 키보드 닫기 */
document.addEventListener('keydown', e => {
    if (e.key === 'Escape') closeHistoryModal();
});
