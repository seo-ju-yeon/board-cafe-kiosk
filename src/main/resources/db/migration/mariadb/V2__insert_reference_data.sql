-- 초기 기준정보. 적용 이력과 트랜잭션은 Flyway가 관리한다.
-- 게임 정보의 근거: src/main/resources/sql/README.md
-- 고객/방문/주문/결제/대여 이력/매출 통계는 생성하지 않는다.

-- ============================================================
-- 관리자 / 키오스크 로그인
-- ============================================================

-- manager
-- 기존 로컬 로그인 계정과 권한 설정을 유지한다.
-- 아래 ID/PW 메모는 포트폴리오 시연용으로 유지한다.
INSERT INTO `manager` (`login_id`, `password`, `name`, `email`,`role`, `is_active`)
VALUES ('admin', '$2a$10$I/U.nHfsL/6wBqXAJV1A3u0KwyHn9wiOVRK7ZVI6rAptphEgRW1Qi', '관리자01', 'wndus6110@naver.com', 'ADMIN', TRUE),
       ('admin02', '$2a$10$RySZbh.V/f9khlbVamY3O.Mg8uY9qbwNTbykKep1SqqtbZ9OMB4xe', '관리자02','wndus6110@naver.com', 'ADMIN', FALSE),
       ('super', '$2a$10$BTMMVv2aPEqCnTF4aWn7u.Tyuh.yruDyPVk1buElSdgCwbMUWOFRi', '사장님', 'wndus6110@naver.com','ADMIN', TRUE),
       ('pass', '$2a$10$BTMMVv2aPEqCnTF4aWn7u.Tyuh.yruDyPVk1buElSdgCwbMUWOFRi', '포트폴리오', 'example@naver.com','SUPER', TRUE),
       ('staff01', '$2a$10$VW29gAYZYxDRdWhNP.KYUOVAkPeS1DZYSrcxywKGdjGpx4z0QitDa', '직원01','wndus6110@naver.com', 'STAFF', TRUE),
       ('staff02', '$2a$10$OhUaODvgez2RlesuWWlyXeMzwWRNhYvTrNjgOy07//KxK8sdWaDFG', '직원02', 'wndus6110@naver.com','STAFF', TRUE),
       ('staff03', '$2a$10$VW29gAYZYxgRdWhNP.KYUOVAkPeS1DZYSrcxywKGdjGpx4z0QitDa', '직원01','wndus6110@naver.com', 'STAFF', TRUE),
       ('staff04', '$2a$10$OhUaODvgez3RlesuWWlyXeMzwWRNhYvTrNjgOy07//KxK8sdWaDFG', '직원02', 'wndus6110@naver.com','STAFF', FALSE),
       ('staff05', '$2a$10$VW29gAYZYxfRdWhNP.KYUOVAkPeS1DZYSrcxywKGdjGpx4z0QitDa', '직원01','wndus6110@naver.com', 'STAFF', FALSE),
       ('staff06', '$2a$10$OhUaODvgez4RlesuWWlyXeMzwWRNhYvTrNjgOy07//KxK8sdWaDFG', '직원02','wndus6110@naver.com', 'STAFF', FALSE);
-- admin / 1111 / ADMIN / TRUE
-- admin02 / 2222 / ADMIN / FALSE
-- super / 1234 / ADMIN / TRUE
-- pass / 1234 / SUPER / TRUE
-- staff01 / 1111 / STAFF / TRUE
-- staff02 / 2222 / STAFF / TRUE

