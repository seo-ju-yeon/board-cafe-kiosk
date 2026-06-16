function filterProducts() {
    const input = document.querySelector('.search-input').value.toLowerCase();
    const rows = document.querySelectorAll('#productList tr:not(.empty-row)');
    rows.forEach(row => {
        const nameEl = row.querySelector('.prod-name');
        if (nameEl) {
            row.style.display = nameEl.textContent.toLowerCase().includes(input) ? '' : 'none';
        }
    });
}