# Board Wave

## 1. 프로젝트 개요

- 기간: 2026.03 ~ 2026.04
- 인원: 4명
- 구분: 코리아IT아카데미 대구 응용 SW 엔지니어링 과정 팀 프로젝트
- 목표: 보드게임 카페의 키오스크 주문, 테이블 세션, 결제, 게임 재고, 관리자 운영 기능을 하나의 웹 시스템으로 관리
- 담당 역할:
  - 키오스크·관리자 로그인 구현
  - 관리자 이메일·OTP 추가 인증과 인증 상태 전환 구현
  - 상품·카테고리·게임 및 실물 재고 등록·관리
  - 게임 등록·수정 시 키오스크 메뉴 데이터 생성·수정
  - STT, RAG, LLM, TTS를 포함한 AI 게임 안내 백엔드 구현
  - 마이크 녹음, 텍스트 보정, 가상 키보드를 포함한 AI 안내 화면 구현

Board Wave는 보드게임 카페에서 고객이 키오스크로 패키지 선택, 메뉴 주문, 포인트 사용, 결제를 진행하고, 관리자가 대시보드에서 테이블 상태, 주문, 상품, 게임 재고, 통계를 관리할 수 있도록 만든 팀 프로젝트입니다.

포트폴리오 제출 목적상 이 README는 구현 내용을 과장하지 않고, 직접 설명 가능한 담당 기능과 로컬 실행 조건을 중심으로 정리했습니다.

로컬 개발 환경에서 기능 동작을 확인했으며, 실제 배포와 별도 성능 측정은 진행하지 않았습니다. 정식 PR과 코드 리뷰 대신 기능 브랜치에서 작업한 뒤 담당자와 실행 결과를 확인하며 `dev`, `main` 브랜치로 병합했습니다.

## 2. 기술 스택

- Language: Java 21
- Backend: Spring Boot 3.5.11, Spring MVC, Spring Security, Spring Batch, Spring WebSocket
- Frontend/View: Thymeleaf, HTML, CSS, JavaScript, Thymeleaf Layout Dialect
- Database: MariaDB, PostgreSQL, pgvector
- Data Access: MyBatis, JdbcTemplate
- AI: Spring AI, OpenAI, VectorStore
- Tools: Gradle, Lombok, ModelMapper
- External: Toss Payments, SMTP Mail

## 3. 프로젝트 주요 기능

Board Wave는 고객이 사용하는 키오스크와 매장 관리 화면으로 나누어 개발했습니다. 아래는 프로젝트에 포함된 전체 기능이며, 제가 맡은 부분은 다음 항목에서 따로 정리했습니다.

- 키오스크: 테이블 로그인, 패키지 선택과 이용 시작, 메뉴·보드게임 주문, 장바구니, 포인트, 결제
- 매장 관리: 관리자·직원 로그인, 이메일·OTP 추가 인증, 대시보드, 테이블·주문 상태 관리
- 상품 관리: 카테고리, 메뉴, 보드게임과 실물 재고 관리
- 실시간 처리: WebSocket을 이용한 주문·메시지 처리
- 게임 안내: Spring AI를 이용한 보드게임 안내

## 4. 담당 기능

### 상품/카테고리/게임 재고 관리

- 카테고리 타입에 따라 메뉴와 게임 상품을 구분해 관리했습니다.
- 보드게임은 종목(`game`)과 실물 재고(`game_item`)를 분리해 관리했습니다.
- 게임을 등록할 때 실물 재고의 일련번호와 `NORMAL`, `DAMAGED`, `LOST` 상태를 입력할 수 있도록 구현했습니다.
- 게임 등록·수정 시 같은 이름의 키오스크 메뉴 데이터를 생성·수정하고 설명 변경을 함께 반영했습니다.
- 게임·메뉴·일부 재고 상태가 바뀌면 조건을 다시 확인해 벡터 문서의 갱신 또는 삭제를 시도했습니다.
- 팀 논의에서 과거 통계 조회에 필요한 메뉴 정보는 삭제 후에도 남겨야 한다는 의견을 반영해 메뉴에 soft delete를 적용했습니다.
- 카테고리는 아직 삭제 처리하지 않은 연결 메뉴가 없을 때만 실제 삭제하고, 게임은 활성 상태 변경과 실제 삭제 기능을 구분했습니다.

