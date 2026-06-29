# Board Wave

## 1. 프로젝트 개요

- 기간: 2026.03 ~ 2026.04
- 인원: 4명
- 목표: 보드게임 카페의 키오스크 주문, 테이블 세션, 결제, 게임 재고, 관리자 운영 기능을 하나의 웹 시스템으로 관리
- 담당 역할:
  - 상품, 카테고리, 게임 재고 관리 기능 구현
  - 관리자 로그인 및 보안 처리
  - 키오스크 테이블 로그인 제어
  - 이메일/OTP 기반 인증 흐름 구현
  - Spring AI와 pgvector를 활용한 보드게임 안내 기능 구현

Board Wave는 보드게임 카페에서 고객이 키오스크로 패키지 선택, 메뉴 주문, 포인트 사용, 결제를 진행하고, 관리자가 대시보드에서 테이블 상태, 주문, 상품, 게임 재고, 통계를 관리할 수 있도록 만든 팀 프로젝트입니다.

포트폴리오 제출 목적상 이 README는 구현 내용을 과장하지 않고, 직접 설명 가능한 담당 기능과 로컬 실행 조건을 중심으로 정리했습니다.

## 2. 기술 스택

- Language: Java 21
- Backend: Spring Boot 3.5.11, Spring MVC, Spring Security, Spring Batch, Spring WebSocket
- Frontend/View: Thymeleaf, HTML, CSS, JavaScript, Thymeleaf Layout Dialect
- Database: MariaDB, PostgreSQL, pgvector
- Data Access: MyBatis, JdbcTemplate
- AI: Spring AI, OpenAI, VectorStore
- Tools: Gradle, Lombok, ModelMapper
- External: Toss Payments, SMTP Mail

## 3. 주요 기능

- 키오스크 테이블 로그인
- 패키지 선택 및 테이블 이용 세션 시작
- 메뉴, 음료, 보드게임 주문
- 장바구니 관리
- 포인트 조회, 사용, 적립
- Toss Payments 기반 결제 흐름
- 관리자/직원 로그인
- 이메일 인증 및 OTP 기반 2차 인증
- 관리자 대시보드
- 카테고리, 메뉴, 보드게임, 게임 재고 CRUD
- 테이블 상태 및 주문 상태 관리
- WebSocket 기반 실시간 주문/메시지 처리
- Spring AI 기반 보드게임 안내 기능

## 4. 담당 기능

### 상품/카테고리/게임 재고 관리

- 카테고리 타입에 따라 메뉴와 게임 상품을 구분해 관리했습니다.
- 보드게임은 종목(`game`)과 실물 재고(`game_item`)를 분리해 관리했습니다.
- 게임 재고 상태를 `NORMAL`, `RENTED`, `DAMAGED`, `LOST`로 나누어 대여 가능 여부를 판단할 수 있도록 구성했습니다.
- 게임 정보가 변경될 때 AI 안내에 사용되는 벡터 데이터도 함께 갱신되도록 설계했습니다.
- 메뉴는 주문 이력 보존이 필요해 soft delete를 적용하고, 게임은 키오스크 노출 제어를 위해 활성/비활성 상태로 관리했습니다.
- 게임 실물 재고는 운영 중인 `NORMAL`, `RENTED` 상태에서는 삭제하지 못하게 제한하고, `DAMAGED`, `LOST` 상태만 삭제 가능하도록 처리했습니다.

### 로그인 및 보안 처리

- 키오스크 사용자와 관리자 사용자의 인증 방식이 달라 Spring Security의 `SecurityFilterChain`을 분리했습니다.
- 키오스크는 테이블 번호와 비밀번호 기반으로 로그인합니다.
- 관리자는 아이디/비밀번호 로그인 후 이메일 확인 또는 OTP 인증 흐름을 거칩니다.
- 권한은 `ADMIN`, `STAFF`, `SUPER`, `TABLE` 역할을 기준으로 분리했습니다.
- 관리자 1차 인증 성공 후에는 `PRE_AUTH_USER`만 세션에 임시 저장하고, 2차 인증 완료 전까지는 `SecurityContext`를 제거해 관리자 권한이 부여되지 않도록 했습니다.
- 키오스크는 테이블별 접근 격리가 필요하므로 URL의 테이블 번호와 세션의 테이블 번호를 비교해 다른 테이블 화면 접근을 차단했습니다.

### 이메일/OTP 인증

