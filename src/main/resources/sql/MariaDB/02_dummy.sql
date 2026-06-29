/*
  Board Wave 시연 데이터

  관리자 로그인, 키오스크 주문, 상품 관리, 게임 재고 관리 흐름을
  바로 확인할 수 있도록 기본 데이터를 넣는다.

  시연 계정은 면접 시 기능 재현을 위해 의도적으로 포함했다.
*/

USE `board_cafe_kiosk_2603`;

-- ============================================================
-- 관리자 / 키오스크 로그인
-- ============================================================

-- manager
-- 관리자 로그인과 권한별 접근 흐름을 확인하기 위한 계정이다.
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
-- admin01 / 1111 / ADMIN / TRUE
-- admin02 / 2222 / ADMIN / FALSE
-- super / 1234 / ADMIN / TRUE
-- pass / 1234 / SUPER / TRUE
-- staff01 / 1111 / STAFF / TRUE
-- staff02 / 2222 / STAFF / FALSE

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

-- 일부 테이블에 자동 로그인 토큰을 발급한다.
# UPDATE cafe_table
# SET access_token = UUID()
# WHERE id IN (1, 2, 3, 4, 5);

-- customer
-- 포인트 조회와 적립 흐름을 확인하기 위한 고객 데이터다.
INSERT INTO `customer` (`phone`, `is_active`)
VALUES ('010-1234-5678', TRUE),
       ('010-2345-6789', TRUE),
       ('010-3456-7890', TRUE),
       ('010-4567-8901', TRUE),
       ('010-5678-9012', TRUE),
       ('010-6789-0123', TRUE),
       ('010-7890-1234', FALSE);

-- ============================================================
-- 상품 / 요금제 / 테이블 이용
-- ============================================================

-- category
-- 음식, 음료, 보드게임, 추가 인원 상품을 구분하기 위한 기본 카테고리다.
INSERT INTO `category` (`name`, `type`)
VALUES ('커피·에스프레소', 'DRINK'), -- 1
       ('논커피·에이드', 'DRINK'),  -- 2
       ('스낵·과자', 'FOOD'),     -- 3
       ('식사류', 'FOOD'),       -- 4
       ('전략 게임', 'GAME'),     -- 5
       ('파티 게임', 'GAME'),     -- 6
       ('협력 게임', 'GAME'),     -- 7
       ('추가 인원', 'GUEST');
-- 8

-- cafe_package
-- 키오스크 패키지 선택과 정산 화면 확인용 요금제 데이터다.
INSERT INTO `cafe_package` (`name`, `type`, `duration_minutes`, `base_price`, `extra_price_per_min`, `is_active`)
VALUES ('1시간 패키지', 'HOURLY', 60, 5000, 3000, TRUE),
       ('2시간 패키지', 'HOURLY', 120, 8000, 3000, TRUE),
       ('3시간 패키지', 'HOURLY', 180, 11000, 3000, TRUE),
       ('종일 자유이용권', 'FREE', NULL, 15000, NULL, TRUE),
       ('초과 시간 요금', 'HOURLY', 60, 2000, 3500, FALSE);

-- table_session
-- 테이블 이용 시작부터 정산까지의 흐름을 확인하기 위한 방문 세션 데이터다.
INSERT INTO `table_session` (`table_id`, `package_id`, `initial_guest_cnt`, `check_in_time`, `check_out_time`,
                             `is_active`, `total_amount`)
VALUES (1, 2, 2, '2026-03-25 13:00:00', '2026-03-25 15:10:00', FALSE, 24500), -- 1
       (2, 3, 4, '2026-03-25 15:30:00', '2026-03-25 18:45:00', FALSE, 58000), -- 2
       (3, 1, 1, '2026-03-25 17:00:00', '2026-03-25 18:05:00', FALSE, 7500),  -- 3
       (5, 2, 3, '2026-03-25 19:00:00', '2026-03-25 21:15:00', FALSE, 35000), -- 4
       (8, 4, 5, '2026-03-25 11:00:00', '2026-03-25 23:00:00', FALSE, 95000); -- 5


