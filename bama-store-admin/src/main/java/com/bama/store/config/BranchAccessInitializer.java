package com.bama.store.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Only the bootstrap administrator receives headquarters access during migration. */
@Component
@Order(100)
@RequiredArgsConstructor
public class BranchAccessInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM t_role WHERE code='HEADQUARTERS'", Long.class) > 0) return;
        jdbc.update("INSERT INTO t_permission(code,name,module) VALUES ('store:all','管理全部分店','门店')");
        jdbc.update("INSERT INTO t_role(code,name,remark) VALUES ('HEADQUARTERS','总部管理员','可切换及新增分店')");
        jdbc.update("INSERT INTO t_role_permission(role_id,permission_id) SELECT r.id,p.id FROM t_role r CROSS JOIN t_permission p WHERE r.code='HEADQUARTERS' AND p.deleted=0");
        jdbc.update("INSERT INTO t_staff_role(staff_id,role_id) SELECT s.id,r.id FROM t_staff s CROSS JOIN t_role r WHERE s.staff_no='BM-ADMIN' AND s.deleted=0 AND r.code='HEADQUARTERS'");
    }
}