- 관리자 로그인, 비밀번호 찾기, 프로필 변경 등 보안이 필요한 흐름에 OTP 인증을 적용했습니다.
- OTP는 서버 메모리 저장소에서 관리하며, 입력값 검증 후 세션 상태를 갱신하는 방식으로 처리했습니다.
- 메일 발송 설정은 실제 계정 정보가 코드에 들어가지 않도록 환경 변수 기반으로 분리했습니다.
- 현재 OTP 저장소는 단일 서버 포트폴리오 환경을 기준으로 `ConcurrentHashMap`을 사용했습니다.
- 다중 서버 환경으로 확장한다면 Redis TTL 기반 저장소로 전환하는 것이 개선 방향입니다.

### Spring AI 기반 보드게임 안내

- MariaDB에 저장된 보드게임 데이터를 PostgreSQL pgvector에 임베딩해 저장했습니다.
- 사용자의 질문을 벡터 검색으로 관련 게임 정보와 매칭한 뒤, 검색된 정보만 기반으로 답변하도록 구성했습니다.
- 등록되지 않은 게임을 안내하지 않도록 RAG 흐름에서 컨텍스트가 없을 때의 응답을 제한했습니다.
- 동일 게임이 벡터 저장소에 중복 등록되지 않도록 메뉴 ID 기반의 고정 식별자를 사용해 갱신 기준을 잡았습니다.

### 설계 선택 요약

- MyBatis 선택: 카테고리, 활성 상태, 페이징 조건을 조합하는 동적 SQL과 집계 쿼리를 직접 제어하기 위해 선택했습니다.
- Service Interface 분리: Controller가 구현체가 아니라 서비스 계약에 의존하도록 구성해 테스트와 변경에 유리하게 만들었습니다.
- 로컬 파일 저장소 사용: 포트폴리오 단계에서는 S3 같은 외부 저장소보다 로컬 업로드 경로로 기능 검증을 우선했습니다.
- 환경 변수 분리: API Key, DB 비밀번호, 메일 앱 비밀번호는 코드에 직접 넣지 않고 `.env`에서 주입하도록 구성했습니다.

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

## 7. 주요 화면

현재 README에는 이미지 파일을 직접 포함하지 않았습니다. 포트폴리오 제출 시 아래 화면을 캡처해 `docs/images/`에 정리하는 것을 권장합니다.

- 관리자 로그인 및 OTP 인증
- 관리자 대시보드
- 카테고리/메뉴 관리
- 보드게임 재고 관리
- 키오스크 패키지 선택
- 키오스크 메뉴/장바구니
- 결제 화면
- AI 보드게임 안내 화면

예시 연결 방식:

```md
![관리자 대시보드](docs/images/admin-dashboard.png)
```

## 8. 트러블슈팅

### 8.1 OpenAI API Key 누락으로 서버 기동 실패

문제:

- Spring AI의 `OpenAiApi` Bean 생성 시 API Key가 없으면 애플리케이션 컨텍스트가 시작되지 않았습니다.

원인:

- `application-key.properties`는 `OPENAI_API_KEY` 환경 변수를 참조하지만, 로컬 `.env`에 값이 없으면 빈 값으로 주입됩니다.

해결:

- 실제 키는 코드에 직접 작성하지 않고 `.env`에만 저장했습니다.
- `.env.example`에는 예시값만 남겨 포트폴리오 제출 시 민감정보가 노출되지 않도록 분리했습니다.

### 8.2 MariaDB 계정 권한 문제로 서버 기동 실패

문제:

- `Access denied for user 'admin'@...` 오류로 Spring Batch의 `JobRepository` Bean 생성이 실패했습니다.

원인:

- 애플리케이션에서 사용하는 MariaDB 계정이 실제 DB에 없거나, 비밀번호 또는 Host 권한이 맞지 않았습니다.

해결:

- `01_init.sql`의 사용자 생성 및 권한 부여 구문을 root 또는 관리자 계정으로 먼저 실행해야 합니다.
- 이후 애플리케이션은 `.env`의 `MARIADB_USERNAME`, `MARIADB_PASSWORD` 값으로 MariaDB에 연결합니다.

### 8.3 키오스크/관리자 인증 체계 분리

문제:

- 키오스크 테이블 계정과 관리자 계정은 로그인 방식, 권한, 접근 URL이 달라 하나의 인증 흐름으로 처리하기 어려웠습니다.

해결:

