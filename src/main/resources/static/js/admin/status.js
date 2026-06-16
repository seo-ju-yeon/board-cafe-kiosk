let dailySalesChart, categorySalesChart;

document.addEventListener('DOMContentLoaded', function() {
    // 오늘 날짜 구하기 및 max 설정
    const dateInput = document.getElementById('targetDate');
    const today = new Date().toISOString().split('T')[0];

    if (dateInput) {
        dateInput.setAttribute('max', today);
        // 초기 차트 생성 (딱 한 번만 실행됨)
        initCharts();
        // 데이터 로드
        loadStatData(dateInput.value);
    }
});

async function loadStatData(date) {
    try {
        console.log("요청 날짜:", date);
        const response = await fetch(`/admin/api/statistics?targetDate=${date}`);

        // 상태 코드가 정상이 아닌 경우 에러를 던져 catch 문으로 이동
        if (!response.ok) {
            throw new Error("API 통신 에러 또는 데이터 없음");
        }

        const data = await response.json();
        console.log("받은 데이터:", data);

        const summary = data.summary || { totalRevenue: 0, orderCount: 0, visitCount: 0, avgUsageTime: 0 };
        const weeklyAvgRevenue = data.weeklyAvgRevenue || 0;

        // [데이터 없음 조건 검사] 매출, 주문건수, 방문자수가 모두 0이면 데이터가 없는 것으로 간주
        if (summary.totalRevenue === 0 && summary.orderCount === 0 && summary.visitCount === 0) {
            alert("해당하는 날짜의 데이터가 없습니다.");
        }

        // 1. 요약 카드 업데이트
        document.getElementById('stat-total-revenue').innerText = '₩' + (summary.totalRevenue || 0).toLocaleString();
        document.getElementById('stat-total-orders').innerText = (summary.orderCount || 0) + '건';
        document.getElementById('stat-total-visitors').innerText = (summary.visitCount || 0) + '명';
        document.getElementById('stat-daily-avg').innerText = '₩' + weeklyAvgRevenue.toLocaleString();
        document.getElementById('stat-avg-time').innerText = (summary.avgUsageTime || 0) + 'm';

        // 2. 주간 매출 차트 업데이트 (데이터 없으면 빈 그래프)
        if (data.weeklySales && data.weeklySales.length > 0) {
            dailySalesChart.data.labels = data.weeklySales.map(d => d.statDate);
            dailySalesChart.data.datasets[0].data = data.weeklySales.map(d => d.totalRevenue);
        } else {
            dailySalesChart.data.labels = [];
            dailySalesChart.data.datasets[0].data = [];
        }
        dailySalesChart.update();

        // 3. 카테고리 차트 업데이트 (데이터 없으면 빈 그래프)
        if (data.categoryStats && data.categoryStats.labels && data.categoryStats.labels.length > 0) {
            categorySalesChart.data.labels = data.categoryStats.labels;
            categorySalesChart.data.datasets[0].data = data.categoryStats.values;
        } else {
            categorySalesChart.data.labels = [];
            categorySalesChart.data.datasets[0].data = [];
        }
        categorySalesChart.update();

        // 4. 베스트 셀러 업데이트
        updateBestSellers(data.topMenus, date);

        // 5. 월간 인기 게임 업데이트
        updateTopGames(data.topGames, date);

    } catch (e) {
        console.error("데이터 로드 중 에러 발생:", e);
        // 에러가 발생한 경우(서버 뻗음, 404 등)에도 팝업 후 데이터 비우기
        alert("해당하는 날짜의 데이터가 없습니다.");
        clearAllData(date);
    }
}

// 예외 상황 발생 시 화면 데이터를 모두 0 및 빈 상태로 초기화하는 헬퍼 함수
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

// 일간 메뉴 베스트 셀러 리스트 렌더링 함수
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

// 월간 인기 게임 리스트 렌더링 함수
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

function initCharts() {
    // 만약 이미 차트가 존재한다면 삭제하고 재생성 (안전장치)
    if (dailySalesChart) dailySalesChart.destroy();
    if (categorySalesChart) categorySalesChart.destroy();

    // 매출 차트 초기화
    dailySalesChart = new Chart(document.getElementById('dailySalesChart'), {
        type: 'line',
        data: { labels: [], datasets: [{ label: '매출액', data: [], borderColor: '#007bff', fill: true, tension: 0.4 }] },
        options: {
            responsive: true,
            plugins: { legend: { display: false } },
            scales: {
                y: {
                    beginAtZero: true, // Y축을 항상 0부터 시작하게 강제
                    min: 0,            // 최소값을 0으로 고정
                    suggestedMax: 10000 // 데이터가 빈 배열일 때 Y축 스케일이 너무 작아지는 것 방지
                }
            }
        }
    });

    // 카테고리 차트 초기화
    categorySalesChart = new Chart(document.getElementById('categorySalesChart'), {
        type: 'bar',
        data: { labels: [], datasets: [{ data: [], backgroundColor: ['#007bff', '#28a745', '#fd7e14', '#ffc107', '#6f42c1'] }] },
        options: {
            responsive: true,
            plugins: { legend: { display: false } },
            scales: {
                y: {
                    beginAtZero: true, // Y축을 항상 0부터 시작하게 강제
                    min: 0
                }
            }
        }
    });
}