/* 모달 제어 */
function openModal(id) {
    document.getElementById(id).classList.add('show');
}

function closeModal(id) {
    document.getElementById(id).classList.remove('show');
}

/* 모달 외부 클릭 */
document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', function (e) {
        if (e.target === this) closeModal(this.id);
    });
});

/* 등록 모달 */
function openAddModal() {
    document.getElementById('addName').value = '';
    document.getElementById('addType').value = 'FOOD';
    openModal('addModal');
    setTimeout(() => document.getElementById('addName').focus(), 100);
}

/* 수정 모달 */
function openEditModal(btn) {
    const id = btn.dataset.id;
    const name = btn.dataset.name;
    const type = btn.dataset.type;

    // CSRF hidden input은 Thymeleaf가 삽입하므로 action만 변경
    const form = document.getElementById('editForm');
    form.action = '/admin/category/edit/' + id;

    document.getElementById('editName').value = name;
    document.getElementById('editType').value = type;

    openModal('editModal');
    setTimeout(() => document.getElementById('editName').focus(), 100);
}

/* 삭제 처리 */
function handleDelete(btn) {
    const id = btn.dataset.id;
    const count = parseInt(btn.dataset.count, 10);
    const name = btn.dataset.name;

    if (count > 0) {
        document.getElementById('deleteBlockMsg').innerHTML =
            '<strong>' + name + '</strong> 카테고리에 연결된 상품이 <strong>' + count + '개</strong> 있습니다.<br>' +
            '먼저 해당 카테고리의 상품을 다른 카테고리로 이동하거나 삭제해 주세요.';
        openModal('deleteBlockModal');
    } else {
        if (confirm('"' + name + '" 카테고리를 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.')) {
            document.getElementById('deleteForm_' + id).submit();
        }
    }
}

/* 토스트 */
['successToast', 'errorToast'].forEach(id => {
    const el = document.getElementById(id);
    if (el) setTimeout(() => el.remove(), 3000);
});