-- menu
-- 키오스크 메뉴, 장바구니, 주문 화면 확인용 상품 데이터다.
INSERT INTO `menu` (`category_id`, `name`, `price`, `description`, `is_available`)
VALUES (1, '아메리카노', 3000, '깔끔하고 진한 에스프레소 베이스', TRUE),   -- 1
       (1, '카페라떼', 3500, '우유와 에스프레소의 조화', TRUE),        -- 2
       (1, '카푸치노', 3500, '풍성한 우유 거품과 에스프레소', TRUE),     -- 3
       (1, '바닐라라떼', 4000, '달콤한 바닐라 시럽 추가', TRUE),       -- 4
       (2, '레몬에이드', 4000, '상큼한 국산 레몬 착즙', TRUE),        -- 5
       (2, '자몽에이드', 4000, '달콤 쌉싸름한 자몽 에이드', TRUE),      -- 6
       (2, '녹차라떼', 3500, '국내산 말차 분말 사용', TRUE),         -- 7
       (2, '유자차', 3500, '따뜻하게도 아이스로도', TRUE),           -- 8
       (3, '팝콘 (오리지널)', 2000, '고소한 버터 팝콘', TRUE),       -- 9
       (3, '팝콘 (카라멜)', 2500, '달콤한 카라멜 코팅', TRUE),       -- 10
       (3, '나초 + 살사소스', 3000, '바삭한 나초와 살사소스 콤보', TRUE), -- 11
       (3, '믹스 너트', 3500, '7가지 프리미엄 너트 혼합', TRUE),      -- 12
       (4, '토스트 세트', 5000, '계란 토스트 + 음료 세트', TRUE),     -- 13
       (4, '컵라면', 1500, '신라면·짜파게티 선택 가능', TRUE),        -- 14
       (4, '핫도그', 3000, '국산 돼지고기 소시지 사용', FALSE),       -- 15
       (8, '인원 추가 (1명)', 5000, '기본 패키지 인당 추가 요금', TRUE); -- 16

-- ============================================================
-- 보드게임 / 재고 / AI 안내
-- ============================================================

-- menu
-- 보드게임도 키오스크 장바구니에 담길 수 있도록 menu 데이터로 함께 등록한다.
INSERT INTO `menu` (`category_id`, `name`, `price`, `description`, `is_available`)
VALUES (5, '맞춤법 게임',  0, '맞춤법을 맞추는 파티 게임', TRUE),   -- 17
       (6, '숫자 맞추기', 0, '숫자를 맞추는 게임', TRUE),           -- 18
       (6, '동물 맞추기', 0, '동물 카드 게임', TRUE),               -- 19
       (7, '색상 맞추기', 0, '색상을 맞추는 협력 게임', TRUE),      -- 20
       (5, '스피드 게임',  0, '빠르게 반응하는 전략 게임', TRUE),   -- 21
       (6, '퀴즈 게임',   0, '다양한 퀴즈 보드게임', TRUE);         -- 22

-- game
-- AI 안내와 게임 재고 관리에서 기준이 되는 보드게임 종목 데이터다.
-- category: 5=전략 게임, 6=파티 게임, 7=협력 게임
INSERT INTO `game` (`category_id`, `name`, `min_players`, `max_players`, `play_time`, `is_active`)
VALUES (6, '맞춤법 게임', 2, 6, 20, TRUE), -- id=1  stock=NORMAL 3개
       (6, '숫자 맞추기', 2, 4, 15, TRUE), -- id=2  stock=NORMAL 2개
       (6, '동물 맞추기', 2, 6, 20, TRUE), -- id=3  stock=NORMAL 0개 (전부 대여중/파손)
       (7, '색상 맞추기', 2, 5, 25, TRUE), -- id=4  stock=NORMAL 1개
       (5, '스피드 게임', 2, 8, 10, TRUE), -- id=5  stock=NORMAL 0개 (전부 대여중)
       (6, '퀴즈 게임', 2, 10, 30, TRUE);
-- id=6  stock=NORMAL 4개

-- game_item
-- 보드게임의 실물 재고 상태를 확인하기 위한 데이터다.
-- NORMAL=대여 가능, RENTED=대여 중, DAMAGED=파손, LOST=분실
INSERT INTO `game_item` (`game_id`, `serial_number`, `status`)
VALUES
-- 맞춤법 게임 (game_id=1): NORMAL 3개
(1, 'SPL-001', 'NORMAL'),
(1, 'SPL-002', 'NORMAL'),
(1, 'SPL-003', 'NORMAL'),
(1, 'SPL-004', 'NORMAL'),
(1, 'SPL-005', 'DAMAGED'), -- 파손