- Spring Security에서 `SecurityFilterChain`을 분리했습니다.
- 키오스크 영역은 `/kiosk/**`, 관리자 영역은 `/admin/**`, `/login/**`, `/forgot-password/**` 중심으로 인증 규칙을 나누었습니다.

### 8.4 2차 인증 전 관리자 권한이 남는 문제

문제:

- 관리자 1차 로그인 직후 2차 인증을 완료하지 않았는데도 이전 `SecurityContext`가 세션에 남아 관리자 권한이 유지될 위험이 있었습니다.

원인:

- `SecurityContextHolder.clearContext()`만 호출하면 응답 처리 과정에서 기존 세션 컨텍스트가 다시 저장될 수 있습니다.

해결:

- 세션의 `SPRING_SECURITY_CONTEXT`를 먼저 제거한 뒤 `SecurityContextHolder.clearContext()`를 호출했습니다.
- 2차 인증 완료 후에는 DB에서 사용자 정보를 다시 로드하고 새 인증 토큰을 만들어 세션에 저장했습니다.

### 8.5 게임 필터, 탭, 페이징 조건 조합 누락

문제:

- 게임 목록에서 활성/비활성 탭과 카테고리 필터를 함께 사용할 때 특정 조건 조합이 누락될 수 있었습니다.

원인:

- Controller에서 탭 분기와 카테고리 분기를 별도로 처리하면 조합 케이스가 늘어날수록 누락 가능성이 커집니다.

해결:

- `isActive`, `categoryId`, `PageRequestDTO`를 함께 받는 단일 조회 흐름으로 정리했습니다.
- MyBatis XML의 `<if>` 동적 SQL을 사용해 카테고리 조건을 선택적으로 적용했습니다.

### 8.6 game_item JOIN으로 임베딩 데이터가 중복 생성되는 문제

문제:

- AI 안내용 게임 데이터를 임베딩할 때 같은 보드게임 정보가 여러 번 벡터 저장소에 들어갈 수 있었습니다.

원인:

- 임베딩 대상 게임을 조회할 때 `menu`, `game`, `game_item`을 함께 조회합니다.
- 하나의 게임에 `NORMAL` 상태의 실물 재고가 여러 개 있으면 `game_item`과의 1:N 관계 때문에 같은 게임이 재고 수만큼 여러 행으로 조회될 수 있습니다.

해결:

- 조회 결과를 바로 `Document`로 변환하지 않고, `menu_id`를 기준으로 중복을 제거했습니다.
- 이후 중복 제거된 데이터만 `Document`로 변환해 pgvector에 저장했습니다.
- `menu_id` 기반의 고정 UUID를 문서 ID로 사용해 같은 게임 정보는 새로 누적되지 않고 갱신되도록 했습니다.

결과:

- 같은 게임이 검색 결과에 반복 노출될 가능성을 줄였습니다.
- RAG 답변 품질은 LLM 호출뿐 아니라 검색 데이터 정제와 중복 제거에 영향을 받는다는 점을 확인했습니다.

### 8.7 RAG 검색 임계값 조정 문제

문제:

- AI 보드게임 안내 기능에서 한국어 음성 질문을 텍스트로 변환한 뒤 벡터 검색을 수행했을 때, 기대한 게임이 검색되지 않거나 검색 결과가 불안정한 경우가 있었습니다.

원인:

- 사용자의 질문은 짧은 구어체 문장인 경우가 많고, STT 결과도 항상 DB의 게임명이나 설명 문장과 정확히 일치하지 않았습니다.
- 처음에는 유사도 임계값을 높게 잡아 관련 게임까지 필터링되는 문제가 있었습니다.

해결:

- 실제 한국어 질문 예시를 기준으로 검색 결과를 확인하며 `similarityThreshold`를 완화했습니다.
- 검색 결과는 `topK`로 제한해 너무 많은 후보가 LLM 컨텍스트에 들어가지 않도록 했습니다.
- 검색 결과가 없을 때는 모델이 임의로 게임을 추천하지 않고, 등록된 게임이 없다는 고정 응답을 반환하도록 프롬프트를 제한했습니다.

결과:

- 짧은 한국어 질문에서도 관련 게임이 검색될 가능성을 높였습니다.
- 동시에 DB에 없는 게임을 안내하는 환각 가능성을 줄였습니다.

### 8.8 음성 입력 종료 시점 감지 문제

문제:

- AI 안내 기능에서 음성 입력이 끝나는 시점을 무음으로 감지하도록 구현했을 때, 테스트 환경에 따라 녹음 종료 결과가 달라졌습니다.