### 로그인 및 보안 처리

- 키오스크 사용자와 관리자 사용자의 인증 방식이 달라 Spring Security의 `SecurityFilterChain`을 분리했습니다.
- 키오스크는 테이블 번호와 비밀번호 기반으로 로그인합니다.
- 관리자는 아이디/비밀번호 로그인 후 이메일 확인 또는 OTP 인증 흐름을 거칩니다.
- 권한은 `ADMIN`, `STAFF`, `SUPER`, `TABLE` 역할을 기준으로 분리했습니다.
- 관리자 1차 인증 성공 후에는 `PRE_AUTH_USER`만 세션에 임시 저장하고, 추가 인증 완료 전까지는 세션과 현재 스레드의 `SecurityContext`를 제거했습니다.
- 이메일 또는 OTP 확인이 끝난 뒤 사용자 정보를 다시 조회하고 인증 객체를 만들어 세션에 저장했습니다.

### 이메일/OTP 인증

- 관리자 로그인, 비밀번호 찾기, 프로필 변경 등 보안이 필요한 흐름에 OTP 인증을 적용했습니다.
- OTP는 서버 메모리 저장소에서 3분 유효 시간과 함께 관리합니다. 만료 후 검증을 시도하거나 검증에 성공하면 저장소에서 제거합니다.
- 메일 발송 설정은 실제 계정 정보가 코드에 들어가지 않도록 환경 변수 기반으로 분리했습니다.
- 현재 OTP 저장소는 `ConcurrentHashMap`을 사용하므로 애플리케이션 프로세스 사이에 인증 상태를 공유하지 않습니다.
- 팀 개발 중 후속 기능을 반복 확인하기 위한 개발·시연용 고정 OTP 경로가 있습니다. 외부 환경에서는 제거하거나 로컬 개발 프로필에서만 활성화하도록 분리해야 합니다.

### 매장 DB 기반 RAG 게임 안내

- MariaDB에서 GAME 카테고리이고 게임이 활성 상태이며, 메뉴가 판매 가능하고 삭제 처리되지 않았고 `NORMAL` 재고가 하나 이상 있는 게임을 조회했습니다.
- 조회한 게임명, 설명, 플레이 인원과 시간을 자연어 형태의 Spring AI `Document`로 변환해 PostgreSQL pgvector에 저장했습니다.
- 사용자 질문과 유사한 게임을 상위 3개, 유사도 기준 0.3으로 검색해 LLM 답변의 컨텍스트로 제공했습니다.
- 검색 결과가 없을 때는 등록된 게임이 없다는 고정 응답을 사용하도록 프롬프트를 제한했습니다.
- `menu_id`로 만든 고정 UUID를 문서 ID로 사용해 같은 메뉴의 문서를 갱신하거나 삭제할 수 있도록 했습니다.
- `NORMAL → RENTED` 변경은 벡터 문서에 즉시 반영하지 않으며, 벡터 갱신 실패에 대한 재시도와 검색 정확도 측정은 구현하지 않았습니다.

### AI 안내 화면

- 음성 질문을 STT로 변환하고, 인식된 문장을 사용자가 수정해 다시 질문할 수 있도록 했습니다.
- 마이크 녹음, 텍스트 보정, 가상 키보드와 TTS 답변 재생 화면을 구현했습니다.
- 개인 개발 환경에서는 무음 감지 방식이 동작했지만 학원 시연 중 주변 소음으로 녹음이 끝나지 않는 문제를 확인했습니다.
- 팀원들과 논의한 뒤 마이크 버튼을 다시 눌러 녹음을 종료하는 방식으로 변경했습니다.

### 팀 전체 기능과 연결되는 부분

- 주문 게임에 `game_item` 일련번호를 배정하고 현재 테이블 주문인지 검증하는 기능은 주문·결제 흐름 담당 팀원이 구현했습니다.
- 대여·반납 시 `game_history`를 기록하고 `NORMAL`, `DAMAGED`, `LOST` 상태로 정리하는 기능도 해당 팀원이 구현했습니다.
- 주문, 결제, 포인트, 통계, WebSocket 주문 알림은 프로젝트 전체 기능이며 본인 담당 기능과 구분합니다.

