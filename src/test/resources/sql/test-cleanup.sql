-- 외래 키 순서대로 테스트 데이터 정리
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM toss_payment;
DELETE FROM payment;
DELETE FROM order_item;
DELETE FROM orders;
DELETE FROM cart_item;
DELETE FROM cart;
DELETE FROM point_history;
DELETE FROM point WHERE phone LIKE '010-9999-%';
DELETE FROM menu        WHERE id IN (1, 2);
DELETE FROM category    WHERE id IN (1, 2);
DELETE FROM cafe_package WHERE id IN (1, 2);
DELETE FROM cafe_table  WHERE table_number = 1;

SET FOREIGN_KEY_CHECKS = 1;