-- 숫자 맞추기 (game_id=2): NORMAL 2개
(2, 'NUM-001', 'NORMAL'),
(2, 'NUM-002', 'NORMAL'),
(2, 'NUM-003', 'NORMAL'),

-- 동물 맞추기 (game_id=3): NORMAL 0개 (전부 대여중 or 파손)
(3, 'ANM-001', 'RENTED'),
(3, 'ANM-002', 'RENTED'),
(3, 'ANM-003', 'DAMAGED'),

-- 색상 맞추기 (game_id=4): NORMAL 1개
(4, 'CLR-001', 'NORMAL'),
(4, 'CLR-002', 'RENTED'),
(4, 'CLR-003', 'LOST'),    -- 분실

-- 스피드 게임 (game_id=5): NORMAL 0개 (전부 대여중)
(5, 'SPD-001', 'RENTED'),
(5, 'SPD-002', 'NORMAL'),

-- 퀴즈 게임 (game_id=6): NORMAL 4개
(6, 'QUZ-001', 'NORMAL'),
(6, 'QUZ-002', 'NORMAL'),
(6, 'QUZ-003', 'NORMAL'),
(6, 'QUZ-004', 'NORMAL'),
(6, 'QUZ-005', 'NORMAL');

-- game_history
-- 게임 대여/반납 이력과 통계 화면 확인을 위한 데이터를 생성한다.
DELIMITER $$

DROP PROCEDURE IF EXISTS generate_game_history_data_v2$$

CREATE PROCEDURE generate_game_history_data_v2()
BEGIN
    DECLARE start_date DATE DEFAULT '2026-03-01';
    DECLARE end_date DATE DEFAULT '2026-04-28';
    DECLARE current_date_ptr DATE;
    DECLARE daily_count INT;
    DECLARE rand_rent_time DATETIME;
    DECLARE rand_duration INT;
    DECLARE target_game_item_id INT;
    DECLARE target_session_id BIGINT;

    SET current_date_ptr = start_date;

    -- 시작일부터 종료일까지 일자별 대여 이력을 생성한다.
    WHILE current_date_ptr <= end_date DO
            SET daily_count = 1;

            -- 하루에 10건씩 생성한다.
            WHILE daily_count <= 10 DO
                    -- 해당 날짜 내 11:00 ~ 21:00 사이의 대여 시간을 사용한다.
                    SET rand_rent_time = DATE_ADD(CAST(current_date_ptr AS DATETIME),
                                                  INTERVAL (11 * 60 + FLOOR(RAND() * 600)) MINUTE);

                    -- 대여 시간은 30분 ~ 180분 사이로 생성한다.
                    SET rand_duration = FLOOR(30 + (RAND() * 150));

                    -- 실제 game_item ID 중 하나를 사용한다.
                    SELECT id INTO target_game_item_id
                    FROM `game_item`
                    ORDER BY RAND()
                    LIMIT 1;

                    -- 해당 날짜의 세션을 우선 사용한다.
                    SELECT id INTO target_session_id
                    FROM `table_session`
                    WHERE DATE(check_in_time) = current_date_ptr
                    ORDER BY RAND()
                    LIMIT 1;

                    -- 해당 날짜 세션이 없으면 외래 키 오류 방지를 위해 기존 세션 중 하나를 사용한다.
                    IF target_session_id IS NULL THEN
                        SELECT id INTO target_session_id FROM `table_session` ORDER BY RAND() LIMIT 1;
                    END IF;

                    -- 두 ID가 모두 존재할 때만 대여 이력을 넣는다.
                    IF target_session_id IS NOT NULL AND target_game_item_id IS NOT NULL THEN
                        INSERT INTO `game_history` (
                            `session_id`,
                            `game_item_id`,
                            `rented_at`,
                            `returned_at`,
                            `status`
                        )
                        VALUES (
                                   target_session_id,
                                   target_game_item_id,
                                   rand_rent_time,
                                   DATE_ADD(rand_rent_time, INTERVAL rand_duration MINUTE),
                                   'NORMAL'
                               );
                    END IF;

                    SET daily_count = daily_count + 1;
                    SET target_session_id = NULL;
                    SET target_game_item_id = NULL;
                END WHILE;

            SET current_date_ptr = DATE_ADD(current_date_ptr, INTERVAL 1 DAY);
        END WHILE;
