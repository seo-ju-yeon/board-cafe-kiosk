let dailySalesChart, categorySalesChart;

/* 초기화 */
document.addEventListener('DOMContentLoaded', function() {
    const dateInput = document.getElementById('targetDate');
    const today = new Date().toISOString().split('T')[0];

    if (dateInput) {
        dateInput.setAttribute('max', today);
        initCharts();
        loadStatData(dateInput.value);
    }
});

/* 통계 데이터 조회 */
async function loadStatData(date) {
    try {
        console.log("요청 날짜:", date);
        const response = await fetch(`/admin/api/statistics?targetDate=${date}`);

        if (!response.ok) {
            throw new Error("API 통신 에러 또는 데이터 없음");
        }

        const data = await response.json();
        console.log("받은 데이터:", data);

        const summary = data.summary || { totalRevenue: 0, orderCount: 0, visitCount: 0, avgUsageTime: 0 };
        const weeklyAvgRevenue = data.weeklyAvgRevenue || 0;

        // 핵심 지표가 모두 0이면 해당 날짜의 운영 데이터가 없는 것으로 처리
        if (summary.totalRevenue === 0 && summary.orderCount === 0 && summary.visitCount === 0) {
            alert("해당하는 날짜의 데이터가 없습니다.");
        }

        document.getElementById('stat-total-revenue').innerText = '₩' + (summary.totalRevenue || 0).toLocaleString();
        document.getElementById('stat-total-orders').innerText = (summary.orderCount || 0) + '건';
        document.getElementById('stat-total-visitors').innerText = (summary.visitCount || 0) + '명';
        document.getElementById('stat-daily-avg').innerText = '₩' + weeklyAvgRevenue.toLocaleString();
        document.getElementById('stat-avg-time').innerText = (summary.avgUsageTime || 0) + 'm';

        if (data.weeklySales && data.weeklySales.length > 0) {
            dailySalesChart.data.labels = data.weeklySales.map(d => d.statDate);
            dailySalesChart.data.datasets[0].data = data.weeklySales.map(d => d.totalRevenue);
        } else {
            dailySalesChart.data.labels = [];
            dailySalesChart.data.datasets[0].data = [];
        }
        dailySalesChart.update();

        if (data.categoryStats && data.categoryStats.labels && data.categoryStats.labels.length > 0) {
            categorySalesChart.data.labels = data.categoryStats.labels;
            categorySalesChart.data.datasets[0].data = data.categoryStats.values;
        } else {
            categorySalesChart.data.labels = [];
            categorySalesChart.data.datasets[0].data = [];
        }
        categorySalesChart.update();

        updateBestSellers(data.topMenus, date);

        updateTopGames(data.topGames, date);

    } catch (e) {
        console.error("데이터 로드 중 에러 발생:", e);
        alert("해당하는 날짜의 데이터가 없습니다.");
        clearAllData(date);
    }
}

/* 통계 화면 초기화 */
function clearAllData(date) {
    document.getElementById('stat-total-revenue').innerText = '₩0';
    document.getElementById('stat-total-orders').innerText = '0건';
    document.getElementById('stat-total-visitors').innerText = '0명';
    document.getElementById('stat-daily-avg').innerText = '₩0';
    document.getElementById('stat-avg-time').innerText = '0m';

    dailySalesChart.data.labels = [];
    dailySalesChart.data.datasets[0].data = [];
    dailySalesChart.update();

    categorySalesChart.data.labels = [];
    categorySalesChart.data.datasets[0].data = [];
    categorySalesChart.update();

    updateBestSellers([], date);
    updateTopGames([], date);
}

/* 메뉴 베스트 셀러 렌더링 */
function updateBestSellers(menus, targetDateStr) {
    const container = document.getElementById('best-seller-container');
    const dateObj = new Date(targetDateStr);
    const monthStr = dateObj.getMonth() + 1;
    const dayStr = dateObj.getDate();

    let html = `<div class="chart-title">${monthStr}월 ${dayStr}일 메뉴 베스트 셀러 (TOP 5)</div>`;

    if (!menus || menus.length === 0) {
        html += '<div style="text-align:center; padding:30px; color:#999;">판매 데이터가 없습니다.</div>';
    } else {
        menus.forEach((item, index) => {
            html += `
            <div class="seller-item">
                <div class="rank-badge">${index + 1}</div>
                <div class="item-name">${item.menuName}</div>
                <div class="item-count">${item.salesQty}개 판매</div>
                <div class="item-price">₩${item.salesAmount.toLocaleString()}</div>
            </div>`;
        });
    }
    container.innerHTML = html;
}

/* 인기 게임 렌더링 */
function updateTopGames(games, targetDateStr) {
    const container = document.getElementById('top-games-container');
    const dateObj = new Date(targetDateStr);
    const monthStr = dateObj.getMonth() + 1;

    let html = `<div class="chart-title">${monthStr}월 인기 보드게임 (TOP 5)</div>`;

    if (!games || games.length === 0) {
        html += '<div style="text-align:center; padding:30px; color:#999;">대여 데이터가 없습니다.</div>';
    } else {
        games.forEach((item, index) => {
            html += `
                <div class="seller-item">
                    <div class="rank-badge">${index + 1}</div>
                    <div class="item-name">${item.gameName}</div>
                    <div class="item-price">${item.rentCount}회 대여</div>
                </div>`;
        });
    }
    container.innerHTML = html;
}

/* 차트 초기화 */
function initCharts() {
    if (dailySalesChart) dailySalesChart.destroy();
    if (categorySalesChart) categorySalesChart.destroy();

    dailySalesChart = new Chart(document.getElementById('dailySalesChart'), {
        type: 'line',
        data: { labels: [], datasets: [{ label: '매출액', data: [], borderColor: '#007bff', fill: true, tension: 0.4 }] },
        options: {
            responsive: true,
            plugins: { legend: { display: false } },
            scales: {
                y: {
                    beginAtZero: true,
                    min: 0,
                    suggestedMax: 10000
                }
            }
        }
    });

    categorySalesChart = new Chart(document.getElementById('categorySalesChart'), {
        type: 'bar',
        data: { labels: [], datasets: [{ data: [], backgroundColor: ['#007bff', '#28a745', '#fd7e14', '#ffc107', '#6f42c1'] }] },
        options: {
            responsive: true,
            plugins: { legend: { display: false } },
            scales: {
                y: {
                    beginAtZero: true,
                    min: 0
                }
            }
        }
    });
}
