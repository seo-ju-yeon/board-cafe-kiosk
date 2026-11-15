# 추가 확인 전후의 SecurityContext 처리

## 문제 상황

관리자는 아이디·비밀번호 확인 후 역할에 따른 추가 확인을 거치도록 구현했습니다.
시연 중 추가 확인이 끝나기 전에도 인증 상태가 남는 문제를 발견했습니다.

`SecurityContext`는 현재 사용자의 인증 정보를 담는 객체입니다.
아이디·비밀번호만 확인한 상태와 추가 확인까지 끝난 상태를 나누어 처리할 필요가 있었습니다.

## 적용한 처리

1차 로그인 후에는 추가 확인에 필요한 로그인 ID만 임시 보관하고 인증 컨텍스트를 제거하도록 구성했습니다.
추가 확인이 끝나면 사용자 정보를 다시 조회해 최종 인증 객체를 만듭니다.

```text
아이디·비밀번호 확인
  → 로그인 ID 임시 보관
  → 세션과 현재 스레드의 SecurityContext 제거
  → 역할별 추가 확인
  → 사용자 정보 재조회와 인증 객체 생성
  → 새 SecurityContext 저장, 임시 로그인 ID 제거
```

[ManagerLoginSuccessHandler](../../src/main/java/org/example/board_cafe_kiosk_2603/security/handler/ManagerLoginSuccessHandler.java#L53)는
로그인 ID를 세션의 `PRE_AUTH_USER`에 보관합니다.
이후 세션의 `SecurityContext` 속성을 제거하고 `SecurityContextHolder.clearContext()`를 호출합니다.

추가 확인 후 [LoginController의 completeLogin()](../../src/main/java/org/example/board_cafe_kiosk_2603/controller/admin/login/LoginController.java#L216)에서
사용자 정보로 인증 객체를 생성해 현재 스레드와 세션에 저장하고 임시 로그인 ID를 제거합니다.

## 남은 검증

수정 전 코드와 실행 로그가 없어 당시 오류의 정확한 발생 과정은 확인하기 어렵습니다.
수정 후 어떤 절차로 확인했는지도 기억이 명확하지 않아 다음 항목을 다시 검증해야 합니다.

- 추가 확인 전에는 보호된 관리자 화면에 접근할 수 없는지
- 추가 확인을 마치면 해당 역할에 허용된 화면에 접근할 수 있는지
- 추가 확인 실패·만료 시 인증이 완료되지 않는지

이 상태 전환을 검증하는 자동 테스트도 필요합니다.

[ADR 001. 인증 분리](../adr/001-authentication-design.md) · [Testing](../testing.md) · [Known Issues](../known-issues.md) · [README](../../README.md)
