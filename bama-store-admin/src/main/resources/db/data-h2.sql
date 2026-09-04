-- H2 版初始化数据（内存库每次启动重建，使用普通 INSERT）

INSERT INTO t_store (id, name, address, phone, status, create_time, update_time)
VALUES (1, '五缘湾旗舰店', '厦门市湖里区五缘湾', '0592-8888888', 1, NOW(), NOW());

INSERT INTO t_permission (id, code, name, module) VALUES
(1,  'dashboard:view',     '数据概览',   'dashboard'),
(2,  'member:view',        '会员查看',   'member'),
(3,  'member:manage',      '会员管理',   'member'),
(4,  'account:recharge',   '会员储值',   'account'),
(5,  'charge:scan',        '扫码收款',   'charge'),
(6,  'reservation:view',   '预定查看',   'reservation'),
(7,  'reservation:manage', '预定管理',   'reservation'),
(8,  'reservation:verify', '核销预定',   'reservation'),
(9,  'product:view',       '货品查看',   'product'),
(10, 'product:manage',     '货品管理',   'product'),
(11, 'staff:view',         '员工查看',   'staff'),
(12, 'staff:manage',       '员工管理',   'staff'),
(13, 'role:view',          '角色查看',   'role'),
(14, 'role:manage',        '角色管理',   'role'),
(15, 'store:manage',       '门店设置',   'store');

INSERT INTO t_role (id, code, name, remark) VALUES
(1, 'STORE_MANAGER', '店长',   '门店全部权限'),
(2, 'CASHIER',       '收银员', '扫码收款、核销、储值'),
(3, 'TEA_ARTIST',    '茶艺师', '预定核销'),
(4, 'STORE_KEEPER',  '仓管',   '货品管理');

INSERT INTO t_role_permission (role_id, permission_id) VALUES
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9),(1,10),(1,11),(1,12),(1,13),(1,14),(1,15),
(2,1),(2,2),(2,4),(2,5),(2,6),(2,8),(2,9),
(3,6),(3,8),
(4,9),(4,10);

INSERT INTO t_member (id, member_no, name, phone, level, discount, points, status, create_time, update_time) VALUES
(1, 'M20260001', '陈明轩', '13800006620', 'BLACK_GOLD', 92, 1880, 1, NOW(), NOW()),
(2, 'M20260002', '周静怡', '15900003308', 'BLACK_GOLD', 92, 2100, 1, NOW(), NOW());

INSERT INTO t_member_account (id, member_id, balance, total_recharge, total_consume, version, create_time, update_time) VALUES
(1, 1, 2860.00, 30000.00, 27140.00, 0, NOW(), NOW()),
(2, 2, 5420.00, 50000.00, 44580.00, 0, NOW(), NOW());

INSERT INTO t_tea_room (id, name, room_type, capacity, price_hour, store_id, status, create_time, update_time) VALUES
(1, '观山茶室', '包厢', '4-6人', 188.00, 1, 1, NOW(), NOW()),
(2, '听雨阁',   '卡座', '2-4人', 128.00, 1, 1, NOW(), NOW()),
(3, '云雾厅',   '包厢', '6-8人', 288.00, 1, 1, NOW(), NOW());

INSERT INTO t_product (id, name, barcode, category, spec, retail_price, member_price, stock, warn_stock, channel, status, store_id, create_time, update_time) VALUES
(1, '赛珍珠1000 浓香型铁观音', '6928447100218', '铁观音', '250g 铁罐', 520.00, 478.00, 168, 20, 'STORE,MINI', 1, 1, NOW(), NOW()),
(2, '大红袍 传统炭焙',        '6928447100225', '岩茶',   '125g 礼盒', 320.00, 288.00, 96,  15, 'STORE,MINI', 1, 1, NOW(), NOW()),
(3, '金骏眉 特级红茶',        '6928447100232', '红茶',   '100g 精装', 480.00, 439.00, 60,  10, 'STORE,MINI', 1, 1, NOW(), NOW());

-- 一条今日待核销预定（便于工作台演示）
INSERT INTO t_reservation (id, order_no, member_id, room_id, reserve_date, start_time, hours, amount, status, store_id, create_time, update_time)
VALUES (1, 'RS-DEMO-0001', 1, 1, CURRENT_DATE, '15:00', 2, 376.00, 'WAITING', 1, NOW(), NOW());