END$$

DELIMITER ;

-- 게임 대여 이력 생성 실행
CALL generate_game_history_data_v2();

-- ============================================================
-- 메시지 / 매장 운영
-- ============================================================

-- macro_message
-- 직원이 키오스크로 보낼 수 있는 기본 안내 문구다.
INSERT INTO `macro_message` (`direction`, `message_text`, `is_active`)
VALUES
    ('STAFF_TO_TABLE', '주문하신 음료와 스낵이 준비되었습니다. 카운터에서 수령해 주세요.', TRUE),
    ('STAFF_TO_TABLE', '이용 시간이 10분 남았습니다. 연장을 원하시면 카운터에 문의해 주세요.', TRUE),
    ('STAFF_TO_TABLE', '주문하신 메뉴가 품절되어 취소 처리되었습니다. 죄송합니다.', TRUE),
    ('STAFF_TO_TABLE', '현재 보드게임 반납 구역이 혼잡하오니 테이블에 그대로 두시면 치워드리겠습니다.', TRUE),
    ('STAFF_TO_TABLE', '진행 중인 이벤트에 당첨되셨습니다! 카운터에서 선물을 확인하세요.', TRUE),
    ('STAFF_TO_TABLE', '외부 음식 반입은 금지되어 있습니다. 양해 부탁드립니다.', TRUE);

-- ============================================================
-- 통계 / 대시보드
-- ============================================================

-- 3월 통계용 데이터 생성
-- 대시보드와 매출 통계 화면을 확인하기 위한 주문/결제 데이터를 만든다.
DELIMITER $$

DROP PROCEDURE IF EXISTS generate_march_stats_data$$

CREATE PROCEDURE generate_march_stats_data()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE rand_date DATETIME;
    DECLARE rand_table_id INT;
    DECLARE rand_package_id INT;
    DECLARE rand_guest_cnt INT;
    DECLARE rand_duration INT;
    DECLARE last_session_id BIGINT;
    DECLARE last_order_id INT;
    DECLARE order_count_per_session INT;

    WHILE i <= 1000 DO
            SET rand_date = FROM_UNIXTIME(
                    UNIX_TIMESTAMP('2026-03-01 11:00:00') + FLOOR(RAND() * (UNIX_TIMESTAMP('2026-03-31 23:00:00') - UNIX_TIMESTAMP('2026-03-01 11:00:00')))
                            );

            SET rand_table_id = FLOOR(1 + (RAND() * 12));
            SET rand_package_id = FLOOR(1 + (RAND() * 4));
            SET rand_guest_cnt = FLOOR(1 + (RAND() * 5));
            SET rand_duration = FLOOR(60 + (RAND() * 180));

            INSERT INTO `table_session` (table_id, package_id, initial_guest_cnt, check_in_time, check_out_time, is_active, total_amount)
            VALUES (rand_table_id, rand_package_id, rand_guest_cnt, rand_date, DATE_ADD(rand_date, INTERVAL rand_duration MINUTE), FALSE, 0);

            SET last_session_id = LAST_INSERT_ID();

            SET order_count_per_session = FLOOR(1 + (RAND() * 2));
            WHILE order_count_per_session > 0 DO
                    INSERT INTO `orders` (session_id, table_id, status, total_amount, ordered_at)
                    VALUES (last_session_id, rand_table_id, 'COMPLETED', 0, DATE_ADD(rand_date, INTERVAL (order_count_per_session * 15) MINUTE));

                    SET last_order_id = LAST_INSERT_ID();

                    -- 메뉴 1~16번 중 하나를 주문 항목으로 사용한다.
                    INSERT INTO `order_item` (order_id, menu_id, menu_name, price, quantity)
                    SELECT last_order_id, id, name, price, FLOOR(1 + (RAND() * 2))
                    FROM menu
                    WHERE id = FLOOR(1 + (RAND() * 16))
                    LIMIT 1;

                    -- 주문 금액이 NULL이 되지 않도록 0으로 보정한다.
                    UPDATE `orders`
                    SET total_amount = (SELECT IFNULL(SUM(price * quantity), 0) FROM `order_item` WHERE order_id = last_order_id)
                    WHERE id = last_order_id;

                    SET order_count_per_session = order_count_per_session - 1;
                END WHILE;

            -- 세션 합계도 NULL이 되지 않도록 0으로 보정한다.
            UPDATE `table_session`
            SET total_amount = (SELECT IFNULL(SUM(total_amount), 0) FROM `orders` WHERE session_id = last_session_id)
            WHERE id = last_session_id;

            INSERT INTO `payment` (session_id, status, final_amount, paid_at)
            SELECT id, 'DONE', total_amount, check_out_time FROM `table_session` WHERE id = last_session_id;

            SET i = i + 1;
        END WHILE;
