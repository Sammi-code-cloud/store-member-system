package com.bama.store.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Stable bootstrap identity; credentials are supplied only through private deployment configuration. */
@Component @Profile("!h2") @Order(0) @RequiredArgsConstructor
public class DefaultAdministrator implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    @Value("${bama.bootstrap.phone:}") private String phone;
    @Value("${bama.bootstrap.password:}") private String password;

    @Override @Transactional
    public void run(ApplicationArguments args) {
        var existing = jdbc.queryForList("SELECT id,deleted FROM t_staff WHERE staff_no='BM-ADMIN'");
        if (existing.isEmpty()) {
            if (phone.isBlank() && password.isBlank()) return;
            if (!(phone.equals("admin") || phone.matches("1[3-9]\\d{9}")) || password.length() < 8 || password.length() > 64)
                throw new IllegalStateException("请在私有配置中设置 admin 或有效手机号，以及 8–64 位初始密码");
            if (jdbc.queryForObject("SELECT COUNT(*) FROM t_staff WHERE phone=?", Long.class, phone) > 0)
                throw new IllegalStateException("默认管理员手机号已有员工使用，请核实账号，不能自动提升其权限");
            jdbc.update("INSERT INTO t_staff(staff_no,name,phone,password,status) VALUES('BM-ADMIN','总部管理员',?,?,1)", phone, passwords.encode(password));
        }
        // Deleted/disabled accounts are never reactivated and an existing password is never reset.
        var ids = jdbc.queryForList("SELECT id FROM t_staff WHERE staff_no='BM-ADMIN' AND deleted=0 AND status=1", Long.class);
        if (ids.isEmpty()) return;
        if (ids.size() != 1) throw new IllegalStateException("默认管理员工号重复，请核实账号");
        Long admin = ids.get(0);
        String[][] definitions = {
            {"dashboard:view","数据概览","dashboard"}, {"member:view","会员查看","member"},
            {"member:manage","会员管理","member"}, {"account:recharge","会员储值","account"},
            {"charge:scan","扫码收款","charge"}, {"reservation:view","预定查看","reservation"},
            {"reservation:manage","预定管理","reservation"}, {"reservation:verify","核销预定","reservation"},
            {"product:view","货品查看","product"}, {"product:manage","货品管理","product"},
            {"staff:view","员工查看","staff"}, {"staff:manage","员工管理","staff"},
            {"role:view","角色查看","role"}, {"role:manage","角色管理","role"},
            {"store:manage","门店设置","store"}, {"account:view","资金流水","account"},
            {"audit:view","操作记录","audit"}, {"store:all","管理全部分店","store"}
        };
        for (var p : definitions)
            jdbc.update("INSERT INTO t_permission(code,name,module) SELECT ?,?,? WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code=? AND deleted=0)", p[0],p[1],p[2],p[0]);
        jdbc.update("INSERT INTO t_role(code,name,remark) SELECT 'HEADQUARTERS','总部管理员','固定管理员，管理全部分店' WHERE NOT EXISTS(SELECT 1 FROM t_role WHERE code='HEADQUARTERS' AND deleted=0)");
        jdbc.update("INSERT INTO t_role_permission(role_id,permission_id) SELECT r.id,p.id FROM t_role r CROSS JOIN t_permission p WHERE r.code='HEADQUARTERS' AND r.deleted=0 AND p.deleted=0 AND NOT EXISTS(SELECT 1 FROM t_role_permission x WHERE x.role_id=r.id AND x.permission_id=p.id)");
        jdbc.update("INSERT INTO t_staff_role(staff_id,role_id) SELECT ?,r.id FROM t_role r WHERE r.code='HEADQUARTERS' AND r.deleted=0 AND NOT EXISTS(SELECT 1 FROM t_staff_role x WHERE x.staff_id=? AND x.role_id=r.id)", admin,admin);
        var stores = jdbc.queryForList("SELECT id FROM t_store WHERE deleted=0 AND status=1 ORDER BY id LIMIT 1", Long.class);
        if (!stores.isEmpty()) attachToFirstStore(stores.get(0));
    }

    /** Called when an active store is created after initialization in an empty database. */
    @Transactional
    public void attachToFirstStore(Long storeId) {
        jdbc.update("UPDATE t_staff SET store_id=? WHERE staff_no='BM-ADMIN' AND deleted=0 AND status=1 AND store_id IS NULL AND EXISTS(SELECT 1 FROM t_store WHERE id=? AND deleted=0 AND status=1)",storeId,storeId);
        jdbc.update("INSERT INTO t_staff_store(staff_id,store_id) SELECT s.id,s.store_id FROM t_staff s WHERE s.staff_no='BM-ADMIN' AND s.deleted=0 AND s.status=1 AND s.store_id=? AND NOT EXISTS(SELECT 1 FROM t_staff_store x WHERE x.staff_id=s.id AND x.store_id=s.store_id)",storeId);
    }
}
