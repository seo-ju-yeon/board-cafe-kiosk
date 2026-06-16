/* ── 이미지 미리보기 ── */
function previewImage(event) {
    const file = event.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = function (e) {
        const img         = document.getElementById('previewImg');
        const placeholder = document.getElementById('imgPlaceholder');
        img.src           = e.target.result;
        img.style.display = 'block';
        placeholder.style.display = 'none';
    };
    reader.readAsDataURL(file);
}

/* ── 상태 변경 시 행 색상 업데이트 + 삭제 버튼 활성/비활성 ── */
function onStatusChange(select, itemId) {
    const row    = document.getElementById('existingRow-' + itemId);
    const delBtn = row.querySelector('.btn-remove-item');
    const status = select.value;

    // 행 색상 갱신
    row.classList.remove('status-NORMAL', 'status-RENTED', 'status-DAMAGED', 'status-LOST');
    row.classList.add('status-' + status);

    // NORMAL / RENTED → 삭제 버튼 비활성 (대여 가능·대여 중은 삭제 불가)
    // DAMAGED / LOST  → 삭제 버튼 활성
    if (status === 'NORMAL' || status === 'RENTED') {
        delBtn.disabled = true;
        delBtn.title    = status === 'NORMAL'
            ? '대여 가능 항목은 삭제할 수 없습니다'
            : '대여 중인 항목은 삭제할 수 없습니다';
        delBtn.onclick  = null;
    } else {
        delBtn.disabled = false;
        delBtn.title    = '아이템을 삭제합니다';
        delBtn.onclick  = function() { markDeleted(itemId, delBtn); };
    }
}

/* ── 기존 item 삭제 표시 ── */
function markDeleted(itemId, btn) {
    // disabled 상태에서 JS 강제 호출 방어
    if (btn.disabled) return;

    const hidden = document.getElementById('deletedItemIds');
    hidden.value = hidden.value ? hidden.value + ',' + itemId : String(itemId);
    btn.closest('.item-row').style.display = 'none';
}

/* ── 신규 재고 행 추가 ── */
let newIdx = 0;

function addNewItem() {
    const list = document.getElementById('newItemList');
    const idx  = newIdx++;
    const row  = document.createElement('div');
    row.className = 'item-row status-NORMAL';
    row.innerHTML =
        '<span class="item-label">시리얼 번호</span>' +
        '<input type="text" class="form-control"' +
        '       name="newItems[' + idx + '].serialNumber"' +
        '       placeholder="시리얼 번호를 입력하세요">' +
        '<input type="hidden" name="newItems[' + idx + '].status" value="NORMAL">' +
        '<button type="button" class="btn-remove-item"' +
        '        onclick="this.closest(\'.item-row\').remove()">' +
        '    삭제' +
        '</button>';
    list.appendChild(row);
}