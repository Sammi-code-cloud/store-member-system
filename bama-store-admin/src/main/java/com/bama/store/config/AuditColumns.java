package com.bama.store.config;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

/** Common fields for new tables. Existing MySQL tables use the backed-up migration script. */
public final class AuditColumns {
    private AuditColumns() {}

    public static String definition(JdbcTemplate jdbc) {
        boolean h2 = Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection ->
                "H2".equals(connection.getMetaData().getDatabaseProductName())));
        return "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', "
                + "update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间', "
                + (h2 ? "deleted INTEGER NOT NULL DEFAULT 0" : "deleted BIT(1) NOT NULL DEFAULT b'0'")
                + " COMMENT '删除标识'";
    }
}
