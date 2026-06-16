CREATE DATABASE IF NOT EXISTS `board_cafe_kiosk_2603`;
USE `board_cafe_kiosk_2603`;

--  보드게임 카페 키오스크 시스템 — 최종 스키마
--  테이블 목록 (총 22개)
--  ┌─────┬─────────────────────┬────────────────────────────────────────────────┐
--  │  #  │ 테이블명              │ 역할 (비고)                                     │
--  ├─────┼─────────────────────┼────────────────────────────────────────────────┤
--  │  1  │ manager             │ 관리자·직원 계정 (ADMIN / STAFF)                  │
--  │  2  │ cafe_table          │ 물리적 테이블 (UUID access_token 추가)            │
--  │  3  │ customer            │ 전화번호 등록 고객 (포인트 대상)                    │
--  │  4  │ category            │ 메뉴·게임·인원(GUEST) 공통 카테고리                │
--  │  5  │ cafe_package        │ 패키지 요금 정책                                 │
--  │  6  │ table_session       │ 테이블 이용 히스토리 및 세션 관리 (핵심)             │
--  │  7  │ menu                │ 음식·음료 및 추가인원 상품                         │
--  │  8  │ orders              │ 주문 헤더 (session_id 외래키 추가)                │
--  │  9  │ order_item          │ 주문 상세 항목                                   │
--  │ 10  │ game                │ 보드게임 종목                                    │
--  │ 11  │ game_item           │ 보드게임 실물 재고 (박스 단위)                      │
--  │ 12  │ cart                │ 테이블별 장바구니 헤더                             │
--  │ 13  │ cart_item           │ 장바구니 담긴 메뉴 항목                            │
--  │ 14  │ game_history        │ 게임 대여 이력 (session_id 기반으로 변경)           │
--  │ 15  │ payment             │ 결제 헤더 (세션 단위 정산으로 변경)                  │
--  │ 16  │ point               │ 전화번호 기반 포인트 계좌                          │
--  │ 17  │ point_history       │ 포인트 적립·사용 이력                              │
--  │ 18  │ macro_message       │ 1클릭 매크로 메시지                               │
--  │ 19  │ table_message       │ 통합 메시지 로그                                  │
--  │ 20  │ item_sales_history  │ 일일 상품별 판매 통계                             │
--  │ 21  │ daily_sales_summary │ 매장 전체 일별 매출 요약                           │
--  │ 22  │ persistent_logins   │ Remember-Me Persistent 토큰 저장소               │
--  └─────┴─────────────────────┴────────────────────────────────────────────────┘

