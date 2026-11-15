# TS 002. 신규 등록 게임의 재고가 집계되지 않은 문제

## 문제 상황

관리자 화면에서 게임과 실물 재고의 일련번호를 등록·관리하는 기능을 담당했습니다.
시연 중 **기존 게임의 재고 수는 맞게 표시되지만, 새로 등록한 게임의 재고는 집계되지 않는 문제**를 발견했습니다.

키오스크에서 게임 대여에 따라 재고를 처리하는 기능은 다른 팀원이 담당했습니다.

## 확인과 수정

조회 SQL에서 문제를 찾아 수정했습니다.
다만 수정 전 SQL이 남아 있지 않아 어떤 조건을 바꿨는지 정확하게 비교하기는 어렵습니다.
아래에는 현재 사용하는 집계 방식을 정리했습니다.

## 현재 집계 방식

[GameMapper.xml](../../src/main/resources/mapper/admin/product/GameMapper.xml#L8)에서는
게임에 연결된 실물 재고 중 `NORMAL` 상태인 행을 집계합니다.

```sql
COUNT(CASE WHEN gi.status = 'NORMAL' THEN 1 END) AS game_item_count
```

`CASE`는 정상 재고이면 `1`, 나머지는 `NULL`을 반환합니다.
`COUNT`는 `NULL`을 제외하므로 정상 재고 수를 구할 수 있습니다.
실물 재고를 `g.id = gi.game_id`로 게임에 연결하고, 게임별로 묶어 집계합니다.

## 남은 검증

수정 후 확인한 데이터와 수량의 상세 기록은 남아 있지 않습니다.
[GameMapperTest](../../src/test/java/org/example/board_cafe_kiosk_2603/repository/admin/product/GameMapperTest.java)도 결과를 주로 로그로 출력하므로,
신규 게임의 등록 전후 수량을 기대값과 비교하는 테스트가 필요합니다.

- 실물 재고가 없을 때는 `0`, 정상 재고를 추가하면 해당 수량이 표시되는지
- 대여·파손·분실 재고는 정상 재고 수에서 제외되는지
- 기존 게임과 신규 게임에 같은 재고 조건을 적용하면 같은 결과가 나오는지

[Testing](../testing.md) · [Known Issues](../known-issues.md) · [README](../../README.md)
