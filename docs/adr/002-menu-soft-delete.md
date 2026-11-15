# ADR 002. 통계 조회에 필요한 메뉴 정보 보존

## 배경

메뉴는 현재 판매 목록뿐 아니라 과거 주문과 통계에서도 참조합니다.
통계 담당 팀원이 메뉴를 완전히 삭제하면 과거 통계에서 해당 메뉴가 `unknown`으로 표시될 수 있다는 의견을 냈습니다.
이를 반영해 판매 목록에서 메뉴를 제거해도 통계에 필요한 메뉴 정보는 남기기로 했습니다.

## 선택한 방법과 이유

메뉴 행을 삭제하는 대신 `is_deleted` 값을 바꾸는 Soft Delete를 적용했습니다.
메뉴 ID와 행을 유지하면서 일반 목록에서 제외하고, 필요하면 복원할 수 있는 방식입니다.

```sql
-- 삭제 처리
UPDATE menu SET is_deleted = true WHERE id = #{id};

-- 복원
UPDATE menu SET is_deleted = false WHERE id = #{id};
```

일반 메뉴 목록에는 `is_deleted = false` 조건을 적용하고, 숨김 목록에서는 삭제 처리한 메뉴를 조회합니다.
통계 조회는 메뉴 테이블을 JOIN해 메뉴명과 카테고리를 가져오므로, 메뉴 행을 남겨 두면 해당 정보를 참조할 수 있습니다.

삭제와 일시적인 판매 중단은 별도 상태로 관리했습니다.

| 상태 | 사용 목적 |
| --- | --- |
| `is_deleted` | 메뉴 삭제 처리와 복원 |
| `is_available` | 메뉴 판매 가능 여부 변경 |

게임 메뉴는 삭제할 때 AI 검색용 문서도 삭제하도록 호출합니다.
복원할 때는 게임 활성 상태와 정상 재고 등 검색 조건을 다시 확인합니다.
갱신 방식은 [RAG 동기화](003-rag-synchronization.md)에 정리했습니다.

## 장점과 주의할 점

- 과거 데이터가 참조할 메뉴 행을 유지하고 삭제 처리한 메뉴를 복원할 수 있습니다.
- 일반 목록에서 삭제된 메뉴가 보이지 않도록 조회 조건을 관리해야 합니다.
- 메뉴명이나 카테고리를 변경한 이력까지 보존하지는 않습니다. 과거 통계에도 현재 메뉴 정보가 표시될 수 있습니다.

## 남은 검증

메뉴 삭제 후 일반 목록에서 제외되는지, 과거 통계에는 필요한 정보가 남는지 함께 확인하는 통합 테스트가 필요합니다.

## 관련 코드

- [MenuMapper.xml](../../src/main/resources/mapper/admin/product/MenuMapper.xml): 삭제·복원 SQL과 목록 조회 조건
- [MenuServiceImpl](../../src/main/java/org/example/board_cafe_kiosk_2603/service/admin/product/MenuServiceImpl.java): 삭제·복원과 벡터 문서 처리
- [StatMapper.xml](../../src/main/resources/mapper/admin/statistics/StatMapper.xml): 메뉴를 참조하는 통계 조회

[Testing](../testing.md) · [Known Issues](../known-issues.md) · [README](../../README.md)
