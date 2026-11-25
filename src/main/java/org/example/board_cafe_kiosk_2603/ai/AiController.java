package org.example.board_cafe_kiosk_2603.ai;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
    /*
    음성 인식(STT), 보드게임 지식 검색(RAG), 답변 생성(LLM), 음성 합성(TTS) 통합 관리 컨트롤러
     */

    private final AiService aiService;
    private final GameEmbeddingService gameEmbeddingService;

    /* [RAG 파이프라인]: 음성 입력 -> RAG + LLM -> 스트리밍 음성 출력 */
    @PostMapping(
            value = "/ai_guide",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public StreamingResponseBody aiGuide(
            @RequestParam("question") MultipartFile question,  // 사용자의 음성 데이터
            @RequestParam(defaultValue = "NOVA") String voice, // TTS 목소리 타입
            @RequestParam(defaultValue = "1.0") Double speed, // TTS 재생 속도
            HttpServletResponse httpResponse
    ) throws IOException {
        AiService.AiResult result = aiService.chatVoiceFlux(question.getBytes(), voice, speed);

        // 오디오는 응답 본문으로 보냄. 질문과 답변은 한글을 보존하도록 Base64로 인코딩해 헤더에 담음
        String encodedStt = Base64.getEncoder()
                .encodeToString(result.sttText().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String encodedAnswer = Base64.getEncoder()
                .encodeToString(result.answerText().getBytes(java.nio.charset.StandardCharsets.UTF_8));

        httpResponse.setHeader("X-STT-Text", encodedStt);
        httpResponse.setHeader("X-Answer-Text", encodedAnswer);
        // 브라우저 JavaScript에서 이 헤더에 접근할 수 있도록 허용 설정
        httpResponse.setHeader("Access-Control-Expose-Headers", "X-STT-Text, X-Answer-Text");

        log.info("[ai_guide] voice={}, speed={}, stt={}", voice, speed, result.sttText());
        return outputStream -> writeFluxToStream(result.audioFlux(), outputStream);
    }

    /* [텍스트 수정 재질문] STT 오인식 시 사용자가 직접 타이핑 → 음성 출력 */
    @PostMapping(
            value = "/ai_guide_text",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public StreamingResponseBody aiGuideText(
            @RequestParam("question") String question,  // 사용자가 수정한 질문 텍스트
            @RequestParam(defaultValue = "NOVA") String voice,
            @RequestParam(defaultValue = "1.0") Double speed,
            HttpServletResponse httpResponse
    ) {
        // 이미 보정된 텍스트가 있으므로 STT를 건너뛰고 RAG 프로세스부터 시작
        AiService.AiResult result = aiService.chatVoiceFluxFromText(question, voice, speed);

        String encodedAnswer = Base64.getEncoder()
                .encodeToString(result.answerText().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        httpResponse.setHeader("X-Answer-Text", encodedAnswer);
        httpResponse.setHeader("Access-Control-Expose-Headers", "X-Answer-Text");

        log.info("[ai_guide_text] question={}, voice={}, speed={}", question, voice, speed);
        return outputStream -> writeFluxToStream(result.audioFlux(), outputStream);
    }

    /* 검색 대상 게임 문서를 수동으로 일괄 저장 */
    @PostMapping("/admin/reindex-games")
    public ResponseEntity<String> reindexGames() {
        log.info("[관리자] 전체 게임 재임베딩 요청");
        int count = gameEmbeddingService.embedAllGames();
        return ResponseEntity.ok(count + "개 게임 임베딩 완료");
    }

    // [유틸] 단독 STT 테스트용 (개발/디버깅 용도로 유지)
    @PostMapping(
            value = "/stt",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public String stt(@RequestParam("speech") MultipartFile speech) throws IOException {
        return aiService.stt(speech.getBytes());
    }

    /* Flux 데이터 조각을 응답 스트림에 순차적으로 기록하는 헬퍼 메서드 */
    // [공통] Flux<byte[]> → OutputStream 쓰기
    private void writeFluxToStream(Flux<byte[]> flux, OutputStream outputStream) throws IOException {
        for (byte[] chunk : flux.toIterable()) {
            outputStream.write(chunk);
            outputStream.flush();  // Chunk-by-chunk 전송을 통해 스트리밍 효과 극대화
        }
    }
}