END$$

DELIMITER ;

-- 3월 통계용 데이터 생성 실행
CALL generate_march_stats_data();

-- item_sales_history
-- 3월 상품별 판매 통계 데이터를 집계한다.
INSERT IGNORE INTO item_sales_history (stat_date, product_id, category, sales_qty, sales_amount)
SELECT
    DATE(p.paid_at) AS stat_date,
    oi.menu_id AS product_id,
    c.type AS category,
    SUM(oi.quantity) AS sales_qty,
    SUM(oi.price * oi.quantity) AS sales_amount
FROM payment p
         JOIN orders o ON p.session_id = o.session_id
         JOIN order_item oi ON o.id = oi.order_id
         JOIN menu m ON oi.menu_id = m.id
         JOIN category c ON m.category_id = c.id
WHERE p.status = 'DONE'
  AND DATE(p.paid_at) BETWEEN '2026-03-01' AND '2026-03-31'
  AND c.type NOT IN ('GAME', 'GUEST')
GROUP BY DATE(p.paid_at), oi.menu_id, c.type;

-- daily_sales_summary
-- 3월 일별 매출, 주문 수, 방문 수 요약 데이터를 집계한다.
INSERT IGNORE INTO daily_sales_summary (stat_date, total_revenue, order_count, visit_count, avg_usage_time)
SELECT
    DATE(p.paid_at) AS stat_date,
    SUM(p.final_amount) AS total_revenue,
    COUNT(DISTINCT o.id) AS order_count,
    -- 초기 인원과 추가 인원 상품 수량을 함께 방문 수로 반영한다.
    SUM(ts.initial_guest_cnt) + IFNULL(extra.extra_qty, 0) AS visit_count,
    IFNULL(AVG(TIMESTAMPDIFF(MINUTE, ts.check_in_time, ts.check_out_time)), 0) AS avg_usage_time
FROM payment p
         JOIN table_session ts ON p.session_id = ts.id
         LEFT JOIN orders o ON ts.id = o.session_id
         LEFT JOIN (
    SELECT o2.session_id, SUM(oi2.quantity) as extra_qty
    FROM order_item oi2
             JOIN orders o2 ON oi2.order_id = o2.id
             JOIN menu m2 ON oi2.menu_id = m2.id
             JOIN category c2 ON m2.category_id = c2.id
    WHERE c2.type = 'GUEST'
    GROUP BY o2.session_id
) extra ON p.session_id = extra.session_id
WHERE p.status = 'DONE'
  AND DATE(p.paid_at) BETWEEN '2026-03-01' AND '2026-03-31'
GROUP BY DATE(p.paid_at);

-- 4월 통계용 데이터 생성
-- 3월과 같은 기준으로 4월 대시보드 확인용 데이터를 만든다.
DELIMITER $$

DROP PROCEDURE IF EXISTS generate_april_stats_data$$

