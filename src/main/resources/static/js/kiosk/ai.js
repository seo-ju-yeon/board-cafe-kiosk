// 상태 변수
let isListening = false;  // 현재 사용자의 말을 듣고 있는 중인지
let isSpeaking = false;  // AI가 답변을 말하고 있는 중인지
let selectedVoice = 'NOVA';  // 선택된 TTS 목소리 (기본값)
let selectedSpeed = 1.0;  // TTS 재생 속도 (기본값)

// 한글 자모 조합을 위한 데이터 구조
let vkInputArray = [];   // 클릭한 자모를 순서대로 담는 배열
let vkInputValue = '';   // 조합이 완료된 최종 문장

let isKorean = true; // 한/영 전환 상태

// [수정 포인트] 무음 감지 타이머 관련 변수 삭제
// let silenceTimer = null;
// let countdownInterval = null;
// const SILENCE_TIMEOUT = 5000;

// Web Audio API
// [수정 포인트] 무음 감지용 변수(audioContext, analyser) 삭제
let mediaStream = null;
let mediaRecorder = null;  // 실제 음성을 녹음하여 데이터로 만드는 객체
let recordedChunks = [];  // 녹음된 데이터 조각들을 모으는 배열

// DOM
const audioPlayer = document.getElementById('audioPlayer');
const micBtn = document.getElementById('micBtn');
const sttDisplay = document.getElementById('sttDisplay');
const aiZone = document.getElementById('aiZone');
const spinner = document.getElementById('spinner');
const countdownWrap = document.getElementById('countdownWrap');
const countdownBar = document.getElementById('countdownBar');

document.addEventListener('DOMContentLoaded', () => {
    audioPlayer.addEventListener('ended', () => {
        isSpeaking = false;
        stopAnimation();
        sttDisplay.innerText = '🎤 마이크 버튼을 눌러 다시 질문해보세요.';
    });
    renderVirtualKeyboard();
});

/* [수정 포인트] 버튼 클릭 시 호출되는 토글 함수 추가 */
async function toggleListening() {
    // AI가 말하고 있는 중이거나, 서버 응답을 기다리는 중(버튼 비활성화)이면 무시
    if (isSpeaking || micBtn.disabled) return;

    if (isListening) {
        // 이미 듣고 있는 상태라면 수동으로 녹음 종료
        stopRecording();
    } else {
        // 대기 상태라면 녹음 시작
        await startListening();
    }
}

/* 1. 음성 녹음 로직 */
// 마이크 녹음
async function startListening() {
    // [수정 포인트] 중복 실행 방지 로직은 toggleListening() 에서 처리하므로 여기서는 제거

    try {
        // 1. 마이크 권한 획득 및 녹음 시작
        mediaStream = await navigator.mediaDevices.getUserMedia({audio: true});
        isListening = true;
        recordedChunks = [];  // 데이터 초기화

        // [수정 포인트] 버튼 비활성화 로직 제거 (토글을 위해 활성화 유지)
        setListeningState(true);

        // [수정 포인트] 사용자에게 다시 버튼을 누르도록 가이드 텍스트 강화
        sttDisplay.innerHTML = '🎙️ 듣고 있어요...<br><small style="color:#0288D1; font-weight:700;">다 말씀하신 후 마이크 버튼을 한 번 더 눌러주세요!</small>';

        // [수정 포인트] 마이크 버튼 UI를 정지(⏹️) 아이콘 및 어두운 톤으로 동적 변경
        micBtn.innerText = '⏹️';
        micBtn.style.background = '#424242';
        micBtn.style.borderColor = '#212121';
        micBtn.style.boxShadow = '0 8px 0 #111';

        // [수정 포인트] 실시간 음량 체크 및 무음 감지 로직(AudioContext 관련) 전체 삭제

        // 3. 녹음기 설정 및 데이터 수집 시작
        mediaRecorder = new MediaRecorder(mediaStream, {mimeType: 'audio/webm'});
        mediaRecorder.ondataavailable = e => {
            if (e.data.size > 0) recordedChunks.push(e.data);
        };
        mediaRecorder.onstop = onRecordingStop;  // 녹음 중단 시 후처리 함수 지정
        mediaRecorder.start(100);  // 0.1초마다 데이터 수집

        // [수정 포인트] 무음 감지 타이머 작동 코드 삭제, 카운트다운 바 숨김
        countdownWrap.classList.remove('active');
    } catch (err) {
        console.error('마이크 초기화 실패:', err);
        sttDisplay.innerText = '마이크를 사용할 수 없습니다.';
        isListening = false;

        // [수정 포인트] 권한 획득 실패 시 버튼 스타일 원래대로 복구
        micBtn.innerText = '🎤';
        micBtn.style.background = '#FF5252';
        micBtn.style.borderColor = '#fff';
        micBtn.style.boxShadow = '0 8px 0 #C62828';
    }
}