-- cafe_table
-- 테이블 번호 기반 키오스크 로그인 흐름을 확인하기 위한 데이터다.
INSERT INTO `cafe_table` (`table_number`, `password`, `status`, `access_token`, `current_session_id`)
VALUES (1, '$2a$12$6.m99XxVXQhLA.kW.pV.8.yAkQtntwMG6zJ2XEzCYdIt6F92AHZoa', 'EMPTY', NULL, NULL),
       (2, '$2a$12$jMyxkDnEXF6zTzs.6odIHuCtzfR35EDFxZmflbbamUHc9drejGipa', 'EMPTY', NULL, NULL),
       (3, '$2a$12$wPexDR2riZFgwKTtj925FOXZFGPaf6U13GkiNK4Gd43M.1hltvlBS', 'EMPTY', NULL, NULL),
       (4, '$2a$12$6UqwKwlaRu05xPzlTbzBQeC68kViy7OrQscQQq.MzUiMyV9eyOlcO', 'EMPTY', NULL, NULL),
       (5, '$2a$12$BoILW/Dwdq267pCpVPlxWuKsjctBoWy1Jz8XY9KHJiA/v86.pPxYe', 'EMPTY', NULL, NULL),
       (6, '$2a$12$EKiULQjPsNUuxtwfm1K9V.tmr1lkGAUZTzdKkFXiKRGt8N.oC2qwq', 'EMPTY', NULL, NULL),
       (7, '$2a$12$epx2tbnDEk1tuGNOcYu1/.Ciww5olY7rULAIuiUEkn1CLGU1zLV3u', 'EMPTY', NULL, NULL),
       (8, '$2a$12$LANNhG93KYJLa7QpyB5t1uJ.pQBpy7CUFg8r8J.9WAX6ARxsNZzJC', 'EMPTY', NULL, NULL),
       (9, '$2a$12$fHPOFdBDC9dlEIeR648aTeGAIEsK9SBC8UgsCq7vcV2KF6dMk6WDi', 'EMPTY', NULL, NULL),
       (10, '$2a$12$A13LCSatRIIpBFoKiTNyLep7invKMx2KUClmbX28sHYLzEwK4Y4ui', 'EMPTY', NULL, NULL),
       (11, '$2a$12$YoAiJOrZMD4Kk/9lHYi1BOjV2Y3kFSeslzr44L75nfKt6cEOCrgNa', 'EMPTY', NULL, NULL),
       (12, '$2a$12$wXHXtdMIS3U7ASVzH1K4T.nbxE5X5nTcNbdv8BHk4zLOcIDSpv0pu', 'EMPTY', NULL, NULL);
-- 1 : 1111
-- 2 : 2222
-- 3 : 3333
-- 4 : 4444
-- 5 : 5555
-- 6 : 6666
-- 7 : 7777
-- 8 : 8888
-- 9 : 9999
-- 10 : 1010
-- 11 : 1011
-- 12 : 1012

-- ============================================================
-- 카테고리 / 요금제 / 일반 메뉴
-- ============================================================

-- 새 설치의 고정 ID. 운영 중인 DB에 덧붙이는 SQL이 아니다.
INSERT INTO `category` (`id`, `name`, `type`)
VALUES (1, '커피·에스프레소', 'DRINK'),
       (2, '논커피·에이드', 'DRINK'),
       (3, '스낵·과자', 'FOOD'),
       (4, '식사류', 'FOOD'),
       (5, '전략 게임', 'GAME'),
       (6, '파티 게임', 'GAME'),
       (7, '협력 게임', 'GAME'),
       (8, '추가 인원', 'GUEST');

-- 기존 요금 정책과 판매 상태를 유지한다.
INSERT INTO `cafe_package` (`name`, `type`, `duration_minutes`, `base_price`, `extra_price_per_min`, `is_active`)
VALUES ('1시간 패키지', 'HOURLY', 60, 5000, 3000, TRUE),
       ('2시간 패키지', 'HOURLY', 120, 8000, 3000, TRUE),
       ('3시간 패키지', 'HOURLY', 180, 11000, 3000, TRUE),
       ('종일 자유이용권', 'FREE', NULL, 15000, NULL, TRUE),
       ('초과 시간 요금', 'HOURLY', 60, 2000, 3500, FALSE);