원인:

- 집처럼 조용한 환경에서는 무음 감지가 비교적 안정적으로 동작했지만, 학원처럼 주변 소음이 있는 환경에서는 배경 소음 때문에 무음 구간 판단이 흔들렸습니다.
- 사용 환경에 따라 마이크 입력 크기와 주변 소음 수준이 달라 자동 종료 기준을 하나로 고정하기 어려웠습니다.

해결:

- 자동 무음 감지 방식 대신 사용자가 버튼을 눌러 음성 인식을 시작하고, 말을 끝낸 뒤 다시 버튼을 눌러 녹음을 종료하는 방식으로 변경했습니다.

결과:

- 주변 소음에 덜 의존하게 되어 테스트 결과가 안정화되었습니다.
- 사용자가 직접 질문 시작과 종료 시점을 제어할 수 있어 키오스크 환경에 더 적합한 입력 흐름을 만들 수 있었습니다.

## 9. 보안 및 민감정보 관리

### 9.1 현재 안전하게 유지해야 하는 파일

- `.env`: 실제 로컬 실행 값이 들어가는 파일입니다. Git에 커밋하지 않습니다.
- `.env.example`: 제출 가능한 예시 파일입니다. 실제 키나 비밀번호를 넣지 않습니다.
- `application.properties`: 환경 변수 이름만 참조하고 실제 민감값은 넣지 않습니다.
- `application-key.properties`: OpenAI API Key를 직접 쓰지 않고 `${OPENAI_API_KEY:}` 형태로 참조합니다.
- `application-portfolio.properties`: 포트폴리오 시연용 키를 직접 쓰지 않고 환경 변수로 참조합니다.

### 9.2 제출 전 확인할 민감정보

- OpenAI API Key
- Toss Payments Secret Key
- DB 계정 비밀번호
- SMTP 메일 앱 비밀번호
- 개인 이메일 주소
- 개인 전화번호
- 실제 운영 계정의 아이디/비밀번호
- 임시 비밀번호 또는 OTP가 출력되는 로그
- 화면 캡처에 노출된 이메일, 전화번호, API Key

### 9.3 이번 포트폴리오 정리 기준

- `.env`는 커밋하지 않습니다.
- `.env.example`은 예시값만 유지합니다.
- SQL 더미 데이터는 포트폴리오 시연 계정과 초기 데이터 목적이 있으므로 이번 정리 범위에서는 수정하지 않습니다.
- README에는 실제 API Key, 실제 메일 비밀번호, 실제 Toss Key를 적지 않습니다.
- 실행에 필요한 계정 생성 절차는 README에 명확히 적되, 외부 서비스의 실제 비밀값은 노출하지 않습니다.

### 9.4 제출 전 점검 명령 예시

```bash
git status --short
```

`.env`가 표시되지 않아야 합니다.

```bash
rg -n "sk-|OPENAI_API_KEY=sk-|TOSS_PAYMENTS_SECRET_KEY=.*live|SPRING_MAIL_PASSWORD=.*[^=]$" .
```

실제 키로 보이는 값이 추적 파일에 들어갔는지 확인합니다.

```bash
rg -n "password|passwd|secret|api-key|token|otp" src/main README.md .env.example
```

민감정보 자체가 아니라, 민감정보를 다루는 코드와 문서가 적절히 마스킹되어 있는지 확인합니다.

## 10. 면접 시연 가이드

면접 또는 포트폴리오 설명 시에는 모든 기능을 길게 보여주기보다, 담당 기능 중심으로 짧게 흐름을 잡는 것이 좋습니다.

### 관리자 흐름

1. 관리자 로그인
2. 이메일 확인 또는 OTP 인증
3. 대시보드에서 테이블 상태와 주문 현황 확인
4. 메뉴/카테고리 관리 화면에서 상품 상태 변경
5. 게임 재고 화면에서 `NORMAL`, `RENTED`, `DAMAGED`, `LOST` 상태 차이 설명
6. 직원 또는 프로필 기능에서 OTP 인증이 필요한 흐름 설명

### 키오스크 흐름

1. 테이블 번호와 비밀번호로 로그인
2. 패키지 선택 후 세션 시작
3. 메뉴/게임 선택 및 장바구니 담기
4. 포인트 조회 또는 결제 흐름 설명
5. AI 안내 화면에서 등록된 게임 데이터 기반 답변 흐름 설명

### 설명 포인트

