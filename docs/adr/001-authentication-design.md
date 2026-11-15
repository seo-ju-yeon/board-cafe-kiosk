# ADR 001. 키오스크와 관리자 인증 흐름 분리

## 배경

Board Wave는 키오스크와 관리자 화면을 하나의 애플리케이션에서 제공합니다.
두 화면은 로그인에 사용하는 정보와 로그인 이후의 흐름이 다릅니다.

| 구분 | 키오스크 | 관리자 화면 |
| --- | --- | --- |
| 로그인 정보 | 테이블 번호와 비밀번호 | 관리자 계정과 비밀번호 |
| 로그인 이후 | 테이블을 기준으로 키오스크 이용 | 역할에 따라 이메일 주소 또는 OTP 확인 |
| 주요 권한 | `TABLE` | `ADMIN`, `STAFF`, `SUPER` |

초기 설계부터 두 로그인 흐름을 따로 관리하기로 했습니다.

## 선택한 방법과 이유

Spring Security의 `SecurityFilterChain`을 키오스크용과 관리자용으로 나눴습니다.
필터 체인은 요청에 적용할 로그인 처리와 접근 권한 규칙을 묶은 설정입니다.

각 체인에 사용자 조회 서비스, 로그인 페이지, 로그인 성공 핸들러를 따로 설정했습니다.
이렇게 하면 테이블 로그인과 관리자 로그인의 처리 과정과 권한 규칙을 각각 확인하고 수정할 수 있습니다.

관리자는 아이디·비밀번호 확인 후 다음 절차를 거칩니다.

- `STAFF`: 입력한 이메일 주소와 DB에 등록된 주소를 비교합니다.
- `ADMIN`, `SUPER`: 이메일로 발송한 OTP를 확인합니다.

`STAFF`의 확인 방식은 이메일 수신 여부를 확인하지 않는다는 한계가 있습니다.
추가 확인 전후의 인증 상태 처리는 [SecurityContext 구현 기록](../troubleshooting/004-security-context.md)에 정리했습니다.

## 장점과 주의할 점

- 사용자 유형별 로그인 설정과 성공 후 처리를 각각 관리할 수 있습니다.
- 요청 경로에 맞는 체인이 적용되는지 확인해야 합니다.
- 공통 설정을 수정할 때 두 체인에 필요한 변경이 모두 반영됐는지 확인해야 합니다.

## 남은 검증

추가 확인 전후의 접근 제한과 역할별 흐름을 검증하는 자동 테스트가 필요합니다.
인증 예외와 API 접근 규칙도 점검할 항목으로 남아 있습니다.

## 관련 코드

- [SecurityConfig](../../src/main/java/org/example/board_cafe_kiosk_2603/config/SecurityConfig.java): `kioskChain`, `adminChain`
- [ManagerLoginSuccessHandler](../../src/main/java/org/example/board_cafe_kiosk_2603/security/handler/ManagerLoginSuccessHandler.java): 역할별 추가 확인 화면으로 이동
- [LoginController](../../src/main/java/org/example/board_cafe_kiosk_2603/controller/admin/login/LoginController.java): 이메일 주소·OTP 확인과 최종 로그인 처리

[Testing](../testing.md) · [Known Issues](../known-issues.md) · [README](../../README.md)
