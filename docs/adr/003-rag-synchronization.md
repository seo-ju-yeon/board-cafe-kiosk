# ADR 003. 게임 정보와 AI 검색 데이터 연결

## 배경

게임 정보는 MariaDB에 저장하고, AI 검색용 문서와 벡터는 PostgreSQL의 pgvector에 저장합니다.
관리자가 게임 설명을 수정하면 AI 안내에도 반영되어야 하므로, 운영 데이터와 검색 문서를 연결할 기준이 필요했습니다.

## 선택한 방법과 이유

### 같은 메뉴는 같은 문서 ID 사용

`menu_id`로 고정 UUID를 만들어 문서 ID로 사용했습니다.
이 ID로 수정하거나 삭제할 문서를 지정할 수 있습니다.

```java
UUID.nameUUIDFromBytes(("game-menu-" + menuId).getBytes()).toString();
```

문서 본문에는 게임명, 설명, 플레이 인원과 시간을 넣고, 메뉴 ID와 게임 ID는 메타데이터에 저장했습니다.
게임과 메뉴를 조회하는 SQL은 이름으로 두 테이블을 연결합니다.

### 관리 기능에서 검색 문서 갱신 호출

게임·메뉴 변경 후 관련 문서를 바로 갱신하도록 서비스에서 호출했습니다.
개별 문서를 갱신할 때는 메뉴 ID로 최신 데이터를 조회하고 다음과 같이 처리합니다.

1. 검색 조건을 만족하면 기존 문서를 삭제하고 새 내용으로 저장합니다.
2. 조건을 만족하지 않으면 기존 문서를 삭제합니다.

검색 대상은 GAME 카테고리에 속한 활성 게임 중, 메뉴가 판매 가능하고 삭제되지 않았으며 `NORMAL` 재고가 있는 게임입니다.

| 변경 시점 | 처리 |
| --- | --- |
| 게임 등록·수정·활성 상태 변경 | 검색 조건을 다시 확인해 문서 갱신 또는 삭제 |
| 게임 메뉴 수정·복원·판매 상태 변경 | 문서 갱신 또는 삭제 |
| 게임 또는 게임 메뉴 삭제 | 문서 삭제 |
| 실물 재고 등록·삭제 | 정상 재고 조건 재확인 |
| 재고를 파손·분실로 변경하거나 해당 상태에서 정상으로 복구 | 정상 재고 조건 재확인 |
| 일반적인 대여·반납 | 벡터 문서를 매번 갱신하지 않음 |

앱 시작 시에도 설정에 따라 검색 조건을 만족하는 게임 문서를 일괄 저장합니다.
관리 기능에서 개별 갱신을 호출하면, 앱을 재시작하지 않고도 변경한 정보를 반영할 수 있습니다.

## 장점과 한계

- 메뉴 ID로 문서를 지정해 전체 문서를 다시 만들지 않고 관련 게임만 갱신할 수 있습니다.
- 관리 요청 안에서 임베딩과 벡터 저장을 호출하므로 외부 서비스가 느려지면 처리 시간도 길어질 수 있습니다.
- 운영 DB 변경과 벡터 저장이 하나의 트랜잭션으로 묶여 있지 않습니다. 벡터 갱신 실패 시 로그만 남기며 자동 재시도는 없습니다.
- 기존 문서를 삭제한 뒤 저장에 실패하면 검색 문서가 없는 상태로 남을 수 있습니다.
- 대여·반납을 매번 반영하지 않아 AI 안내와 실제 대여 가능 재고가 다를 수 있습니다.
- 앱 시작 시 일괄 저장은 조건에 맞지 않는 기존 문서를 대조해 정리하지 않습니다.

갱신 실패와 복구를 검증할 테스트가 필요합니다. 실제 질문에 대한 검색 품질은 [RAG 검색 기준 조정](../troubleshooting/003-rag-similarity.md)에서 다룹니다.

## 관련 코드

- [GameEmbeddingService](../../src/main/java/org/example/board_cafe_kiosk_2603/ai/GameEmbeddingService.java): 조회 조건, 문서·ID 생성, 저장·삭제
- [GameEmbeddingRunner](../../src/main/java/org/example/board_cafe_kiosk_2603/ai/GameEmbeddingRunner.java): 시작 시 일괄 저장
- [GameServiceImpl](../../src/main/java/org/example/board_cafe_kiosk_2603/service/admin/product/GameServiceImpl.java), [MenuServiceImpl](../../src/main/java/org/example/board_cafe_kiosk_2603/service/admin/product/MenuServiceImpl.java): 관리 기능에서 갱신·삭제 호출
- [GameItemServiceImpl](../../src/main/java/org/example/board_cafe_kiosk_2603/service/admin/product/GameItemServiceImpl.java): 재고 상태에 따른 갱신 호출

[Testing](../testing.md) · [Known Issues](../known-issues.md) · [README](../../README.md)
