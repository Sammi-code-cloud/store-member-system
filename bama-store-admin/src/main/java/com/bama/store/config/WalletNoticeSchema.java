package com.bama.store.config;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component @Order(-50) @RequiredArgsConstructor
public class WalletNoticeSchema implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public void run(ApplicationArguments args) {
        String audit=AuditColumns.definition(jdbc);
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_wx_wallet_receiver (app_id VARCHAR(64) NOT NULL, member_id BIGINT NOT NULL, open_id VARCHAR(128) NOT NULL, PRIMARY KEY(app_id,member_id), UNIQUE(app_id,open_id), "+audit+")");
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_wx_wallet_notice (event_key VARCHAR(128) PRIMARY KEY, member_id BIGINT NOT NULL, app_id VARCHAR(64) NOT NULL, open_id VARCHAR(128), template_id VARCHAR(128), payload TEXT NOT NULL, status VARCHAR(24) NOT NULL, error_code VARCHAR(40), "+audit+")");
    }
}
