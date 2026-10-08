package com.bama.store.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component @Order(-50) @RequiredArgsConstructor
public class WalletSmsSchema implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public void run(ApplicationArguments args) {
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_sms_wallet_notice (event_key VARCHAR(128) PRIMARY KEY, "
            +"member_id BIGINT NOT NULL, store_id BIGINT NOT NULL, phone VARCHAR(32), sign_name VARCHAR(128) NOT NULL, "
            +"template_code VARCHAR(64) NOT NULL, payload TEXT NOT NULL, status VARCHAR(24) NOT NULL, "
            +"error_code VARCHAR(64), provider_id VARCHAR(128), create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            +"update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, INDEX idx_sms_wallet_status(status,create_time))");
    }
}
