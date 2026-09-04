-- H2 版建表脚本（用于无 MySQL 的演示环境）

CREATE TABLE IF NOT EXISTS t_store (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    address     VARCHAR(255),
    phone       VARCHAR(20),
    status      TINYINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted     TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_staff (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_no    VARCHAR(32) NOT NULL,
    name        VARCHAR(32) NOT NULL,
    phone       VARCHAR(20) NOT NULL,
    password    VARCHAR(100) NOT NULL,
    store_id    BIGINT,
    status      TINYINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted     TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_staff_phone UNIQUE (phone, deleted),
    CONSTRAINT uk_staff_no UNIQUE (staff_no, deleted)
);

CREATE TABLE IF NOT EXISTS t_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(32) NOT NULL,
    name        VARCHAR(32) NOT NULL,
    remark      VARCHAR(128),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted     TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_role_code UNIQUE (code, deleted)
);

CREATE TABLE IF NOT EXISTS t_permission (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(64) NOT NULL,
    name        VARCHAR(64) NOT NULL,
    module      VARCHAR(32),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted     TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_perm_code UNIQUE (code, deleted)
);

CREATE TABLE IF NOT EXISTS t_staff_role (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_id BIGINT NOT NULL,
    role_id  BIGINT NOT NULL,
    CONSTRAINT uk_staff_role UNIQUE (staff_id, role_id)
);

CREATE TABLE IF NOT EXISTS t_role_permission (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    CONSTRAINT uk_role_perm UNIQUE (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS t_member (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_no   VARCHAR(32) NOT NULL,
    name        VARCHAR(32),
    phone       VARCHAR(20) NOT NULL,
    openid      VARCHAR(64),
    level       VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    discount    INT NOT NULL DEFAULT 100,
    points      INT NOT NULL DEFAULT 0,
    status      TINYINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted     TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_member_phone UNIQUE (phone, deleted)
);

CREATE TABLE IF NOT EXISTS t_member_account (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id      BIGINT NOT NULL,
    balance        DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_recharge DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_consume  DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    version        INT NOT NULL DEFAULT 0,
    create_time    TIMESTAMP,
    update_time    TIMESTAMP,
    deleted        TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_account_member UNIQUE (member_id, deleted)
);

CREATE TABLE IF NOT EXISTS t_wallet_txn (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id      BIGINT NOT NULL,
    biz_no         VARCHAR(64) NOT NULL,
    type           VARCHAR(16) NOT NULL,
    amount         DECIMAL(12,2) NOT NULL,
    balance_before DECIMAL(12,2) NOT NULL,
    balance_after  DECIMAL(12,2) NOT NULL,
    ref_order_no   VARCHAR(64),
    staff_id       BIGINT,
    store_id       BIGINT,
    remark         VARCHAR(128),
    create_time    TIMESTAMP,
    update_time    TIMESTAMP,
    deleted        TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_txn_bizno UNIQUE (biz_no, deleted)
);
CREATE INDEX IF NOT EXISTS idx_txn_member ON t_wallet_txn(member_id);

CREATE TABLE IF NOT EXISTS t_tea_room (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    room_type   VARCHAR(32),
    capacity    VARCHAR(32),
    price_hour  DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    image       VARCHAR(255),
    store_id    BIGINT,
    status      TINYINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted     TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_reservation (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no        VARCHAR(32) NOT NULL,
    member_id       BIGINT NOT NULL,
    room_id         BIGINT NOT NULL,
    reserve_date    DATE NOT NULL,
    start_time      VARCHAR(8) NOT NULL,
    hours           DECIMAL(4,1) NOT NULL DEFAULT 1,
    amount          DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status          VARCHAR(16) NOT NULL DEFAULT 'WAITING',
    verify_staff_id BIGINT,
    store_id        BIGINT,
    create_time     TIMESTAMP,
    update_time     TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_reservation_no UNIQUE (order_no, deleted),
    CONSTRAINT uk_room_slot UNIQUE (room_id, reserve_date, start_time, deleted)
);
CREATE INDEX IF NOT EXISTS idx_reservation_member ON t_reservation(member_id);

CREATE TABLE IF NOT EXISTS t_product (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(128) NOT NULL,
    barcode      VARCHAR(64),
    category     VARCHAR(32),
    spec         VARCHAR(64),
    retail_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    member_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    stock        INT NOT NULL DEFAULT 0,
    warn_stock   INT NOT NULL DEFAULT 0,
    channel      VARCHAR(32),
    image        VARCHAR(255),
    status       TINYINT NOT NULL DEFAULT 1,
    store_id     BIGINT,
    create_time  TIMESTAMP,
    update_time  TIMESTAMP,
    deleted      TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_product_barcode UNIQUE (barcode, deleted)
);

CREATE TABLE IF NOT EXISTS t_consume_order (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no      VARCHAR(32) NOT NULL,
    member_id     BIGINT NOT NULL,
    staff_id      BIGINT,
    store_id      BIGINT,
    origin_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    pay_amount    DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    pay_type      VARCHAR(16) NOT NULL DEFAULT 'MEMBER_CARD',
    status        VARCHAR(16) NOT NULL DEFAULT 'PAID',
    remark        VARCHAR(128),
    create_time   TIMESTAMP,
    update_time   TIMESTAMP,
    deleted       TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_consume_no UNIQUE (order_no, deleted)
);
CREATE INDEX IF NOT EXISTS idx_consume_member ON t_consume_order(member_id);

CREATE TABLE IF NOT EXISTS t_consume_item (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id  BIGINT NOT NULL,
    item_type VARCHAR(16) NOT NULL,
    item_name VARCHAR(128) NOT NULL,
    price     DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    quantity  INT NOT NULL DEFAULT 1,
    subtotal  DECIMAL(12,2) NOT NULL DEFAULT 0.00
);
CREATE INDEX IF NOT EXISTS idx_item_order ON t_consume_item(order_id);