-- 가격과 판매 상태는 유지하고 확인되지 않은 원산지/브랜드/구성 수 표현은 제거한다.
INSERT INTO `menu` (`id`, `category_id`, `name`, `price`, `description`, `is_available`)
VALUES (1, 1, '아메리카노', 3000, '에스프레소에 물을 더한 커피입니다.', TRUE),
       (2, 1, '카페라떼', 3500, '에스프레소에 우유를 더한 커피입니다.', TRUE),
       (3, 1, '카푸치노', 3500, '에스프레소에 우유와 우유 거품을 올린 커피입니다.', TRUE),
       (4, 1, '바닐라라떼', 4000, '카페라떼에 바닐라 시럽을 더한 음료입니다.', TRUE),
       (5, 2, '레몬에이드', 4000, '레몬을 넣은 탄산음료입니다.', TRUE),
       (6, 2, '자몽에이드', 4000, '자몽을 넣은 탄산음료입니다.', TRUE),
       (7, 2, '녹차라떼', 3500, '녹차와 우유를 섞은 음료입니다.', TRUE),
       (8, 2, '유자차', 3500, '유자청을 물에 풀어 만든 차입니다.', TRUE),
       (9, 3, '팝콘 (오리지널)', 2000, '기본 팝콘입니다.', TRUE),
       (10, 3, '팝콘 (카라멜)', 2500, '카라멜을 입힌 팝콘입니다.', TRUE),
       (11, 3, '나초 + 살사소스', 3000, '나초와 살사소스를 함께 제공합니다.', TRUE),
       (12, 3, '믹스 너트', 3500, '여러 종류의 견과류를 섞은 간식입니다.', TRUE),
       (13, 4, '토스트 세트', 5000, '계란 토스트와 음료로 구성된 세트입니다.', TRUE),
       (14, 4, '컵라면', 1500, '컵라면입니다.', TRUE),
       (15, 4, '핫도그', 3000, '빵에 소시지를 넣은 핫도그입니다.', FALSE),
       (16, 8, '인원 추가 (1명)', 5000, '이용 중 추가되는 인원 1명에 대한 상품입니다.', TRUE);

-- ============================================================
-- 보드게임 / 재고 / AI 검색 원문
-- ============================================================

-- 한 목록에서 game과 menu를 함께 생성하여 이름/카테고리 불일치를 방지한다.
-- 이 임시 테이블은 설치 세션에서만 사용하고 마지막에 제거한다.
CREATE TEMPORARY TABLE `seed_game_catalog`
(
    `id`               INT PRIMARY KEY,
    `category_id`      INT NOT NULL,
    `name`             VARCHAR(100) NOT NULL UNIQUE,
    `min_players`      INT NOT NULL,
    `max_players`      INT NOT NULL,
    `play_time`        INT NOT NULL,
    `inventory_prefix` VARCHAR(10) NOT NULL UNIQUE,
    `description`      TEXT NOT NULL
) DEFAULT CHARSET = utf8mb4;

-- 기본판 기준. 시간은 발행사 안내값이며 실제 진행 시간은 달라질 수 있다.
INSERT INTO `seed_game_catalog`
    (`id`, `category_id`, `name`, `min_players`, `max_players`, `play_time`, `inventory_prefix`, `description`)
