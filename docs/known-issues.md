# 현재 한계와 개선할 내용

외부 공개 전 인증 점검과 테스트 환경 준비를 우선하고, 이후 데이터 갱신과 기능 검증을 진행하려고 합니다.
`미해결`은 구현에 남아 있는 한계, `검증 필요`는 발생 조건과 영향을 더 확인해야 하는 항목입니다.

## 개선 목록

| 항목 | 우선순위·상태 | 영향 | 다음 작업 |
| --- | --- | --- | --- |
| KI-01 인증·접근 제어 | 높음 · 검증 필요 | 시연용 인증 처리의 환경 분리와 API별 접근 규칙 점검이 필요합니다. 인증 단계별 자동 테스트도 부족합니다. | 인증 예외와 민감정보 로그를 점검하고, 역할·인증 단계별 접근 테스트를 추가합니다. |
| KI-02 테스트 실행 준비 | 높음 · 미해결 | 일부 테스트가 실제 DB를 변경하거나 메일을 발송합니다. Flyway 검증용 스크립트와 SQL도 누락되어 있습니다. | 테스트 DB와 외부 서비스 대체 수단을 준비하고 누락된 파일을 재작성합니다. |
| KI-03 RAG 갱신 실패 | 중간 · 미해결 | 벡터 갱신 실패 시 자동 재시도가 없어 운영 DB의 변경이 검색에 반영되지 않을 수 있습니다. | 실패를 추적하고 재처리할 방법을 마련합니다. |
| KI-04 RAG와 현재 재고 | 중간 · 미해결 | 대여·반납을 매번 반영하지 않아 AI 안내와 실제 재고가 다를 수 있습니다. 시작 시 일괄 저장도 오래된 문서를 대조해 정리하지 않습니다. | 안내에 필요한 재고 기준을 정하고 오래된 문서를 정리하는 방법을 마련합니다. |
| KI-05 RAG 검색·응답 품질 | 중간 · 검증 필요 | 일부 질문만 수동 확인했으며 별도 검색 품질 평가는 없습니다. 검색 누락도 미등록 게임으로 안내할 수 있습니다. | 대표 질문으로 결과를 비교하고, 검색 실패와 실제 미등록 게임을 구분하도록 개선합니다. |
| KI-06 게임·메뉴 연결 | 중간 · 검증 필요 | 게임과 메뉴를 이름으로 JOIN하는 조회에서 같은 이름의 메뉴가 여러 건이면 재고가 중복 집계될 가능성이 있습니다. | 이름 중복·변경 상황을 검증하고 ID로 연결하는 방법을 검토합니다. |
| KI-07 OTP 메모리 저장 | 낮음 · 미해결 | 서버 재시작 시 OTP가 사라지고, 여러 서버가 상태를 공유하지 못합니다. | 만료·동시 요청을 검증하고, 다중 서버 운영이 필요하면 공용 저장소를 검토합니다. |
| KI-08 과거 메뉴 정보 | 낮음 · 미해결 | Soft Delete는 메뉴 행을 남기지만 변경 이력은 보존하지 않아 과거 통계에도 현재 메뉴 정보가 표시될 수 있습니다. | 주문·통계에 당시 메뉴 정보를 별도로 저장할 필요가 있는지 검토합니다. |

KI-06은 아직 재현하지 않은 가능성으로, 과거 신규 게임 집계 오류의 원인과는 별도로 확인해야 합니다.
개선 항목은 관련 Issue·PR과 테스트 결과를 연결해 관리할 예정입니다.

## 관련 코드와 문서

- **인증·OTP**: [SecurityConfig](../src/main/java/org/example/board_cafe_kiosk_2603/config/SecurityConfig.java), [OtpStore](../src/main/java/org/example/board_cafe_kiosk_2603/config/OtpStore.java), [ADR 001](adr/001-authentication-design.md)
- **테스트 준비**: [Testing](testing.md)
- **RAG 갱신·재고 반영**: [GameEmbeddingService](../src/main/java/org/example/board_cafe_kiosk_2603/ai/GameEmbeddingService.java), [GameItemServiceImpl](../src/main/java/org/example/board_cafe_kiosk_2603/service/admin/product/GameItemServiceImpl.java), [ADR 003](adr/003-rag-synchronization.md)
- **검색·응답**: [AiService](../src/main/java/org/example/board_cafe_kiosk_2603/ai/AiService.java)
- **이름 기반 연결**: [GameMapper.xml](../src/main/resources/mapper/admin/product/GameMapper.xml)의 `selectGameWithCategory`
- **과거 메뉴 정보**: [StatMapper.xml](../src/main/resources/mapper/admin/statistics/StatMapper.xml), [ADR 002](adr/002-menu-soft-delete.md)

[README](../README.md)
