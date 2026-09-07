package com.bama.store.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

/** Additive, repeatable upgrade for the original MySQL/H2 schema. No business rows are removed. */
@Component
@Order(-100)
@RequiredArgsConstructor
public class AdminSchemaUpgrade implements ApplicationRunner {
    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            boolean h2 = connection.getMetaData().getDatabaseProductName().equals("H2");
            add(connection, "t_member", Map.of("username", "VARCHAR(40)", "password", "VARCHAR(100)",
                    "last_login_time", "TIMESTAMP NULL", "remark", "VARCHAR(500)"));
            try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, "t_member", "phone")) {
                if (columns.next() && columns.getInt("NULLABLE") == 0) {
                    jdbc.execute(h2 ? "ALTER TABLE t_member ALTER COLUMN phone DROP NOT NULL"
                            : "ALTER TABLE t_member MODIFY phone VARCHAR(20) NULL");
                }
            }
            if (!hasIndex(connection, "t_member", "uk_member_username")) {
                jdbc.execute("CREATE UNIQUE INDEX uk_member_username ON t_member(username)");
            }
            Map<String, String> room = new LinkedHashMap<>();
            room.put("description", "VARCHAR(1000)"); room.put("facilities", "VARCHAR(255)");
            room.put("open_time", "VARCHAR(5) DEFAULT '10:00'"); room.put("close_time", "VARCHAR(5) DEFAULT '22:00'");
            room.put("min_hours", "DECIMAL(4,1) DEFAULT 1"); room.put("advance_days", "INT DEFAULT 30");
            room.put("sort_order", "INT DEFAULT 0");
            add(connection, "t_tea_room", room);
            add(connection, "t_store", Map.of("open_time", "VARCHAR(5) DEFAULT '10:00'", "close_time", "VARCHAR(5) DEFAULT '22:00'",
                    "reservation_notice", "VARCHAR(1000)"));
            add(connection, "t_reservation", Map.of("room_name", "VARCHAR(64)", "contact_name", "VARCHAR(64)",
                    "contact_phone", "VARCHAR(20)", "guests", "INT DEFAULT 1", "remark", "VARCHAR(500)",
                    "cancel_reason", "VARCHAR(255)", "source", "VARCHAR(16) DEFAULT 'STAFF'", "price_hour", "DECIMAL(10,2)"));
            // A start-time unique key prevents rebooking cancelled orders and misses interval overlap.
            // ReservationService now serializes mutations by locking the room row, then checks intervals.
            if (hasIndex(connection, "t_reservation", "uk_room_slot")) {
                jdbc.execute(h2 ? "ALTER TABLE t_reservation DROP CONSTRAINT uk_room_slot"
                        : "ALTER TABLE t_reservation DROP INDEX uk_room_slot");
            }
            jdbc.execute("CREATE TABLE IF NOT EXISTS t_room_closure (id BIGINT AUTO_INCREMENT PRIMARY KEY, room_id BIGINT NOT NULL, closure_date DATE NOT NULL, start_time VARCHAR(5) NOT NULL, end_time VARCHAR(5) NOT NULL, reason VARCHAR(255), store_id BIGINT, create_time TIMESTAMP NULL, update_time TIMESTAMP NULL, deleted TINYINT DEFAULT 0 NOT NULL)");
            jdbc.execute("CREATE TABLE IF NOT EXISTS t_audit_log (id BIGINT AUTO_INCREMENT PRIMARY KEY, staff_id BIGINT, store_id BIGINT, actor VARCHAR(64), action VARCHAR(64), target VARCHAR(128), detail VARCHAR(2000), create_time TIMESTAMP NULL, update_time TIMESTAMP NULL, deleted TINYINT DEFAULT 0 NOT NULL)");
            for (String[] p : new String[][]{{"account:view", "资金流水", "account"}, {"audit:view", "操作记录", "audit"}}) {
                jdbc.update("INSERT INTO t_permission(code,name,module) SELECT ?,?,? WHERE NOT EXISTS (SELECT 1 FROM t_permission WHERE code=? AND deleted=0)", p[0], p[1], p[2], p[0]);
                jdbc.update("INSERT INTO t_role_permission(role_id,permission_id) SELECT r.id,p.id FROM t_role r,t_permission p WHERE r.code='STORE_MANAGER' AND r.deleted=0 AND p.code=? AND p.deleted=0 AND NOT EXISTS(SELECT 1 FROM t_role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id)", p[0]);
            }
        }
    }

    private void add(Connection c, String table, Map<String, String> columns) throws Exception {
        for (var entry : columns.entrySet()) {
            try (ResultSet result = c.getMetaData().getColumns(c.getCatalog(), null, table, entry.getKey())) {
                if (!result.next()) jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + entry.getKey() + " " + entry.getValue());
            }
        }
    }

    private boolean hasIndex(Connection c, String table, String name) throws Exception {
        try (ResultSet result = c.getMetaData().getIndexInfo(c.getCatalog(), null, table, false, false)) {
            while (result.next()) {
                String index = result.getString("INDEX_NAME");
                if (index != null && (index.equalsIgnoreCase(name) || index.toLowerCase().startsWith(name + "_index"))) return true;
            }
        }
        return false;
    }
}
