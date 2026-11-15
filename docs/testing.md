# 테스트와 검증 상태

자동 테스트의 실행 결과는 재확인이 필요합니다.
개발 중 수동으로 확인한 내용과 아직 검증하지 못한 항목을 함께 정리했습니다.

## 테스트 구성

프로젝트에는 Mockito를 사용하는 단위 테스트와 실제 DB·외부 서비스를 사용하는 테스트가 섞여 있습니다.
일부 테스트는 결과를 로그로 출력하므로, 기대값을 비교하는 검증문을 보강해야 합니다.

| 테스트 | 확인하는 내용 | 한계와 준비 사항 |
| --- | --- | --- |
| [CafePackageServiceTest](../src/test/java/org/example/board_cafe_kiosk_2603/service/kiosk/CafePackageServiceTest.java) | Mapper를 Mockito로 대체하고 목록 크기, 빈 결과, 단건 조회 등을 검증 | 실제 DB 조회 SQL과 화면 흐름은 별도 검증 필요 |
| [MenuServiceImplTest](../src/test/java/org/example/board_cafe_kiosk_2603/service/admin/product/MenuServiceImplTest.java), [GameMapperTest](../src/test/java/org/example/board_cafe_kiosk_2603/repository/admin/product/GameMapperTest.java) | 실제 애플리케이션 구성에서 상품 기능 호출 | 결과를 주로 로그로 출력하며, DB 데이터를 변경하는 테스트 포함 |
| [FlywayMigrationTest](../src/test/java/org/example/board_cafe_kiosk_2603/config/FlywayMigrationTest.java) | 스키마 설치, 재시작 시 데이터 유지, 벡터 저장·조회 검증 | 검증용 파일 누락으로 실행 준비 필요. 가짜 임베딩을 사용하므로 실제 검색 품질은 검증하지 않음 |
| [MailSenderServiceTest](../src/test/java/org/example/board_cafe_kiosk_2603/service/admin/sms/MailSenderServiceTest.java) | 메일 발송 서비스 호출 | 실제 메일이 발송되므로 수신처와 테스트 환경 분리 필요 |

## 실행 방법

### 단위 테스트

JDK 21과 Gradle 의존성을 준비한 뒤, DB와 외부 서비스를 사용하지 않는 테스트를 선택해 실행할 수 있습니다.

```bash
./gradlew test --tests 'org.example.board_cafe_kiosk_2603.service.kiosk.CafePackageServiceTest'
```

실행 결과는 `build/reports/tests/test/index.html`과 `build/test-results/test`에서 확인합니다.

### DB·외부 서비스 연동 테스트

일부 `@SpringBootTest`는 실제 DB에 등록·수정·삭제를 수행하며 테스트 클래스에 롤백 설정이 없습니다.
전체 테스트를 실행하려면 먼저 다음 준비가 필요합니다.

1. 개발 데이터와 분리한 테스트 DB를 사용합니다.
2. 테스트 데이터를 준비하고 실행 후 정리하는 방법을 정합니다.
3. 메일·AI 호출은 가짜 구현으로 대체하거나 별도 연동 테스트로 분리합니다.

### Flyway 검증

[build.gradle](../build.gradle)은 일반 `test`에서 `flyway` 태그를 제외하고 별도 `flywayTest` 작업으로 실행하도록 구성되어 있습니다.
현재 다음 파일이 없어 검증 절차를 복구해야 합니다.

- `scripts/verify-flyway.py`: 테스트 DB 준비 스크립트
- `src/main/resources/sql/MariaDB/03_validate_reference_data.sql`: 초기 데이터 검증 SQL

두 파일과 테스트 DB 접속 환경 변수를 준비한 뒤 `flywayTest`를 실행할 수 있습니다.

## 수동으로 확인한 내용

| 사례 | 개발 중 확인한 내용 | 남은 검증 |
| --- | --- | --- |
| [음성 녹음](troubleshooting/001-voice-recording.md) | 버튼 종료 방식으로 바꾼 뒤 잡음이 많은 학원에서도 녹음을 종료할 수 있음을 확인 | 기기·브라우저별 호환성 |
| [신규 게임 재고 집계](troubleshooting/002-game-stock-count.md) | 기존 게임은 정상 집계되지만 신규 게임은 집계되지 않는 문제를 발견해 SQL 수정 | 수정 후 데이터·수량의 상세 기록이 없어 재확인 필요 |
| [RAG 검색 기준](troubleshooting/003-rag-similarity.md) | 임베딩 저장을 확인한 뒤 기준을 `0.7`에서 `0.3`으로 낮춰 관련 검색 결과 확인. 이후 “종치는 게임이 뭐지?”라는 질문에도 게임 안내 확인 | 질문별 검색 결과와 점수 비교, 정확도 평가 |
| [SecurityContext](troubleshooting/004-security-context.md) | 추가 확인 전에도 인증 상태가 남는 문제를 시연 중 발견 | 수정 후 확인 방식이 불명확해 인증 단계별 접근 제한 재검증 필요 |

## 추가할 검증

- **인증**: 추가 확인 전후의 접근 여부, 실패·만료 시 상태, 역할별 확인 절차
- **재고**: 신규·기존 게임에 정상·대여·파손·분실 재고를 섞었을 때 정상 재고 수만 집계되는지
- **Soft Delete**: 삭제 후 일반 목록에서 제외되고 과거 통계에 필요한 메뉴 정보가 남는지
- **RAG 갱신**: 게임 설명·판매 상태 변경 반영과 벡터 저장 실패 후 복구
- **RAG 검색**: 게임명·특징·인원 질문과 무관한 질문의 검색 결과 비교
- **음성 입력**: 시작·종료 반복, 마이크 권한 거절, 네트워크 오류 후 재입력

[Known Issues](known-issues.md) · [README](../README.md)
