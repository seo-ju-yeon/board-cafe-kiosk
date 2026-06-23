/*
 * 키오스크 AI 안내 화면
 *
 * 프론트 역할: 음성 녹음, 텍스트 보정 입력, TTS 옵션 전달, 오디오 재생 담당
 * 실제 AI 처리: 백엔드 /api/ai/ai_guide 계열 API에서 수행됨
 *
 * 처리 흐름:
 * 1. 사용자 질문 입력
 * 2. 프론트에서 질문 데이터와 voice/speed 옵션 전송
 * 3. 백엔드에서 STT -> RAG 검색 -> LLM 답변 생성 -> TTS 변환 처리
 * 4. 프론트에서 응답 헤더 텍스트 표시 및 응답 Body MP3 재생
 */

/* AI 안내 상태 */
let isListening = false; // 마이크 녹음 중 여부
let isSpeaking = false;  // AI 음성 답변 재생 중 여부
let selectedVoice = 'NOVA';  // 백엔드 TTS에 전달할 목소리
let selectedSpeed = 1.0;  // 백엔드 TTS에 전달할 재생 속도

/* 가상 키보드 입력 상태 */
let vkInputArray = [];  // 조합 전 한글 자모/영문 입력값
let vkInputValue = '';  // 화면에 표시하고 백엔드로 보낼 최종 질문
let isKorean = true;  // 가상 키보드 한/영 모드

/* 음성 녹음 상태 */
let mediaStream = null;  // 브라우저 마이크 입력 스트림
let mediaRecorder = null;  // 마이크 입력을 녹음 데이터로 변환하는 객체
let recordedChunks = [];  // MediaRecorder가 만든 음성 조각 목록

/* DOM 참조 */
const audioPlayer = document.getElementById('audioPlayer');
const micBtn = document.getElementById('micBtn');
const sttDisplay = document.getElementById('sttDisplay');
const aiZone = document.getElementById('aiZone');
const spinner = document.getElementById('spinner');
const countdownWrap = document.getElementById('countdownWrap');
const countdownBar = document.getElementById('countdownBar');

/* 초기화 */
document.addEventListener('DOMContentLoaded', () => {
    audioPlayer.addEventListener('ended', () => {
        isSpeaking = false;
        stopAnimation();
        sttDisplay.innerText = '🎤 마이크 버튼을 눌러 다시 질문해보세요.';
    });
    renderVirtualKeyboard();
});

/* 마이크 녹음 토글 */
async function toggleListening() {
    // AI 음성 재생 중 또는 서버 응답 대기 중 중복 입력 방지
    if (isSpeaking || micBtn.disabled) return;

    if (isListening) {
        stopRecording();
    } else {
        await startListening();
    }
}

/* 음성 질문 녹음 */
async function startListening() {
    try {
        // 사용자가 마이크 버튼을 다시 눌러 녹음을 종료하는 방식
        mediaStream = await navigator.mediaDevices.getUserMedia({audio: true});
        isListening = true;
        recordedChunks = [];

        setListeningState(true);

        sttDisplay.innerHTML = '🎙️ 듣고 있어요...<br><small style="color:#0288D1; font-weight:700;">다 말씀하신 후 마이크 버튼을 한 번 더 눌러주세요!</small>';

        micBtn.innerText = '⏹️';
        micBtn.style.background = '#424242';
        micBtn.style.borderColor = '#212121';
        micBtn.style.boxShadow = '0 8px 0 #111';

        // 0.1초 단위 webm 음성 조각 수집 후 녹음 종료 시 Blob으로 병합
        mediaRecorder = new MediaRecorder(mediaStream, {mimeType: 'audio/webm'});
        mediaRecorder.ondataavailable = e => {
            if (e.data.size > 0) recordedChunks.push(e.data);
        };
        mediaRecorder.onstop = onRecordingStop;
        mediaRecorder.start(100);

        countdownWrap.classList.remove('active');
    } catch (err) {
        console.error('마이크 초기화 실패:', err);
        sttDisplay.innerText = '마이크를 사용할 수 없습니다.';
        isListening = false;

        micBtn.innerText = '🎤';
        micBtn.style.background = '#FF5252';
        micBtn.style.borderColor = '#fff';
        micBtn.style.boxShadow = '0 8px 0 #C62828';
    }
}

/* 음성 질문 녹음 종료 */
function stopRecording() {
    if (!isListening) return;
    isListening = false;

    countdownWrap.classList.remove('active');

    micBtn.innerText = '🎤';
    micBtn.style.background = '#FF5252';
    micBtn.style.borderColor = '#fff';
    micBtn.style.boxShadow = '0 8px 0 #C62828';

    // 녹음 파일 서버 전송 중 중복 클릭 방지
    micBtn.disabled = true;

    if (mediaRecorder?.state !== 'inactive') mediaRecorder.stop();
    mediaStream?.getTracks().forEach(t => t.stop());
}