CREATE PROCEDURE generate_april_stats_data()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE rand_date DATETIME;
    DECLARE rand_table_id INT;
    DECLARE rand_package_id INT;
    DECLARE rand_guest_cnt INT;
    DECLARE rand_duration INT;
    DECLARE last_session_id BIGINT;
    DECLARE last_order_id INT;
    DECLARE order_count_per_session INT;

    -- 3월 데이터량과 비슷한 비율로 4월은 900건을 생성한다.
    WHILE i <= 900 DO
            SET rand_date = FROM_UNIXTIME(
                    UNIX_TIMESTAMP('2026-04-01 11:00:00') + FLOOR(RAND() * (UNIX_TIMESTAMP('2026-04-28 23:00:00') - UNIX_TIMESTAMP('2026-04-01 11:00:00')))
                            );

            SET rand_table_id = FLOOR(1 + (RAND() * 12));
            SET rand_package_id = FLOOR(1 + (RAND() * 4));
            SET rand_guest_cnt = FLOOR(1 + (RAND() * 5));
            SET rand_duration = FLOOR(60 + (RAND() * 180));

            INSERT INTO `table_session` (table_id, package_id, initial_guest_cnt, check_in_time, check_out_time, is_active, total_amount)
            VALUES (rand_table_id, rand_package_id, rand_guest_cnt, rand_date, DATE_ADD(rand_date, INTERVAL rand_duration MINUTE), FALSE, 0);

            SET last_session_id = LAST_INSERT_ID();

            SET order_count_per_session = FLOOR(1 + (RAND() * 2));
            WHILE order_count_per_session > 0 DO
                    INSERT INTO `orders` (session_id, table_id, status, total_amount, ordered_at)
                    VALUES (last_session_id, rand_table_id, 'COMPLETED', 0, DATE_ADD(rand_date, INTERVAL (order_count_per_session * 15) MINUTE));

                    SET last_order_id = LAST_INSERT_ID();

                    -- 메뉴 1~16번 중 하나를 주문 항목으로 사용한다.
                    INSERT INTO `order_item` (order_id, menu_id, menu_name, price, quantity)
                    SELECT last_order_id, id, name, price, FLOOR(1 + (RAND() * 2))
                    FROM menu
                    WHERE id = FLOOR(1 + (RAND() * 16))
                    LIMIT 1;

                    -- 주문 금액이 NULL이 되지 않도록 0으로 보정한다.
                    UPDATE `orders`
                    SET total_amount = (SELECT IFNULL(SUM(price * quantity), 0) FROM `order_item` WHERE order_id = last_order_id)
                    WHERE id = last_order_id;

                    SET order_count_per_session = order_count_per_session - 1;
                END WHILE;

            -- 세션 합계도 NULL이 되지 않도록 0으로 보정한다.
            UPDATE `table_session`
            SET total_amount = (SELECT IFNULL(SUM(total_amount), 0) FROM `orders` WHERE session_id = last_session_id)
            WHERE id = last_session_id;

            INSERT INTO `payment` (session_id, status, final_amount, paid_at)
            SELECT id, 'DONE', total_amount, check_out_time FROM `table_session` WHERE id = last_session_id;

            SET i = i + 1;
        END WHILE;
END$$

DELIMITER ;

-- 4월 통계용 데이터 생성 실행
CALL generate_april_stats_data();

-- item_sales_history
-- 4월 상품별 판매 통계 데이터를 집계한다.
INSERT IGNORE INTO item_sales_history (stat_date, product_id, category, sales_qty, sales_amount)
SELECT
    DATE(p.paid_at) AS stat_date,
    oi.menu_id AS product_id,
    c.type AS category,
    SUM(oi.quantity) AS sales_qty,
    SUM(oi.price * oi.quantity) AS sales_amount
FROM payment p
         JOIN orders o ON p.session_id = o.session_id
         JOIN order_item oi ON o.id = oi.order_id
         JOIN menu m ON oi.menu_id = m.id
         JOIN category c ON m.category_id = c.id
WHERE p.status = 'DONE'
  AND DATE(p.paid_at) BETWEEN '2026-04-01' AND '2026-04-28'
  AND c.type NOT IN ('GAME', 'GUEST')
GROUP BY DATE(p.paid_at), oi.menu_id, c.type;

-- daily_sales_summary
-- 4월 일별 매출, 주문 수, 방문 수 요약 데이터를 집계한다.
INSERT IGNORE INTO daily_sales_summary (stat_date, total_revenue, order_count, visit_count, avg_usage_time)
SELECT
    DATE(p.paid_at) AS stat_date,
    SUM(p.final_amount) AS total_revenue,
    COUNT(DISTINCT o.id) AS order_count,
    SUM(ts.initial_guest_cnt) + IFNULL(extra.extra_qty, 0) AS visit_count,
    IFNULL(AVG(TIMESTAMPDIFF(MINUTE, ts.check_in_time, ts.check_out_time)), 0) AS avg_usage_time
FROM payment p
         JOIN table_session ts ON p.session_id = ts.id
         LEFT JOIN orders o ON ts.id = o.session_id
         LEFT JOIN (
    SELECT o2.session_id, SUM(oi2.quantity) as extra_qty
    FROM order_item oi2
             JOIN orders o2 ON oi2.order_id = o2.id
             JOIN menu m2 ON oi2.menu_id = m2.id
             JOIN category c2 ON m2.category_id = c2.id
    WHERE c2.type = 'GUEST'
    GROUP BY o2.session_id
) extra ON p.session_id = extra.session_id
WHERE p.status = 'DONE'
  AND DATE(p.paid_at) BETWEEN '2026-04-01' AND '2026-04-28'