// [수정 포인트] 불필요해진 무음 감지 관련 함수(startSilenceDetection, resetSilenceTimer, startCountdownBar) 완전 삭제

function stopRecording() {
    if (!isListening) return;
    isListening = false;

    // [수정 포인트] 무음 타이머 관련 클리어 로직 삭제, 카운트다운 관련 UI 원복
    countdownWrap.classList.remove('active');

    // [수정 포인트] 녹음 종료 시 마이크 버튼 UI 원래 상태(🎤, 붉은색)로 복구
    micBtn.innerText = '🎤';
    micBtn.style.background = '#FF5252';
    micBtn.style.borderColor = '#fff';
    micBtn.style.boxShadow = '0 8px 0 #C62828';

    // [수정 포인트] 서버 통신 중에는 마이크 버튼 중복 클릭이 안 되도록 비활성화
    micBtn.disabled = true;

    if (mediaRecorder?.state !== 'inactive') mediaRecorder.stop();
    mediaStream?.getTracks().forEach(t => t.stop());
    // [수정 포인트] audioContext.close() 삭제
}

/* 서버 통신 (STT/TTS API 호출) */
// 녹음 종료 → 백엔드 전송
async function onRecordingStop() {
    setListeningState(false);
    sttDisplay.innerText = '분석 중입니다...';
    setSpinner(true);

    try {
        // 1. 모인 녹음 조각들을 하나의 파일(Blob)로 변합
        const audioBlob = new Blob(recordedChunks, {type: 'audio/webm'});
        const formData = new FormData();
        formData.append('question', audioBlob, 'speech.webm');
        formData.append('voice', selectedVoice);
        formData.append('speed', String(selectedSpeed));

        // 2. 서버 AI API 호출
        const response = await fetch('/api/ai/ai_guide', {
            method: 'POST',
            headers: {'Accept': 'application/octet-stream'},
            body: formData
        });

        if (!response.ok) throw new Error(`서버 오류: ${response.status}`);

        // 3. 헤더에 담긴 텍스트 정보 추출 (Base64 인코딩된 한글 데이터 디코딩)
        const sttEncoded = response.headers.get('X-STT-Text');
        const answerEncoded = response.headers.get('X-Answer-Text');

        const sttText = sttEncoded ? decodeBase64Utf8(sttEncoded) : '';  // 내가 한 말 표시
        const answerText = answerEncoded ? decodeBase64Utf8(answerEncoded) : '';  // AI 답변 카드 노출

        if (sttText) sttDisplay.innerText = `🗣️ "${sttText}"`;
        if (answerText) showAnswerCard(answerText);

        // 4. 응답 바디(Body)에 담긴 오디오 데이터 재생
        await playAudioResponse(response);

    } catch (err) {
        console.error('AI 응답 실패:', err);
        sttDisplay.innerText = '죄송합니다. 다시 말씀해 주시겠어요?';
        showAnswerCard('오류가 발생했습니다. 마이크 버튼을 눌러 다시 시도해주세요.');
    } finally {
        setSpinner(false);
        micBtn.disabled = false; // [수정 포인트] 서버 처리가 끝난 후 버튼 활성화
    }
}

function decodeBase64Utf8(encoded) {
    try {
        const binary = atob(encoded);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        return new TextDecoder('utf-8').decode(bytes);
    } catch (e) {
        return '';
    }
}

async function playAudioResponse(response) {
    const arrayBuffer = await response.arrayBuffer();
    const blob = new Blob([arrayBuffer], {type: 'audio/mpeg'});
    const url = URL.createObjectURL(blob);

    audioPlayer.src = url;
    audioPlayer.onloadeddata = () => {
        isSpeaking = true;
        setSpeakingState(true);
        sttDisplay.innerText = '답변을 말씀드릴게요.';
        audioPlayer.play();
    };
    audioPlayer.onerror = () => {
        sttDisplay.innerText = '음성 재생에 실패했습니다.';
    };
    audioPlayer.onended = () => {
        URL.revokeObjectURL(url);
    };
}