### 구현 구성 요약

- MyBatis Mapper와 XML에서 상품 필터, 페이징과 통계 집계 SQL을 관리합니다.
- 상품 관리 영역은 Service Interface와 구현체를 분리했고, 일부 다른 영역은 Service 클래스를 직접 사용합니다.
- 업로드 이미지는 `my.upload.path`로 지정한 로컬 경로에 UUID 파일명으로 저장합니다.
- OpenAI, Toss Payments와 SMTP 비밀값은 `.env`의 환경 변수로 주입합니다.
- MariaDB의 `admin` 계정과 비밀번호 `0331`은 로컬 실행·시연용 SQL에 포함되어 있으므로 외부 환경에서 그대로 사용하지 않아야 합니다.

## 5. 프로젝트 구조

```text
src/main/java/org/example/board_cafe_kiosk_2603
├── ai              # Spring AI, RAG, 게임 임베딩
├── config          # Security, DB, Mail, WebSocket, Batch 설정
├── controller      # 관리자/키오스크/공통 요청 처리
├── domain          # 도메인 객체
├── dto             # 요청/응답 DTO
├── mapper          # MyBatis Mapper 인터페이스
├── scheduler       # 통계 스케줄러
├── security        # UserDetailsService, 인증 성공 핸들러, 인가 처리
├── service         # 비즈니스 로직
└── websocket       # WebSocket 핸들러

src/main/resources
├── mapper          # MyBatis XML
├── sql             # MariaDB/PostgreSQL 초기화 SQL
├── static          # CSS, JavaScript
└── templates       # Thymeleaf 화면
```

## 6. 실행 방법

### 6.1 사전 요구사항

- JDK 21
- MariaDB
- PostgreSQL
- PostgreSQL pgvector 확장
- OpenAI API Key
- Gradle Wrapper

결제와 메일 기능까지 확인하려면 아래 값도 필요합니다.

- Toss Payments Secret Key / Client Key
- SMTP 메일 계정 및 앱 비밀번호

### 6.2 환경 변수 파일 준비

이 프로젝트는 `application.properties`에서 로컬 `.env` 파일을 읽습니다.

```properties
spring.config.import=optional:file:.env[.properties]
```

`.env`는 로컬 실행용 파일이며, `.gitignore`에 포함되어 Git에 커밋되지 않습니다.

```gitignore
.env
.env.*
!.env.example
```

처음 실행할 때는 `.env.example`을 참고해 프로젝트 루트에 `.env`를 만들고 값을 채웁니다.

필수 실행 값:

```properties
MARIADB_URL=jdbc:mariadb://localhost:3306/board_cafe_kiosk_2603
MARIADB_USERNAME=admin
MARIADB_PASSWORD=0331

PGVECTOR_URL=jdbc:postgresql://localhost:5432/board_cafe_kiosk_2603
PGVECTOR_USERNAME=your-postgres-username
PGVECTOR_PASSWORD=your-postgres-password

OPENAI_API_KEY=your-openai-api-key
```

선택 기능 값:

```properties
TOSS_PAYMENTS_SECRET_KEY=your-toss-secret-key
TOSS_PAYMENTS_CLIENT_KEY=your-toss-client-key

SPRING_MAIL_USERNAME=your-mail@example.com
SPRING_MAIL_PASSWORD=your-mail-app-password
MYAPP_MAIL_FROM=your-mail@example.com
MYAPP_MAIL_FROM_NAME=BOARD_WAVE_SYSTEM

PORTFOLIO_SUPER_KEY_ID=your-demo-super-id
PORTFOLIO_SUPER_KEY_OTP=your-demo-super-otp
PORTFOLIO_SUPER_KEY_TEMP_PASSWD=your-demo-temp-password
```

### 6.3 MariaDB 초기화

`src/main/resources/sql/MariaDB/01_init.sql`에는 데이터베이스, 테이블, 애플리케이션용 MariaDB 계정 생성 구문이 포함되어 있습니다.

계정 생성과 권한 부여는 MariaDB root 또는 관리자 권한 계정으로 먼저 실행해야 합니다.

```sql
CREATE DATABASE IF NOT EXISTS `board_cafe_kiosk_2603`;

CREATE USER IF NOT EXISTS `admin`@`%` IDENTIFIED BY '0331';
GRANT ALL PRIVILEGES ON `board_cafe_kiosk_2603`.* TO `admin`@`%`;
FLUSH PRIVILEGES;
```

