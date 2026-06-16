package org.example.board_cafe_kiosk_2603.ai;

import lombok.extern.log4j.Log4j2;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
public class AiService {

    /* 음성 인식(STT), 보드게임 지식 검색(RAG), 답변 생성(LLM), 음성 합성(TTS) 통합 관리 서비스 */

    private final ChatClient chatClient;
    @Autowired
    private OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel; // 음성을 텍스트로 변환 STT 모델
    @Autowired
    private OpenAiAudioSpeechModel openAiAudioSpeechModel; // 텍스트를 음성으로 변환 TTS 모델
    @Autowired
    private VectorStore vectorStore;  // PGVector VectorStore (PgVectorConfig에서 빈으로 등록됨)

    /* RAG 튜닝 포인트 */
    // RAG 유사도 검색 임계값 (한국어 특성상 짧은 문장은 점수가 낮을 수 있어 완화된 기준 적용)
    private static final double SIMILARITY_THRESHOLD = 0.3;
    // 검색 결과 최대 개수 (가장 관련성 높은 상위 3개의 게임 정보만 참조)
    private static final int TOP_K = 3;

    public AiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /* 음성 입력을 받아 최종 AI 응답 객체(AiResult) 생성 */
    public AiResult chatVoiceFlux(byte[] audioQuestion, String voice, Double speed) {
        // 1. STT : 사용자의 목소리를 텍스트로 반환
        String textQuestion = stt(audioQuestion);
        log.info("[STT 결과] {}", textQuestion);

        // 2. RAG + LLM : 변환된 텍스트로 지식을 검색하고 답변 생성
        String textAnswer = generateAnswer(textQuestion);
        log.info("[LLM 답변] {}", textAnswer);

        // 3. TTS Flux : 답변 텍스트를 다시 음성 스트림으로 변환
        Flux<byte[]> audioFlux = ttsFlux(textAnswer, voice, speed);

        // 결과 종합 (질문 텍스트, 답변 텍스트, 오디오 스트림)
        return new AiResult(textQuestion, textAnswer, audioFlux);
    }

    /* STT 과정에서 '음성 인식 오류'를 보정하기 위한 경로 */
    // 사용자가 직접 입력하거나 수정된 텍스트를 바탕으로 RAG 단계로 진입
    public AiResult chatVoiceFluxFromText(String textQuestion, String voice, Double speed) {
        log.info("[텍스트 직접 입력] {}", textQuestion);

        // 1. RAG + LLM : STT 단계를 생략하고, 전달받은 텍스트로 바로 지식 베이스(VectorStore) 검색 및 답변 생성
        String textAnswer = generateAnswer(textQuestion);
        log.info("[LLM 답변] {}", textAnswer);

        // 2. TTS Flux: 생성된 답변을 사용자 설정(목소리, 속도)에 맞춰 음성 스트림으로 변환
        Flux<byte[]> audioFlux = ttsFlux(textAnswer, voice, speed);

        // 3. 결과 반환: 입력 텍스트(질문), 생성 텍스트(답변), 오디오 스트림을 묶어서 반환
        return new AiResult(textQuestion, textAnswer, audioFlux);
    }

    /* [RAG] 질문과 관련된 지식을 VectorStore에서 찾아 LLM에게 전달 */
    // - PGVector 유사도 검색 → DB에 있는 게임만 컨텍스트 사용
    // - 검색 결과 없음 → "등록된 게임 없음" 프롬프트
    // - 검색 결과 있음 → 해당 게임 정보를 LLM 프롬프트에 주입
    private String generateAnswer(String textQuestion) {

        // 1. 유사도 검색: VectorDB에서 질문과 가장 유사한 게임 정보를 조회
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(textQuestion)
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .build()
        );
        log.info("[RAG] 검색된 게임 수: {}", docs.size());

        docs.forEach(d -> log.info("[RAG] 매칭 게임: {}, 유사도: {}",
                d.getMetadata().get("gameName"), d.getScore()));

