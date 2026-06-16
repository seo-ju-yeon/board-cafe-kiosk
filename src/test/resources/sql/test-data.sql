-- 테스트용 기초 데이터 삽입
-- cafe_table (table_number 1번 보장)
INSERT IGNORE INTO cafe_table (table_number, password, status)
VALUES (1, '1111', 'EMPTY');

-- category
INSERT IGNORE INTO category (id, name, type) VALUES (1, '커피/음료', 'DRINK');
INSERT IGNORE INTO category (id, name, type) VALUES (2, '식사/간식', 'FOOD');

-- menu (테스트용)
INSERT IGNORE INTO menu (id, category_id, name, price, is_available, is_deleted)
VALUES (1, 1, '아이스 아메리카노', 4000, TRUE, FALSE);
INSERT IGNORE INTO menu (id, category_id, name, price, is_available, is_deleted)
VALUES (2, 2, '소금 버터 팝콘', 3500, TRUE, FALSE);

-- cafe_package
INSERT IGNORE INTO cafe_package (id, name, type, duration_minutes, base_price, extra_price_per_min, is_active)
VALUES (1, '평일 1시간 권', 'HOURLY', 60, 3000, 50.00, TRUE);
INSERT IGNORE INTO cafe_package (id, name, type, duration_minutes, base_price, extra_price_per_min, is_active)
VALUES (2, '평일 무제한 권', 'FREE', NULL, 15000, 0.00, TRUE);