이미 `admin` 계정이 있는데 비밀번호가 다르다면 아래처럼 재설정합니다.

```sql
ALTER USER `admin`@`%` IDENTIFIED BY '0331';
GRANT ALL PRIVILEGES ON `board_cafe_kiosk_2603`.* TO `admin`@`%`;
FLUSH PRIVILEGES;
```

그 다음 아래 순서로 SQL을 적용합니다.

1. `src/main/resources/sql/MariaDB/01_init.sql`
2. `src/main/resources/sql/MariaDB/02_dummy.sql`

`02_dummy.sql`은 포트폴리오 시연을 위한 초기 데이터가 포함되어 있습니다.

### 6.4 PostgreSQL/pgvector 초기화

PostgreSQL은 AI 안내 기능의 벡터 저장소로 사용합니다.

```text
src/main/resources/sql/PostgresSQL/01_init.sql
```

PostgreSQL 데이터베이스 이름은 기본적으로 MariaDB와 동일하게 `board_cafe_kiosk_2603`을 사용합니다.

### 6.5 애플리케이션 실행

```bash
./gradlew bootRun
```

Windows:

```powershell
.\gradlew.bat bootRun
```

실행 후 기본 접속 주소:

```text
http://localhost:8080
```

### 6.6 실행 실패 시 먼저 확인할 것

- `OpenAI API key must be set`: `.env`의 `OPENAI_API_KEY` 확인
- `Access denied for user`: `.env`의 `MARIADB_USERNAME`, `MARIADB_PASSWORD`와 MariaDB 계정 권한 확인
- `Unknown database`: `board_cafe_kiosk_2603` 데이터베이스 생성 여부 확인
- `Connection refused`: MariaDB 또는 PostgreSQL 서버 실행 여부 확인
- `relation/vector table does not exist`: PostgreSQL pgvector 초기화 여부 확인
- 포트 충돌: `8080` 포트를 사용하는 다른 프로세스 종료 또는 서버 포트 변경

## 7. 문제 해결과 설계 판단

실제 동작 중 확인한 문제와 구현 과정에서 고려한 설계를 구분해 정리했습니다.

### 7.1 추가 인증 완료 전 인증 정보가 남는 문제

문제 상황:

- 관리자 1차 로그인 직후 추가 인증을 완료하지 않았는데도 이전 `SecurityContext`가 세션에 남아 관리자 권한이 유지될 수 있었습니다.

확인한 원인:

- `SecurityContextHolder.clearContext()`만 호출하면 응답 처리 과정에서 기존 세션 컨텍스트가 다시 저장될 수 있었습니다.

변경한 내용:

- 세션의 `SPRING_SECURITY_CONTEXT`를 먼저 제거한 뒤 `SecurityContextHolder.clearContext()`를 호출했습니다.
- 추가 인증 완료 후에는 DB에서 사용자 정보를 다시 조회하고 새 인증 토큰을 만들어 세션에 저장했습니다.

### 7.2 탭·필터가 적용된 게임 목록에서 재고 수가 0으로 표시된 문제

문제 상황:

- 게임 관리 화면을 하나의 HTML 페이지 안에서 활성·비활성 탭과 카테고리 필터로 전환하도록 구성했습니다.
- 게임, 카테고리, 메뉴와 실물 재고 테이블을 JOIN해 조회했을 때 게임별 대여 가능 재고 수가 0으로 표시되는 문제를 확인했습니다.

확인한 원인:

- 초기 조건 집계식이 JOIN 결과에서 `NORMAL` 상태의 재고 행을 원하는 방식으로 집계하지 못하고 있었습니다.

변경한 내용:

- 대여 가능한 게임 재고 수를 `COUNT(CASE WHEN gi.status = 'NORMAL' THEN 1 END)`로 집계하도록 변경했습니다.
- 활성 상태와 선택한 카테고리를 함께 조회할 수 있도록 MyBatis `<if>` 조건을 적용했습니다.
- 수정 후 탭과 카테고리 필터를 바꿔가며 게임별 재고 수가 표시되는 것을 화면에서 확인했습니다.