GROUP BY DATE(p.paid_at);

-- 오늘 통계용 데이터 생성
-- 실시간 대시보드 확인을 위해 오늘 날짜 기준 주문/결제 데이터를 만든다.
DELIMITER $$

DROP PROCEDURE IF EXISTS generate_today_stats_data$$

CREATE PROCEDURE generate_today_stats_data()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE rand_date DATETIME;
    DECLARE rand_table_id INT;
    DECLARE rand_package_id INT;
    DECLARE rand_guest_cnt INT;
    DECLARE rand_duration INT;
    DECLARE last_session_id BIGINT;
    DECLARE last_order_id INT;
    DECLARE order_count_per_session INT;
    DECLARE is_extra_guest INT;

    -- 오늘 날짜 기준으로 50건의 세션 데이터를 생성한다.
    WHILE i <= 50 DO
            -- 오전 10시 ~ 오후 10시 사이의 임의 시간을 사용한다.
            SET rand_date = DATE_ADD(CURDATE(), INTERVAL FLOOR(10*60 + RAND() * 12*60) MINUTE);

            SET rand_table_id = FLOOR(1 + (RAND() * 12));
            SET rand_package_id = FLOOR(1 + (RAND() * 4));
            SET rand_guest_cnt = FLOOR(1 + (RAND() * 4));
            SET rand_duration = FLOOR(60 + (RAND() * 120));

            -- 테이블 이용 세션을 생성한다.
            INSERT INTO `table_session` (table_id, package_id, initial_guest_cnt, check_in_time, check_out_time, is_active, total_amount)
            VALUES (rand_table_id, rand_package_id, rand_guest_cnt, rand_date, DATE_ADD(rand_date, INTERVAL rand_duration MINUTE), FALSE, 0);

            SET last_session_id = LAST_INSERT_ID();

            -- 세션별 주문은 1~3건 생성한다.
            SET order_count_per_session = FLOOR(1 + (RAND() * 3));
            WHILE order_count_per_session > 0 DO
                    INSERT INTO `orders` (session_id, table_id, status, total_amount, ordered_at)
                    VALUES (last_session_id, rand_table_id, 'COMPLETED', 0, DATE_ADD(rand_date, INTERVAL (order_count_per_session * 15) MINUTE));

                    SET last_order_id = LAST_INSERT_ID();

                    SET is_extra_guest = FLOOR(1 + (RAND() * 10));

                    IF is_extra_guest <= 3 THEN
                        INSERT INTO `order_item` (order_id, menu_id, menu_name, price, quantity)
                        SELECT last_order_id, id, name, price, FLOOR(1 + (RAND() * 2))
                        FROM menu
                        WHERE name = '인원 추가 (1명)'
                        LIMIT 1;
                    ELSE
                        INSERT INTO `order_item` (order_id, menu_id, menu_name, price, quantity)
                        SELECT last_order_id, id, name, price, FLOOR(1 + (RAND() * 2))
                        FROM menu
                        WHERE name != '인원 추가 (1명)'
                        ORDER BY RAND()
                        LIMIT 1;
                    END IF;

                    UPDATE `orders`
                    SET total_amount = (SELECT IFNULL(SUM(price * quantity), 0) FROM `order_item` WHERE order_id = last_order_id)
                    WHERE id = last_order_id;

                    SET order_count_per_session = order_count_per_session - 1;
                END WHILE;

            -- 세션 총액을 주문 금액 합계로 갱신한다.
            UPDATE `table_session`
            SET total_amount = (SELECT IFNULL(SUM(total_amount), 0) FROM `orders` WHERE session_id = last_session_id)
            WHERE id = last_session_id;

            -- 결제 완료 데이터를 생성한다.
            INSERT INTO `payment` (session_id, status, final_amount, paid_at)
            SELECT id, 'DONE', total_amount, check_out_time FROM `table_session` WHERE id = last_session_id;

            SET i = i + 1;
        END WHILE;
END$$

DELIMITER ;

-- 오늘 통계용 데이터 생성 실행
CALL generate_today_stats_data();
