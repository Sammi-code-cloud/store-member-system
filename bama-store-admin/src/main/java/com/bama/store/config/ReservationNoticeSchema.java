package com.bama.store.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component @Order(-50) @RequiredArgsConstructor
public class ReservationNoticeSchema implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public void run(ApplicationArguments args) {
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_staff_wxpusher (store_id BIGINT NOT NULL, staff_id BIGINT NOT NULL, "
                + "uid VARCHAR(110) NOT NULL, PRIMARY KEY(store_id,staff_id), " + AuditColumns.definition(jdbc) + ")");
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_reservation_notice ("
                + "event_key VARCHAR(200) PRIMARY KEY, store_id BIGINT NOT NULL, "
                + "receiver_uid VARCHAR(110) NOT NULL DEFAULT '', payload TEXT NOT NULL, "
                + "status VARCHAR(24) NOT NULL, attempts INTEGER NOT NULL DEFAULT 0, "
                + "next_attempt DATETIME NOT NULL, lease_token VARCHAR(36), "
                + "message_id VARCHAR(100), error_code VARCHAR(64), "
                + AuditColumns.definition(jdbc) + ", INDEX idx_reservation_notice_due(status,next_attempt))");
    }
}
