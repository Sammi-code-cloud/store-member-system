-- ============================================================
-- 八马门店后台管理系统 · 建表脚本（幂等，可重复执行）
-- 数据库需提前创建：CREATE DATABASE bama_store DEFAULT CHARSET utf8mb4;
-- ============================================================

-- 门店
CREATE TABLE IF NOT EXISTS t_store (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)  NOT NULL COMMENT '门店名称',
    address     VARCHAR(255)          DEFAULT NULL COMMENT '门店地址',
    phone       VARCHAR(20)           DEFAULT NULL COMMENT '联系电话',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1营业 0停业',
    create_time DATETIME              DEFAULT NULL,
    update_time DATETIME              DEFAULT NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='门店';

-- 员工
CREATE TABLE IF NOT EXISTS t_staff (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    staff_no    VARCHAR(32)  NOT NULL COMMENT '工号',
    name        VARCHAR(32)  NOT NULL COMMENT '姓名',
    phone       VARCHAR(20)  NOT NULL COMMENT '手机号（登录账号）',
    password    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密）',
    store_id    BIGINT                DEFAULT NULL COMMENT '所属门店',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1在职 0停用',
    create_time DATETIME              DEFAULT NULL,
    update_time DATETIME              DEFAULT NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_staff_phone (phone, deleted),
    UNIQUE KEY uk_staff_no (staff_no, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='员工';

-- 角色
CREATE TABLE IF NOT EXISTS t_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(32)  NOT NULL COMMENT '角色编码',
    name        VARCHAR(32)  NOT NULL COMMENT '角色名称',
    remark      VARCHAR(128)          DEFAULT NULL,
    create_time DATETIME              DEFAULT NULL,
    update_time DATETIME              DEFAULT NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (code, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='角色';

-- 权限
CREATE TABLE IF NOT EXISTS t_permission (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(64)  NOT NULL COMMENT '权限编码，如 charge:scan',
    name        VARCHAR(64)  NOT NULL COMMENT '权限名称',
    module      VARCHAR(32)           DEFAULT NULL COMMENT '所属模块',
    create_time DATETIME              DEFAULT NULL,
    update_time DATETIME              DEFAULT NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_perm_code (code, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='权限';

-- 员工-角色
CREATE TABLE IF NOT EXISTS t_staff_role (
    id       BIGINT NOT NULL AUTO_INCREMENT,
    staff_id BIGINT NOT NULL,
    role_id  BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_staff_role (staff_id, role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='员工角色关联';

-- 角色-权限
CREATE TABLE IF NOT EXISTS t_role_permission (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='角色权限关联';

-- 会员（客户）
CREATE TABLE IF NOT EXISTS t_member (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    member_no   VARCHAR(32)  NOT NULL COMMENT '会员号',
    name        VARCHAR(32)           DEFAULT NULL COMMENT '姓名',
    phone       VARCHAR(20)  NOT NULL COMMENT '手机号',
    openid      VARCHAR(64)           DEFAULT NULL COMMENT '微信 openid',
    level       VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT '等级 NORMAL/GOLD/BLACK_GOLD',
    discount    INT          NOT NULL DEFAULT 100 COMMENT '折扣（百分比，92 表示 92 折）',
    points      INT          NOT NULL DEFAULT 0 COMMENT '积分',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1正常 0冻结',
    create_time DATETIME              DEFAULT NULL,
    update_time DATETIME              DEFAULT NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_phone (phone, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='会员';

-- 会员账户（余额）
CREATE TABLE IF NOT EXISTS t_member_account (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    member_id      BIGINT        NOT NULL COMMENT '会员ID',
    balance        DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '可用余额（本金+赠送）',
    total_recharge DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计充值',
    total_consume  DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费',
    version        INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    create_time    DATETIME               DEFAULT NULL,
    update_time    DATETIME               DEFAULT NULL,
    deleted        TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_member (member_id, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='会员账户';

-- 资金流水（只增不改）
CREATE TABLE IF NOT EXISTS t_wallet_txn (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    member_id      BIGINT        NOT NULL,
    biz_no         VARCHAR(64)   NOT NULL COMMENT '业务幂等号',
    type           VARCHAR(16)   NOT NULL COMMENT '类型 RECHARGE/CONSUME/REFUND/GIFT',
    amount         DECIMAL(12,2) NOT NULL COMMENT '变动金额（正数）',
    balance_before DECIMAL(12,2) NOT NULL COMMENT '变动前余额',
    balance_after  DECIMAL(12,2) NOT NULL COMMENT '变动后余额',
    ref_order_no   VARCHAR(64)            DEFAULT NULL COMMENT '关联订单号',
    staff_id       BIGINT                 DEFAULT NULL COMMENT '经办员工',
    store_id       BIGINT                 DEFAULT NULL,
    remark         VARCHAR(128)           DEFAULT NULL,
    create_time    DATETIME               DEFAULT NULL,
    update_time    DATETIME               DEFAULT NULL,
    deleted        TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_txn_bizno (biz_no, deleted),
    KEY idx_txn_member (member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='资金流水';

-- 茶室
CREATE TABLE IF NOT EXISTS t_tea_room (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    name         VARCHAR(64)   NOT NULL COMMENT '茶室名称',
    room_type    VARCHAR(32)            DEFAULT NULL COMMENT '类型：包厢/卡座',
    capacity     VARCHAR(32)            DEFAULT NULL COMMENT '容纳人数，如 4-6人',
    price_hour   DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '每小时价格',
    image        VARCHAR(255)           DEFAULT NULL,
    store_id     BIGINT                 DEFAULT NULL,
    status       TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1可用 0停用',
    create_time  DATETIME               DEFAULT NULL,
    update_time  DATETIME               DEFAULT NULL,
    deleted      TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='茶室';

-- 茶室预定
CREATE TABLE IF NOT EXISTS t_reservation (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    order_no      VARCHAR(32)   NOT NULL COMMENT '预定单号',
    member_id     BIGINT        NOT NULL,
    room_id       BIGINT        NOT NULL,
    reserve_date  DATE          NOT NULL COMMENT '预定日期',
    start_time    VARCHAR(8)    NOT NULL COMMENT '开始时段 如 15:00',
    hours         DECIMAL(4,1)  NOT NULL DEFAULT 1 COMMENT '时长（小时）',
    amount        DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '金额',
    status        VARCHAR(16)   NOT NULL DEFAULT 'WAITING' COMMENT 'WAITING待到店/USING进行中/VERIFIED已核销/CANCELLED已取消',
    verify_staff_id BIGINT               DEFAULT NULL COMMENT '核销员工',
    store_id      BIGINT                 DEFAULT NULL,
    create_time   DATETIME               DEFAULT NULL,
    update_time   DATETIME               DEFAULT NULL,
    deleted       TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reservation_no (order_no, deleted),
    UNIQUE KEY uk_room_slot (room_id, reserve_date, start_time, deleted),
    KEY idx_reservation_member (member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='茶室预定';

-- 货品
CREATE TABLE IF NOT EXISTS t_product (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    name         VARCHAR(128)  NOT NULL COMMENT '货品名称',
    barcode      VARCHAR(64)            DEFAULT NULL COMMENT '条码/SKU',
    category     VARCHAR(32)            DEFAULT NULL COMMENT '分类',
    spec         VARCHAR(64)            DEFAULT NULL COMMENT '规格/净含量',
    retail_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '零售价',
    member_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '会员价',
    stock        INT           NOT NULL DEFAULT 0 COMMENT '库存',
    warn_stock   INT           NOT NULL DEFAULT 0 COMMENT '预警阈值',
    channel      VARCHAR(32)            DEFAULT NULL COMMENT '销售渠道',
    image        VARCHAR(255)           DEFAULT NULL,
    status       TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1上架 0下架',
    store_id     BIGINT                 DEFAULT NULL,
    create_time  DATETIME               DEFAULT NULL,
    update_time  DATETIME               DEFAULT NULL,
    deleted      TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_barcode (barcode, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='货品';

-- 消费单
CREATE TABLE IF NOT EXISTS t_consume_order (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    order_no      VARCHAR(32)   NOT NULL COMMENT '消费单号',
    member_id     BIGINT        NOT NULL,
    staff_id      BIGINT                 DEFAULT NULL COMMENT '收款员工',
    store_id      BIGINT                 DEFAULT NULL,
    origin_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '原价',
    pay_amount    DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '折后实扣',
    pay_type      VARCHAR(16)   NOT NULL DEFAULT 'MEMBER_CARD' COMMENT '支付方式',
    status        VARCHAR(16)   NOT NULL DEFAULT 'PAID' COMMENT 'PAID已支付/REFUNDED已退款',
    remark        VARCHAR(128)           DEFAULT NULL,
    create_time   DATETIME               DEFAULT NULL,
    update_time   DATETIME               DEFAULT NULL,
    deleted       TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_consume_no (order_no, deleted),
    KEY idx_consume_member (member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='消费单';

-- 消费明细
CREATE TABLE IF NOT EXISTS t_consume_item (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    order_id    BIGINT        NOT NULL,
    item_type   VARCHAR(16)   NOT NULL COMMENT 'PRODUCT货品/ROOM茶室',
    item_name   VARCHAR(128)  NOT NULL COMMENT '名称快照',
    price       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    quantity    INT           NOT NULL DEFAULT 1,
    subtotal    DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    KEY idx_item_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='消费明细';

-- 扣款幂等记录，不随资金流水归档删除
CREATE TABLE IF NOT EXISTS t_charge_request (
    biz_no VARCHAR(64) PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    result_json TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员扣款幂等请求及原始回执';