        // 2. 검색된 문서들의 내용을 하나로 합침
        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        // 3. 시스템 프롬프트에 지식을 주입하여 답변 생성
        return chatClient.prompt()
                .system(buildSystemPrompt(context))
                .user(textQuestion)
                .call()
                .content();
    }

    /* RAG 검색 결과에 따른 결과 처리 */
    private String buildSystemPrompt(String context) {
        if (context == null || context.isBlank()) {
            // 1. 검색 결과가 없는 경우
            // 모델이 지식을 지어내지 못하게 차단 (Hallucination 방지)
            return """
                    당신은 보드카페 키오스크 AI 안내원입니다.
                    사용자가 요청한 게임은 저희 매장에 등록되어 있지 않습니다.
                    반드시 다음 문장으로만 답변하세요:
                    "죄송합니다. 요청하신 게임은 저희 매장에 등록된 게임이 없습니다."
                    게임 추천이나 다른 안내는 절대 하지 마세요.
                    """;
        }
        // 2. 검색 결과가 있는 경우
        // 제공된 지식(Context) 내에서만 답변하도록 제약
        return """
                당신은 보드카페 키오스크 AI 안내원입니다.
                반드시 아래 [게임 정보]에 있는 내용만을 바탕으로 답변하세요.
                게임 이름, 설명, 플레이 인원, 평균 플레이 시간을 자연스럽게 안내하세요.
                [게임 정보]에 없는 게임을 질문받으면 "요청하신 게임은 저희 매장에 등록된 게임이 없습니다."라고만 답하세요.
                답변은 200자 이내 한국어로 해주세요.
                
                [게임 정보]
                %s
                """.formatted(context);
    }

    /* STT: 음성(byte[])을 텍스트로 추출 */
    public String stt(byte[] bytes) {
        Resource audioResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() { return "audio.mp3"; }
        };

        OpenAiAudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions.builder()
                .model("whisper-1")
                .language("ko")
                .build();

        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource, options);
        AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(prompt);
        return response.getResult().getOutput();
    }

    // TTS 동기: 텍스트 → byte[] (테스트용)
//    public byte[] tts(String text, String voice, Double speed) {
//        TextToSpeechPrompt prompt = new TextToSpeechPrompt(text, buildSpeechOptions(voice, speed));
//        TextToSpeechResponse response = openAiAudioSpeechModel.call(prompt);
//        return response.getResult().getOutput();
//    }

    /* TTS 스트리밍: 답변을 실시간 오디오 조각(Flux)으로 변환 (텍스트 → Flux<byte[]>) */
    private Flux<byte[]> ttsFlux(String text, String voice, Double speed) {
        TextToSpeechPrompt prompt = new TextToSpeechPrompt(text, buildSpeechOptions(voice, speed));
        Flux<TextToSpeechResponse> responseFlux = openAiAudioSpeechModel.stream(prompt);
        return responseFlux.map(r -> r.getResult().getOutput());
    }

    /* TTS option: 목소리 종류와 재생 속도를 동적으로 설정 */
    private OpenAiAudioSpeechOptions buildSpeechOptions(String voice, Double speed) {
        OpenAiAudioApi.SpeechRequest.Voice selectedVoice;
        try {
            selectedVoice = OpenAiAudioApi.SpeechRequest.Voice.valueOf(voice.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("[TTS] 알 수 없는 voice: {}. 기본값 NOVA 사용.", voice);
            selectedVoice = OpenAiAudioApi.SpeechRequest.Voice.NOVA;
        }

        // 속도 제한
        double clampedSpeed = Math.max(0.25, Math.min(2.0, speed));

        return OpenAiAudioSpeechOptions.builder()
                .model("gpt-4o-mini-tts")
                .voice(selectedVoice)
                .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
                .speed(clampedSpeed)
                .build();
    }

    /* AI 처리 결과를 담는 DTO 레코드 */
    public record AiResult(String sttText, String answerText, Flux<byte[]> audioFlux) {}
}