- 키오스크와 관리자의 인증 체계를 분리한 이유
- 2차 인증 전에는 관리자 권한을 주지 않도록 `SecurityContext`를 수동 제어한 이유
- 게임 종목과 실물 재고를 분리한 이유
- 주문 이력 보존을 위해 메뉴 삭제를 soft delete로 처리한 이유
- Spring AI 답변이 DB에 등록된 게임 정보를 기반으로 생성되도록 제한한 이유

## 11. 예상 질문

### 왜 `SecurityFilterChain`을 두 개로 분리했나요?

키오스크와 관리자는 URL, 로그인 방식, 권한 체계가 완전히 달랐습니다. 하나의 체인에서 조건문으로 처리하면 인증 흐름이 섞일 위험이 있어 `/kiosk/**`와 관리자 영역을 분리했습니다.

### OTP를 Redis가 아니라 인메모리로 구현한 이유는 무엇인가요?

이 프로젝트는 단일 서버 포트폴리오 환경을 기준으로 했기 때문에 Redis 인프라 없이 `ConcurrentHashMap`으로 3분 TTL과 1회성 검증을 구현했습니다. 다중 서버 환경에서는 Redis TTL 기반 저장소로 전환하는 것이 맞습니다.

### MyBatis를 사용한 이유는 무엇인가요?

활성/비활성 탭, 카테고리 필터, 페이징, 집계처럼 SQL 조건 조합을 직접 제어해야 하는 기능이 많았습니다. MyBatis의 XML과 `<if>` 동적 SQL이 이 요구에 잘 맞았습니다.

### 메뉴 삭제에 soft delete를 적용한 이유는 무엇인가요?

메뉴를 물리 삭제하면 과거 주문 이력에서 메뉴 정보를 설명하기 어려워집니다. 운영 이력과 통계 보존을 위해 `is_deleted` 기반 soft delete를 선택했습니다.

### 파일 업로드에 UUID 파일명을 사용한 이유는 무엇인가요?

파일명 충돌을 줄이고, 한글/특수문자 파일명의 인코딩 문제를 피하며, 원본 파일명 예측을 통한 직접 접근 위험을 낮추기 위해 UUID 기반 저장명을 사용했습니다.

### RAG 검색 임계값을 조정한 이유는 무엇인가요?

사용자의 한국어 음성 질문은 짧거나 구어체인 경우가 많아 DB의 게임 설명과 문장 형태가 정확히 일치하지 않았습니다. 임계값이 높으면 관련 게임도 검색에서 제외될 수 있어 실제 질문 예시를 기준으로 임계값을 완화하고, 대신 `topK`와 프롬프트 제한으로 답변 범위를 제어했습니다.

### 임베딩 데이터 중복 문제는 어떻게 해결했나요?

게임은 하나의 종목에 여러 실물 재고가 연결되는 구조라 `game_item`과 JOIN하면 같은 게임이 여러 행으로 조회될 수 있었습니다. 그래서 임베딩 전에 `menu_id` 기준으로 중복을 제거하고, `menu_id` 기반 고정 UUID를 문서 ID로 사용해 같은 게임 정보가 벡터 저장소에 반복 누적되지 않도록 했습니다.

### 음성 입력을 무음 감지에서 버튼 방식으로 바꾼 이유는 무엇인가요?

무음 감지는 조용한 환경에서는 동작했지만 주변 소음이 있는 환경에서는 종료 시점이 흔들렸습니다. 키오스크는 실제 매장 소음이 있을 수 있으므로, 사용자가 버튼으로 시작과 종료를 직접 제어하는 방식이 더 안정적이라고 판단했습니다.

## 12. 회고

이 프로젝트를 통해 단순 CRUD보다 실제 매장 운영 흐름에 가까운 도메인을 다뤄볼 수 있었습니다. 특히 키오스크 사용자와 관리자 사용자의 인증 흐름이 다르다는 점을 고려해 Spring Security 설정을 분리했고, 주문/세션/재고/결제 흐름이 서로 연결되는 구조를 경험했습니다.

또한 Spring AI와 pgvector를 활용하면서 외부 API Key, 벡터 저장소, 비즈니스 DB를 함께 다루는 실행 환경의 중요성을 체감했습니다. 포트폴리오 제출 단계에서는 기능 구현뿐 아니라 실행 조건, 민감정보 분리, 트러블슈팅 기록을 명확히 남기는 것이 프로젝트 신뢰도를 높인다는 점을 배웠습니다.
