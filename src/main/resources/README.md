# Board Wave

보드게임 카페 운영을 위한 통합 키오스크 및 관리자 대시보드 프로젝트입니다.  
키오스크 주문, 패키지/정산, 포인트, 보드게임 대여, 실시간 주문 처리, 관리자 통계와 운영 기능을 하나의 시스템으로 구현했습니다.

## 프로젝트 개요

Board Wave는 보드게임 카페 환경에 맞춘 웹 기반 운영 시스템입니다.

- 고객은 키오스크에서 인원 선택, 전화번호 등록, 패키지 선택, 메뉴 주문, 결제를 진행할 수 있습니다.
- 직원과 관리자는 대시보드에서 테이블 상태, 실시간 주문, 서비스 요청, 정산, 상품/게임/직원/통계를 관리할 수 있습니다.
- 주문 상태와 서비스 요청은 실시간으로 반영되며, 결제와 포인트 적립/사용 흐름까지 하나로 연결됩니다.

## 주요 기능

### 키오스크

- 테이블 로그인 및 세션 시작
- 인원 선택, 전화번호 등록, 패키지 선택
- 음식/음료/게임 장바구니 및 주문
- 포인트 조회, 사용, 적립
- 토스페이먼츠 기반 결제
- 서비스 요청 메시지 전송
- AI 기반 TTS/STT 챗봇 기능

### 관리자/직원

- Spring Security 기반 로그인
- 이메일 OTP 기반 2차 인증
- 실시간 대시보드
- 주문 상태 관리
- 테이블 메시지 응대
- 메뉴/게임/재고 관리
- 패키지 및 가격 정책 관리
- 직원 계정 및 권한 관리
- 매출/방문/인기 상품 통계

### 보드게임 카페 도메인 기능

- `table_session` 기반 방문 세션 관리
- `orders`, `order_item` 기반 주문 처리
- `game`, `game_item`, `game_history` 기반 게임 대여 이력 관리
- `point`, `point_history` 기반 포인트 적립/사용 관리
- `payment` 기반 세션 단위 정산 관리

## 기술 스택

### Backend

- Java 21
- Spring Boot 3.5.11
- Spring MVC
- Spring Security
- Spring WebSocket / STOMP
- Spring Batch
- Spring Mail
- MyBatis
- ModelMapper
- Lombok

### Frontend

- Thymeleaf
- HTML / CSS / JavaScript
- Thymeleaf Layout Dialect

### Database

- MariaDB
- PostgreSQL + PGVector

### External / AI

- Toss Payments
- Solapi
- Spring AI
- OpenAI

## 시스템 구성

프로젝트는 크게 3개 영역으로 나뉩니다.

- `common`: 로그인, 공통 진입, 보안 보조 흐름
- `kiosk`: 고객용 키오스크 화면, 주문, 결제, 포인트, 메시지, AI 기능
- `admin`: 대시보드, 상품/게임/정책/계정/통계 관리

코드 기준 주요 디렉터리:

- `src/main/java/org/example/board_cafe_kiosk_2603/controller`
- `src/main/java/org/example/board_cafe_kiosk_2603/service`
- `src/main/java/org/example/board_cafe_kiosk_2603/mapper`
- `src/main/resources/templates`
- `src/main/resources/static`
- `src/main/resources/mapper`
- `src/main/resources/sql/MariaDB`

## 주요 화면 및 흐름

### 키오스크 기본 흐름

1. 테이블 로그인
2. 인원 수 선택
3. 전화번호 등록 또는 건너뛰기
4. 패키지 선택
5. 메뉴/게임 주문
6. 장바구니 확인
7. 포인트 조회 및 적용
8. 결제
9. 주문 처리 및 세션 종료

### 관리자 운영 흐름

1. 관리자/직원 로그인
2. 이메일 인증
3. 대시보드 진입
4. 테이블 상태 및 주문 현황 확인
5. 주문 상태 변경 또는 메시지 응대
6. 정산/통계/정책 관리

## API 구성

산출물 기준으로 다음 영역의 API가 정의되어 있습니다.

- 키오스크 진입 / 세션
- 패키지 선택
- 장바구니
- 주문
- 포인트 조회
- 서비스 요청 / 메시지
- AI TTS / STT / 챗봇

REST API와 서버 렌더링 화면이 혼합된 구조이며, 인증은 Spring Security와 HttpSession을 사용합니다.

## 데이터베이스

문서 기준 총 22개 테이블로 구성되어 있습니다.

대표 테이블:

- `manager`
- `cafe_table`
- `customer`
- `category`
- `cafe_package`
- `table_session`
- `menu`
- `orders`
- `order_item`
- `game`
- `game_item`
- `cart`
- `cart_item`
- `game_history`
- `payment`
- `point`
- `point_history`
- `macro_message`
- `table_message`
- `item_sales_history`
- `daily_sales_summary`
- `persistent_logins`

초기 스키마와 더미 데이터는 아래 경로에 있습니다.

- [01_init.sql](/C:/dev/board_cafe_kiosk_2603/src/main/resources/sql/MariaDB/01_init.sql)
- [02_dummy.sql](/C:/dev/board_cafe_kiosk_2603/src/main/resources/sql/MariaDB/02_dummy.sql)

## 실행 환경

### 요구 사항

- JDK 21
- MariaDB
- PostgreSQL
- Gradle

### 설정 파일

설정 항목 예시:

- MariaDB 연결 정보
- PostgreSQL / PGVector 연결 정보
- Toss Payments 키
- 메일 서버 설정
- OpenAI / Spring AI 설정
- 업로드 경로 설정

## 실행 방법

1. MariaDB에 `01_init.sql`, `02_dummy.sql`을 적용합니다.
2. 필요한 외부 설정 파일과 키 값을 준비합니다.
3. Gradle로 애플리케이션을 실행합니다.

```bash
./gradlew bootRun
```

Windows 환경에서는:

```powershell
.\gradlew.bat bootRun
```

## 테스트

기본 테스트는 Spring Boot Test, MyBatis Test, Spring Security Test, Spring Batch Test를 사용합니다.

```bash
./gradlew test
```

Windows 환경에서는:

```powershell
.\gradlew.bat test
```

## 프로젝트 문서

프로젝트 산출물에는 다음 문서가 포함되어 있습니다.

- 기능 설명서
- 요구사항 정의서
- 유스케이스
- API 명세서
- 데이터베이스 정의서
- 시연 영상

README 작성 참고 자료:

- `보드게임 카페 기능 설명서`
- `보드게임 카페 요구사항 정의서`
- `보드게임 카페 유스케이스`
- `보드게임 카페 API 명세서`
- `보드게임 카페 데이터베이스 정의서`
- `보드게임 카페 시연 영상`

## 팀
참여자:

- 서주연
- 강수현
- 김민기
- 서민성
- 최종현

## 한 줄 소개

Board Wave는 보드게임 카페의 주문, 정산, 대여, 포인트, 실시간 운영 관리를 하나로 통합한 스마트 키오스크 솔루션입니다.