### 7.3 MariaDB의 변경을 AI 검색 데이터에 반영하는 방식

고려한 점:

- 게임 정보를 등록하거나 수정할 때 MariaDB의 데이터와 PostgreSQL pgvector의 문서를 연결해 갱신할 기준이 필요했습니다.

적용한 방식:

- `menu_id`로 고정 UUID를 생성해 같은 메뉴가 항상 같은 문서 ID를 사용하도록 했습니다.
- 게임 정보를 갱신할 때 기존 문서를 삭제한 뒤 현재 정보를 다시 저장합니다.
- 판매 불가, 삭제, 게임 비활성 또는 `NORMAL` 재고 없음으로 조회 조건을 충족하지 않으면 해당 문서를 삭제합니다.

현재 한계:

- `NORMAL` 재고가 대여 상태로 바뀌는 시점에는 벡터 문서를 즉시 갱신하지 않습니다.
- 벡터 저장 실패 시 로그만 남기며 자동 재시도나 MariaDB와 pgvector 사이의 일관성 보장 기능은 구현하지 않았습니다.

### 7.4 검색되지 않던 게임 정보의 유사도 기준 조정

문제 상황:

- AI 보드게임 안내 기능에서 한국어 질문으로 벡터 검색을 수행했을 때 초기 유사도 기준인 0.7에서는 게임이 검색되지 않는 경우가 있었습니다.

확인한 원인:

- 사용자의 질문은 짧은 구어체일 수 있고, STT 결과도 DB의 게임명이나 설명 문장과 정확히 일치하지 않을 수 있었습니다.

변경한 내용:

- `similarityThreshold`를 0.7에서 0.3으로 낮추고 검색 결과를 상위 3개로 제한했습니다.
- 개발 중 실제 질문을 입력해 화면에서 검색 여부와 답변을 확인했습니다.
- 검색 결과가 없을 때는 등록된 게임이 없다는 고정 응답을 반환하도록 프롬프트를 작성했습니다.

확인 범위:

- 별도의 평가 데이터셋이나 검색 정확도 지표를 사용하지 않았으므로 정확도 또는 성능 개선으로 표현하지 않습니다.
- 프롬프트로 답변 범위를 제한했지만 LLM 응답을 완전히 보장하는 장치로 보지는 않습니다.

### 7.5 시연 환경에서 녹음이 종료되지 않던 문제

문제 상황:

- 개인 개발 환경에서는 무음 감지로 녹음이 종료됐지만, 학원에서 팀원들에게 시연할 때는 주변 소음으로 녹음이 계속되어 검색 결과까지 확인하지 못했습니다.

확인한 원인:

- 무음 구간을 기준으로 자동 종료하는 방식이 주변 소음의 영향을 받았습니다.

변경한 내용:

- 문제를 확인한 뒤 팀원들과 논의해 사용자가 버튼을 눌러 녹음을 시작하고 다시 눌러 종료하는 방식으로 변경했습니다.
- STT로 변환된 문장을 화면에서 수정한 뒤 다시 질문할 수 있도록 했습니다.

## 8. 회고

이 프로젝트에서는 키오스크·관리자 인증, 상품·카테고리·게임 재고 관리와 매장 DB 기반 AI 게임 안내를 맡았습니다. 관리자 1차 로그인과 추가 인증 사이의 인증 상태를 직접 다루면서 Spring Security의 세션 인증 흐름을 구체적으로 확인할 수 있었습니다.

또한 MariaDB의 구조화된 게임 데이터를 Spring AI `Document`로 바꾸고 pgvector 검색 결과를 LLM 컨텍스트로 전달하는 과정을 구현했습니다. 검색 임계값을 조정하거나 음성 입력 방식을 바꾸는 과정에서는 개발 환경에서 동작한 기능도 다른 환경에서 직접 확인하고 수정할 필요가 있다는 점을 경험했습니다.

주문·결제와 대여·반납 처리는 다른 팀원이 담당했지만, 제가 구현한 게임·메뉴·벡터 데이터가 해당 기능과 연결되는 구조를 함께 확인했습니다. 실제 배포와 성능 측정까지 진행하지 못한 점, 벡터 데이터 갱신에 재시도와 일관성 보장 기능이 없는 점은 이후 보완할 부분입니다.