/* 녹음 종료 후 백엔드 AI 파이프라인 호출 */
async function onRecordingStop() {
    setListeningState(false);
    sttDisplay.innerText = '분석 중입니다...';
    setSpinner(true);

    try {
        // 녹음 조각을 하나의 webm 파일로 묶어 백엔드에 전달
        const audioBlob = new Blob(recordedChunks, {type: 'audio/webm'});
        const formData = new FormData();
        formData.append('question', audioBlob, 'speech.webm');
        formData.append('voice', selectedVoice);
        formData.append('speed', String(selectedSpeed));

        // 백엔드에서 STT -> RAG 검색 -> LLM 답변 생성 -> TTS 변환 처리
        const response = await fetch('/api/ai/ai_guide', {
            method: 'POST',
            headers: {'Accept': 'application/octet-stream'},
            body: formData
        });

        if (!response.ok) throw new Error(`서버 오류: ${response.status}`);

        // HTTP 헤더의 한글 깨짐 방지를 위한 Base64 인코딩 값 수신
        const sttEncoded = response.headers.get('X-STT-Text');
        const answerEncoded = response.headers.get('X-Answer-Text');

        const sttText = sttEncoded ? decodeBase64Utf8(sttEncoded) : '';
        const answerText = answerEncoded ? decodeBase64Utf8(answerEncoded) : '';

        if (sttText) sttDisplay.innerText = `🗣️ "${sttText}"`;
        if (answerText) showAnswerCard(answerText);

        // 응답 Body에 포함된 TTS 결과 MP3 재생
        await playAudioResponse(response);

    } catch (err) {
        console.error('AI 응답 실패:', err);
        sttDisplay.innerText = '죄송합니다. 다시 말씀해 주시겠어요?';
        showAnswerCard('오류가 발생했습니다. 마이크 버튼을 눌러 다시 시도해주세요.');
    } finally {
        setSpinner(false);
        micBtn.disabled = false;
    }
}

/* Base64 헤더 텍스트 디코딩 */
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

/* 백엔드 TTS 오디오 재생 */
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

/* STT 보정 텍스트 재전송 */
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
        // STT 결과 보정 후 같은 RAG 파이프라인으로 재전송
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
        vkInputArray = [];
        vkInputValue = '';
        updateInputDisplay();
    }
}

/* 가상 키보드 키 배열 */
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

/* 가상 키보드 렌더링 */
function renderVirtualKeyboard() {
    const rows = isKorean ? KO_ROWS : EN_ROWS;
    const vkRows = document.getElementById('vkRows');
    vkRows.innerHTML = rows.map(row =>
        `<div class="vk-row">` +
        row.map(k => `<button class="vk-key" onclick="vkType('${k}')">${k}</button>`).join('') +
        `</div>`
    ).join('');
}

/* 가상 키보드 한/영 전환 */
function toggleLang() {
    isKorean = !isKorean;
    renderVirtualKeyboard();
}

/* 가상 키보드 입력 */
function vkType(char) {
    vkInputArray.push(char);
    updateInputDisplay();
}

/* 가상 키보드 삭제 */
function vkDel() {
    vkInputArray.pop();
    updateInputDisplay();
}

/* 가상 키보드 입력값 표시 */
function updateInputDisplay() {
    const el = document.getElementById('inputText');

    // Hangul.assemble()을 통한 자모 배열의 한글 문장 조합
    vkInputValue = Hangul.assemble(vkInputArray);

    if (vkInputValue) {
        el.style.color = '#333';
        el.textContent = vkInputValue;
    } else {
        el.style.color = '#999';
        el.textContent = '여기를 눌러 입력하세요...';
    }
}

/* 가상 키보드 열기 */
function openVirtualKeyboard() {
    document.getElementById('vkWrap').classList.add('active');
    document.getElementById('virtualInputDisplay').classList.add('focus');
}

/* 가상 키보드 닫기 */
function closeVirtualKeyboard() {
    document.getElementById('vkWrap').classList.remove('active');
    document.getElementById('virtualInputDisplay').classList.remove('focus');
}

/* TTS 목소리 선택 */
function selectVoice(btn) {
    document.querySelectorAll('.voice-btn').forEach(b => b.classList.remove('selected'));
    btn.classList.add('selected');
    selectedVoice = btn.dataset.voice;
}

/* TTS 속도 선택 */
function updateSpeedLabel(val) {
    selectedSpeed = parseFloat(val);
    document.getElementById('speedValue').innerText = `${selectedSpeed.toFixed(1)}x`;
}

/* 녹음 중 UI 상태 */
function setListeningState(active) {
    aiZone.classList.toggle('listening', active);
    if (active) aiZone.classList.remove('speaking');
}

/* 답변 재생 중 UI 상태 */
function setSpeakingState(active) {
    aiZone.classList.toggle('speaking', active);
    if (active) aiZone.classList.remove('listening');
}

/* AI 상태 애니메이션 중지 */
function stopAnimation() {
    aiZone.classList.remove('listening', 'speaking');
}

/* 로딩 표시 */
function setSpinner(active) {
    spinner.classList.toggle('active', active);
}

/* AI 답변 카드 표시 */
function showAnswerCard(text) {
    const displayArea = document.getElementById('displayArea');
    const emptyMsg = document.getElementById('emptyMsg');
    if (emptyMsg) emptyMsg.style.display = 'none';
    displayArea.innerHTML = `<div class="info-card">${text}</div>`;
}

/* STT 보정 입력 영역 토글 */
function toggleFeedback() {
    const box = document.getElementById('feedbackBox');
    box.classList.toggle('active');
    if (box.classList.contains('active')) {
        setTimeout(() => openVirtualKeyboard(), 50);
    } else {
        closeVirtualKeyboard();
    }
}

/* AI 안내 화면 종료 */
function resetAll() {
    if (confirm('대화를 종료하고 이전 화면으로 돌아갈까요?')) {
        window.history.back();
    }
}