/* 한글 자모 타이핑 (가상 키보드) */
async function applyFeedback() {
    const revisedText = vkInputValue.trim();
    if (!revisedText) {
        alert('질문 내용을 입력해주세요.');
        return;
    }

    document.getElementById('feedbackBox').classList.remove('active');
    closeVirtualKeyboard();
    sttDisplay.innerText = `"${revisedText}"`;
    setSpinner(true);
    micBtn.disabled = true;

    try {
        const formData = new FormData();
        formData.append('question', revisedText);
        formData.append('voice', selectedVoice);
        formData.append('speed', String(selectedSpeed));

        const response = await fetch('/api/ai/ai_guide_text', {
            method: 'POST',
            headers: {'Accept': 'application/octet-stream'},
            body: formData
        });

        if (!response.ok) throw new Error(`서버 오류: ${response.status}`);

        const answerEncoded = response.headers.get('X-Answer-Text');
        const answerText = answerEncoded ? decodeBase64Utf8(answerEncoded) : '';
        if (answerText) showAnswerCard(answerText);

        await playAudioResponse(response);

    } catch (err) {
        console.error('텍스트 질문 실패:', err);
        sttDisplay.innerText = '죄송합니다. 다시 시도해주세요.';
    } finally {
        setSpinner(false);
        micBtn.disabled = false;
        // 입력 데이터 초기화
        vkInputArray = [];
        vkInputValue = '';
        updateInputDisplay();  // 실시간 조합 후 화면 갱신
    }
}

/* 가상 키보드  */
const KO_ROWS = [
    ['ㅂ', 'ㅈ', 'ㄷ', 'ㄱ', 'ㅅ', 'ㅛ', 'ㅕ', 'ㅑ', 'ㅐ', 'ㅔ'],
    ['ㅁ', 'ㄴ', 'ㅇ', 'ㄹ', 'ㅎ', 'ㅗ', 'ㅓ', 'ㅏ', 'ㅣ'],
    ['ㅋ', 'ㅌ', 'ㅊ', 'ㅍ', 'ㅠ', 'ㅜ', 'ㅡ']
];
const EN_ROWS = [
    ['q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p'],
    ['a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l'],
    ['z', 'x', 'c', 'v', 'b', 'n', 'm']
];

function renderVirtualKeyboard() {
    const rows = isKorean ? KO_ROWS : EN_ROWS;
    const vkRows = document.getElementById('vkRows');
    vkRows.innerHTML = rows.map(row =>
        `<div class="vk-row">` +
        row.map(k => `<button class="vk-key" onclick="vkType('${k}')">${k}</button>`).join('') +
        `</div>`
    ).join('');
}

function toggleLang() {
    isKorean = !isKorean;
    renderVirtualKeyboard();
}

// 자모를 배열에 쌓음
function vkType(char) {
    vkInputArray.push(char);
    updateInputDisplay();
}

// 배열에서 마지막 자모 제거
function vkDel() {
    vkInputArray.pop();
    updateInputDisplay();
}

// hangul-js를 이용해 실시간으로 조합된 한글 표시
function updateInputDisplay() {
    const el = document.getElementById('inputText');

    // Hangul.assemble()이 배열에 담긴 'ㅎ','ㅏ','ㄹ'을 '할'로 합쳐줍니다.
    vkInputValue = Hangul.assemble(vkInputArray);

    if (vkInputValue) {
        el.style.color = '#333';
        el.textContent = vkInputValue;
    } else {
        el.style.color = '#999';
        el.textContent = '여기를 눌러 입력하세요...';
    }
}

function openVirtualKeyboard() {
    document.getElementById('vkWrap').classList.add('active');
    document.getElementById('virtualInputDisplay').classList.add('focus');
}

function closeVirtualKeyboard() {
    document.getElementById('vkWrap').classList.remove('active');
    document.getElementById('virtualInputDisplay').classList.remove('focus');
}

/* 설정 / 목소리 / 속도 */
function selectVoice(btn) {
    document.querySelectorAll('.voice-btn').forEach(b => b.classList.remove('selected'));
    btn.classList.add('selected');
    selectedVoice = btn.dataset.voice;
}

function updateSpeedLabel(val) {
    selectedSpeed = parseFloat(val);
    document.getElementById('speedValue').innerText = `${selectedSpeed.toFixed(1)}x`;
}

/* UI 헬퍼 */
function setListeningState(active) {
    aiZone.classList.toggle('listening', active);
    if (active) aiZone.classList.remove('speaking');
}

function setSpeakingState(active) {
    aiZone.classList.toggle('speaking', active);
    if (active) aiZone.classList.remove('listening');
}

function stopAnimation() {
    aiZone.classList.remove('listening', 'speaking');
}

function setSpinner(active) {
    spinner.classList.toggle('active', active);
}

function showAnswerCard(text) {
    const displayArea = document.getElementById('displayArea');
    const emptyMsg = document.getElementById('emptyMsg');
    if (emptyMsg) emptyMsg.style.display = 'none';
    displayArea.innerHTML = `<div class="info-card">${text}</div>`;
}

function toggleFeedback() {
    const box = document.getElementById('feedbackBox');
    box.classList.toggle('active');
    if (box.classList.contains('active')) {
        setTimeout(() => openVirtualKeyboard(), 50);
    } else {
        closeVirtualKeyboard();
    }
}

function resetAll() {
    if (confirm('대화를 종료하고 이전 화면으로 돌아갈까요?')) {
        window.history.back();
    }
}