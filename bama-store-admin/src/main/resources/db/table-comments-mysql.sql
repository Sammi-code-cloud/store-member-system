-- MySQL 表备注补全脚本，可重复执行。
-- 先完成 schema.sql 和应用启动时的扩展表初始化，再在目标业务库执行。
-- 仅修改表备注，不修改字段、索引或业务数据。
ALTER TABLE t_store COMMENT = '门店';
ALTER TABLE t_staff COMMENT = '员工';
ALTER TABLE t_role COMMENT = '角色';
ALTER TABLE t_permission COMMENT = '权限';
ALTER TABLE t_staff_role COMMENT = '员工角色关联';
ALTER TABLE t_role_permission COMMENT = '角色权限关联';
ALTER TABLE t_member COMMENT = '会员';
ALTER TABLE t_member_account COMMENT = '会员账户';
ALTER TABLE t_wallet_txn COMMENT = '资金流水';
ALTER TABLE t_tea_room COMMENT = '茶室';
ALTER TABLE t_reservation COMMENT = '茶室预定';
ALTER TABLE t_product COMMENT = '货品';
ALTER TABLE t_consume_order COMMENT = '消费单';
ALTER TABLE t_consume_item COMMENT = '消费明细';
ALTER TABLE t_staff_store COMMENT = '员工可访问门店关联（数据权限）';
ALTER TABLE t_room_closure COMMENT = '茶室暂停预订时段';
ALTER TABLE t_audit_log COMMENT = '后台操作审计日志';
ALTER TABLE t_banner COMMENT = '门店首页轮播图';
ALTER TABLE t_banner_initialized COMMENT = '门店默认轮播图初始化标记';
ALTER TABLE t_wechat_account COMMENT = '微信身份与员工、会员账号绑定关系';
ALTER TABLE t_wechat_flow COMMENT = '微信登录、绑定及扫码流程临时凭证';
ALTER TABLE t_charge_request COMMENT = '会员扣款幂等请求及原始回执';