VALUES
    (1, 5, '루미큐브', 2, 4, 30, 'RUM',
     '루미큐브(Rummikub)는 숫자 타일을 조합하는 경쟁 게임입니다. 같은 숫자의 서로 다른 색 타일을 모으거나, 같은 색의 연속된 숫자를 이어 놓습니다. 테이블에 놓인 조합을 재배열하며 자신의 받침대에 있는 타일을 모두 내려놓는 것이 목표입니다. 숫자 조합과 순서 계산을 중심으로 진행합니다.'),
    (2, 6, '할리갈리', 2, 6, 15, 'HAL',
     '할리갈리(Halli Galli)는 과일 카드와 종을 사용하는 순발력 경쟁 게임입니다. 차례대로 카드를 공개하고, 펼쳐진 카드에서 같은 종류의 과일이 정확히 다섯 개가 되면 종을 칩니다. 먼저 올바르게 종을 친 사람이 펼쳐진 카드를 가져갑니다. 과일 개수를 세고 빠르게 반응하는 것이 핵심입니다.'),
    (3, 6, '도블', 2, 8, 15, 'DOB',
     '도블(Dobble, Spot It!)은 두 카드에 공통으로 있는 그림을 찾는 관찰력 경쟁 게임입니다. 어떤 두 카드를 비교해도 공통된 그림이 하나씩 있습니다. 그림의 크기나 방향이 달라도 같은 그림을 먼저 찾아 이름을 말합니다. 선택한 미니게임 규칙에 따라 카드를 모으거나 먼저 없애는 것을 목표로 합니다.'),
    (4, 5, '스플렌더', 2, 4, 30, 'SPL',
     '스플렌더(Splendor)는 보석 토큰으로 개발 카드를 구매하는 경쟁 전략 게임입니다. 차례마다 보석을 모으거나 카드를 구매하고 예약하면서 자신의 카드 구성을 발전시킵니다. 구매한 카드의 보석 보너스를 쌓아 다음 카드의 비용을 줄이고 명성 점수를 얻습니다. 자원 관리와 카드 구매 순서를 계획하는 것이 핵심입니다.'),
    (5, 6, '딕싯', 3, 8, 30, 'DIX',
     '딕싯(Dixit)은 그림 카드에서 떠오르는 말이나 이야기로 단서를 주는 경쟁 게임입니다. 이야기꾼이 그림에 맞는 단서를 말하면 다른 사람도 그 단서에 어울리는 카드를 한 장씩 냅니다. 카드를 섞어 공개한 뒤 이야기꾼의 카드를 찾아 투표합니다. 모두가 맞히거나 아무도 맞히지 않는 단서를 피하며 상상력과 연상을 활용합니다.'),
    (6, 7, '하나비', 2, 5, 30, 'HAN',
     '하나비(Hanabi)는 모두가 한 팀이 되어 불꽃놀이를 완성하는 협력 카드 게임입니다. 자신의 카드 앞면은 볼 수 없고 다른 사람의 카드만 볼 수 있습니다. 제한된 힌트로 카드의 색깔이나 숫자를 알려 주고, 정보를 기억해 같은 색 카드를 1부터 5까지 순서대로 내려놓습니다. 상대를 이기기보다 함께 높은 점수를 만드는 것이 목표입니다.');

INSERT INTO `game` (`id`, `category_id`, `name`, `min_players`, `max_players`, `play_time`, `is_active`)
SELECT `id`, `category_id`, `name`, `min_players`, `max_players`, `play_time`, TRUE
FROM `seed_game_catalog`
ORDER BY `id`;

-- 게임 메뉴는 이용 요금에 포함된 대여 상품으로 기존처럼 가격 0원을 사용한다.
INSERT INTO `menu` (`id`, `category_id`, `name`, `price`, `description`, `is_available`)
SELECT 16 + `id`, `category_id`, `name`, 0, `description`, TRUE
FROM `seed_game_catalog`
ORDER BY `id`;

-- 게임별 두 박스. 방문/대여 기록이 없는 새 설치에서는 전부 대여 가능 상태다.
INSERT INTO `game_item` (`game_id`, `serial_number`, `status`)
SELECT g.`id`, CONCAT(g.`inventory_prefix`, '-', copies.suffix), 'NORMAL'
FROM `seed_game_catalog` g
CROSS JOIN (SELECT '001' AS suffix UNION ALL SELECT '002') copies
ORDER BY g.`id`, copies.suffix;

DROP TEMPORARY TABLE `seed_game_catalog`;

-- ============================================================
-- 기본 안내 문구
-- ============================================================

INSERT INTO `macro_message` (`direction`, `message_text`, `is_active`)
VALUES ('STAFF_TO_TABLE', '주문하신 메뉴가 준비되었습니다. 카운터에서 수령해 주세요.', TRUE),
       ('STAFF_TO_TABLE', '이용 시간이 10분 남았습니다. 연장을 원하시면 카운터에 문의해 주세요.', TRUE),
       ('STAFF_TO_TABLE', '이용하신 게임을 반납해 주세요. 정리가 필요하면 직원을 호출해 주세요.', TRUE),
       ('TABLE_TO_STAFF', '게임 규칙 설명을 부탁드립니다.', TRUE),
       ('TABLE_TO_STAFF', '주문한 메뉴를 확인해 주세요.', TRUE),
       ('TABLE_TO_STAFF', '정산을 부탁드립니다.', TRUE);