-- 1. manager
CREATE TABLE `manager`
(
    `id`         INT                    NOT NULL AUTO_INCREMENT COMMENT '관리자/직원 고유 번호 (PK)',
    `login_id`   VARCHAR(50)            NOT NULL COMMENT '로그인 아이디 (중복 불가)',
    `password`   VARCHAR(255)           NOT NULL COMMENT 'BCrypt 암호화 비밀번호',
    `name`       VARCHAR(30)            NOT NULL COMMENT '실명',
    `email`      VARCHAR(100)           NOT NULL comment 'OTP인증용 이메일',
    `role`       ENUM ('ADMIN','STAFF','SUPER') NOT NULL DEFAULT 'STAFF' COMMENT '권한: ADMIN(사장), STAFF(직원)',
    `is_active`  BOOLEAN                NOT NULL DEFAULT TRUE COMMENT '활성 상태 (FALSE=비활성)',
    `created_at` TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '계정 생성 일시',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_manager_login_id` (`login_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='관리자·직원 계정';

-- 2. cafe_table
-- 수정사항: 로그인 유지를 위한 access_token(UUID) 및 현재 세션 추적용 컬럼 추가
CREATE TABLE `cafe_table`
(
    `id`                 INT                                  NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '카페 내 테이블(물리) 고유 번호',
    `table_number`       INT                                  NOT NULL UNIQUE COMMENT '표시 테이블 번호',
    `password`           VARCHAR(100)                         NOT NULL COMMENT '태블릿 최초 인증 비밀번호',
    `status`             ENUM ('EMPTY','OCCUPIED','CLEANING') NOT NULL DEFAULT 'EMPTY',
    `access_token`       VARCHAR(255)                                  DEFAULT NULL UNIQUE COMMENT 'UUID 기반 자동 로그인 토큰',
    `current_session_id` BIGINT                                        DEFAULT NULL COMMENT '현재 진행 중인 table_session ID'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='카페 물리 테이블 정보';

-- 3. customer
CREATE TABLE `customer`
(
    `id`         INT         NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '고객 고유 식별 번호',
    `phone`      VARCHAR(20) NOT NULL UNIQUE COMMENT '전화번호 (유일 식별자)',
    `is_active`  BOOLEAN     NOT NULL DEFAULT TRUE COMMENT '활성 상태 여부',
    `created_at` TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입 일시'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='전화번호 등록 고객 정보';

-- 4. category
-- 수정사항: 인원 추가 관리를 위한 GUEST 타입 확장
CREATE TABLE `category`
(
    `id`   INT                                  NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '카테고리 고유 번호',
    `name` VARCHAR(50)                          NOT NULL COMMENT '카테고리명',
    `type` ENUM ('DRINK','FOOD','GAME','GUEST') NOT NULL COMMENT 'GUEST: 인원추가 전용'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='메뉴·게임·인원 공통 대분류';

-- 5. cafe_package (순서 조정: session 참조용)
CREATE TABLE `cafe_package`
(
    `id`                  INT                    NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '요금제 고유 번호',
    `name`                VARCHAR(50)            NOT NULL COMMENT '패키지 명칭',
    `type`                ENUM ('HOURLY','FREE') NOT NULL COMMENT '요금제 유형',
    `duration_minutes`    INT                             DEFAULT NULL COMMENT ' 기본 제공 시간',
    `base_price`          INT                    NOT NULL DEFAULT 0 COMMENT '1인당 기본 요금',
    `extra_price_per_min` INT                             DEFAULT NULL COMMENT '추가 10분당 요금',
    `is_active`           BOOLEAN                NOT NULL DEFAULT TRUE COMMENT '판매 상태',
    `updated_at`          TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '정책 수정일'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='패키지 요금 정책';

-- 6. table_session
-- 추가이유: 요구하신 '테이블 이용 이력 히스토리' 및 세션 로그인 유지를 위한 핵심 테이블
CREATE TABLE `table_session`
(
    `id`                BIGINT    NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `table_id`          INT       NOT NULL COMMENT '이용 테이블 (FK)',
    `package_id`        INT       NOT NULL COMMENT '선택 패키지 (FK)',
    `initial_guest_cnt` INT       NOT NULL DEFAULT 1 COMMENT '최초 입장 인원',
    `check_in_time`     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입장 시간',
    `check_out_time`    TIMESTAMP NULL COMMENT '퇴장 시간',
    `is_active`         BOOLEAN   NOT NULL DEFAULT TRUE COMMENT '현재 세션 활성화 여부',
    `total_amount`      INT       NOT NULL DEFAULT 0 COMMENT '최종 정산 금액 (퇴실 시 합산)',
    CONSTRAINT `fk_session_table` FOREIGN KEY (`table_id`) REFERENCES `cafe_table` (`id`),
    CONSTRAINT `fk_session_package` FOREIGN KEY (`package_id`) REFERENCES `cafe_package` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='테이블 이용 세션 및 방문 히스토리';

-- 7. menu
-- 수정사항: 추가 인원(GUEST) 상품이 이 테이블에 등록됨
CREATE TABLE `menu`
(
    `id`           INT          NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '메뉴 고유 식별 번호',
    `category_id`  INT                   DEFAULT NULL COMMENT '소속 카테고리',
    `name`         VARCHAR(100) NOT NULL COMMENT '메뉴/상품 이름',
    `price`        INT          NOT NULL COMMENT '판매 가격',
    `description`  TEXT                  DEFAULT NULL COMMENT '상세 설명',
    `image_url`    VARCHAR(255)          DEFAULT NULL COMMENT '이미지 경로 저장',
    `is_available` BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '판매 가능 여부',
    `is_deleted`   BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '삭제 여부',
    `created_at`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '상품 등록일',
    CONSTRAINT `fk_menu_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='판매 메뉴 및 인원추가 상품';

-- 8. orders
-- 수정사항: session_id를 추가하여 '어느 방문 건'의 주문인지 명확히 식별
CREATE TABLE `orders`
(
    `id`             INT                                                                              NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '주문 고유 번호',
    `session_id`     BIGINT                                                                           NOT NULL COMMENT '방문 세션 ID (FK)',
    `table_id`       INT                                                                              NOT NULL COMMENT '주문 테이블 (FK)',
    `customer_phone` VARCHAR(20)                                                                               DEFAULT NULL COMMENT '주문자 연락처(포인트 적립용)',
    `status`         ENUM ('ORDERED', 'CONFIRMED', 'COOKING', 'DELIVERING', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'ORDERED' COMMENT '주문 상태',
    `total_amount`   INT                                                                              NOT NULL DEFAULT 0 COMMENT '주문 총액',
    `ordered_at`     TIMESTAMP                                                                        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '주문 일시',
    CONSTRAINT `fk_orders_session` FOREIGN KEY (`session_id`) REFERENCES `table_session` (`id`),
    CONSTRAINT `fk_orders_table` FOREIGN KEY (`table_id`) REFERENCES `cafe_table` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='주문 헤더';

-- 9. order_item
CREATE TABLE `order_item`
(
    `id`        INT          NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '주문 상세 고유 번호',
    `order_id`  INT          NOT NULL COMMENT '주문서 번호',
    `menu_id`   INT DEFAULT NULL COMMENT '메뉴 식별자',
    `menu_name` VARCHAR(100) NOT NULL COMMENT '주문 당시 메뉴명',
    `price`     INT          NOT NULL COMMENT '주문 당시 단가',
    `quantity`  INT          NOT NULL COMMENT '주문 수량',
    CONSTRAINT `fk_orderitem_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='주문 상세 항목';

-- 10. game
CREATE TABLE `game`
(
    `id`          INT          NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '게임 고유 식별 번호',
    `category_id` INT                   DEFAULT NULL COMMENT '게임 장르',
    `name`        VARCHAR(100) NOT NULL COMMENT '보드게임 이름',
    `min_players` INT COMMENT '최소 인원',
    `max_players` INT COMMENT '최대 인원',
    `play_time`   INT COMMENT '평균 플레이 시간(분)',
    `is_active`   BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '운영 상태',
    `image_url`   VARCHAR(255)          DEFAULT NULL COMMENT '게임 썸네일 경로',
    CONSTRAINT `fk_game_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='보드게임 종목';

-- 11. game_item
CREATE TABLE `game_item`
(
    `id`            INT                                       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '실물 개체 고유 번호',
    `game_id`       INT                                       NOT NULL COMMENT '게임 정보 연결',
    `serial_number` VARCHAR(50)                               NOT NULL UNIQUE COMMENT '관리 번호',
    `status`        ENUM ('NORMAL','RENTED','DAMAGED','LOST') NOT NULL DEFAULT 'NORMAL' COMMENT '현재 개체 상태',
    CONSTRAINT `fk_gameitem_game` FOREIGN KEY (`game_id`) REFERENCES `game` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='보드게임 실물 재고(박스 단위)';

-- 12. cart
CREATE TABLE `cart`
(
    `id`         INT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '장바구니 고유 번호',
    `table_id`   INT       NOT NULL UNIQUE COMMENT '테이블 식별 번호',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '최종 수정 시각',
    CONSTRAINT `fk_cart_table` FOREIGN KEY (`table_id`) REFERENCES `cafe_table` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='테이블별 장바구니 헤더';

-- 13. cart_item
CREATE TABLE `cart_item`
(
    `id`       INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '장바구니 항목 고유 번호',
    `cart_id`  INT NOT NULL COMMENT '상위 장바구니 번호',
    `menu_id`  INT NOT NULL COMMENT '선택한 메뉴 번호',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '담은 수량',
    UNIQUE KEY `uq_cartitem_cart_menu` (`cart_id`, `menu_id`),
    CONSTRAINT `fk_cartitem_cart` FOREIGN KEY (`cart_id`) REFERENCES `cart` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cartitem_menu` FOREIGN KEY (`menu_id`) REFERENCES `menu` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='장바구니 담긴 메뉴 항목';

-- 14. game_history
-- 수정사항: table_id 대신 session_id를 사용하여 히스토리 추적성 강화
CREATE TABLE `game_history`
(
    `id`           BIGINT                                       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '대여 이력 고유 번호',
    `session_id`   BIGINT                                       NOT NULL COMMENT '방문 세션 ID (FK)',
    `game_item_id` INT                                          NOT NULL COMMENT '실물 게임 ID',
    `rented_at`    TIMESTAMP                                    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '대여 시각',
    `returned_at`  TIMESTAMP                                    NULL COMMENT '반납 시각',
    `status`       ENUM ('NORMAL','RENTED','DAMAGED','LOST') NOT NULL DEFAULT 'NORMAL' COMMENT '대여 진행 상태',
    CONSTRAINT `fk_rental_session` FOREIGN KEY (`session_id`) REFERENCES `table_session` (`id`),
    CONSTRAINT `fk_rental_item` FOREIGN KEY (`game_item_id`) REFERENCES `game_item` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='게임 대여 이력 (session_id 기반으로 변경)';

-- 15. payment (토스페이먼츠 결제 정보 통합)
CREATE TABLE `payment`
(
    `id`            INT                   NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '결제 고유 번호',
    `session_id`    BIGINT                NOT NULL UNIQUE COMMENT '세션당 최종 1회 결제',
    `table_number`  INT                   NULL COMMENT '결제 당시 테이블 번호',
    `status`        ENUM ('READY','DONE') NOT NULL                                DEFAULT 'READY' COMMENT '결제 상태',
    `final_amount`  INT                   NOT NULL COMMENT '최종 실결제 금액',
    `payment_key`   VARCHAR(200) UNIQUE COMMENT '토스 결제 키 (중복 결제 방지)',
    `order_id_toss` VARCHAR(64)                                                   DEFAULT NULL COMMENT '토스용 주문번호',
    `method`        VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '결제 수단 (카드, 간편결제 등)',
    `raw_response`  JSON                                                          DEFAULT NULL COMMENT '토스 API 응답 원문',
    `approved_at`   TIMESTAMP                                                     DEFAULT NULL COMMENT '토스 승인 시각',
    `paid_at`       TIMESTAMP                                                     DEFAULT NULL COMMENT '결제 완료 시각',
    INDEX `idx_table_number` (`table_number`),
    INDEX `idx_method` (`method`),
    CONSTRAINT `fk_payment_session` FOREIGN KEY (`session_id`) REFERENCES `table_session` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='결제 헤더(session 단위 정산)';

-- 16. point
CREATE TABLE `point`
(
    `id`         INT         NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '포인트 계정 ID',
    `phone`      VARCHAR(20) NOT NULL UNIQUE COMMENT '고객 식별 키',
    `balance`    INT         NOT NULL DEFAULT 0 COMMENT '현재 잔여 포인트',
    `updated_at` TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '최종 수정 시간'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='전화번호 기반 포인트 계좌';

-- 17. point_history
CREATE TABLE `point_history`
(
    `id`            BIGINT              NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '이력 고유 ID',
    `point_id`      INT                 NOT NULL COMMENT '포인트 계정 식별자',
    `order_id`      INT                          DEFAULT NULL COMMENT '연관 주문 번호',
    `type`          ENUM ('EARN','USE') NOT NULL COMMENT '변동 유형',
    `amount`        INT                 NOT NULL COMMENT '변동 금액',
    `balance_after` INT                 NOT NULL COMMENT '변동 후 잔액',
    `created_at`    TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '기록 일시',
    CONSTRAINT `fk_pointhistory_point` FOREIGN KEY (`point_id`) REFERENCES `point` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_pointhistory_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='포인트 적립/사용';

-- 18. macro_message
CREATE TABLE `macro_message`
(
    `id`           INT                                      NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '메세지 고유 번호',
    `direction`    ENUM ('STAFF_TO_TABLE','TABLE_TO_STAFF') NOT NULL COMMENT '전송 방향',
    `message_text` VARCHAR(255)                             NOT NULL COMMENT '메세지 내용',
    `is_active`    BOOLEAN                                  NOT NULL DEFAULT TRUE COMMENT '활성화 상태'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='1클릭 매크로 메세지';

-- 19. table_message
CREATE TABLE `table_message`
(
    `id`         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '메세지 로그 고유 번호',
    `table_id`   INT          NOT NULL COMMENT '발생 테이블',
    `macro_id`   INT                   DEFAULT NULL COMMENT '사용된 매크로 번호',
    `content`    VARCHAR(255) NOT NULL COMMENT '실제 메세지 본문',
    `direction`  ENUM ('STAFF_TO_TABLE', 'TABLE_TO_STAFF') NOT NULL COMMENT '전송 방향',
    `is_read`    BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '읽음 상태',
    `created_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '전송 시각',
    CONSTRAINT `fk_tablemsg_table` FOREIGN KEY (`table_id`) REFERENCES `cafe_table` (`id`),
    CONSTRAINT `fk_tablemsg_macro` FOREIGN KEY (`macro_id`) REFERENCES `macro_message` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='통합 메세지 로그';

-- 20. item_sales_history
CREATE TABLE `item_sales_history`
(
    `id`           INT                                     NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '통계 레코드 고유 번호',
    `stat_date`    DATE                                    NOT NULL COMMENT '통계 기준 날짜',
    `product_id`   INT                                     NOT NULL COMMENT '상품 식별 번호',
    `category`     ENUM ('DRINK', 'FOOD', 'GAME', 'GUEST') NOT NULL COMMENT '상품 카테고리',
    `sales_qty`    INT                                     NOT NULL DEFAULT 0 COMMENT '당일 판매 수량',
    `sales_amount` BIGINT                                  NOT NULL DEFAULT 0 COMMENT '당일 판매 금액',
    UNIQUE KEY `uq_stat_date_product` (`stat_date`, `product_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='일일 상품별 판매 통계';

-- 21. daily_sales_summary
CREATE TABLE `daily_sales_summary`
(
    `id`             BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '통계 일련 번호',
    `stat_date`      DATE   NOT NULL UNIQUE COMMENT '통계 기준 날짜',
    `total_revenue`  BIGINT NOT NULL DEFAULT 0 COMMENT '당일 총 매출액',
    `order_count`    INT    NOT NULL DEFAULT 0 COMMENT '총 주문 건수',
    `visit_count`    INT    NOT NULL DEFAULT 0 COMMENT '총 방문객 수',
    `avg_usage_time` INT             DEFAULT 0 COMMENT '평균 이용 시간'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT =' 매장 전체 일별 매출 요약 ';

-- 22. persistent_logins
CREATE TABLE persistent_logins
(
    series    VARCHAR(64) NOT NULL,
    username  VARCHAR(64) NOT NULL,
    token     VARCHAR(64) NOT NULL,
    last_used TIMESTAMP   NOT NULL,
    PRIMARY KEY (series)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
    COMMENT = 'Spring Security Remember-Me Persistent 토큰 저장소. 관리자 전체 리셋 시 전체 삭제됨.';

-- 사용자 생성 및 권한
--
CREATE USER IF NOT EXISTS `admin`@`%` IDENTIFIED BY '0331';
GRANT ALL PRIVILEGES ON `board_cafe_kiosk_2603`.* TO `admin`@`%`;
FLUSH PRIVILEGES;