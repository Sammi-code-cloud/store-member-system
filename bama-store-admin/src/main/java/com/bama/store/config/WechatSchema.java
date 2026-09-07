package com.bama.store.config;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component @Order(-50) @RequiredArgsConstructor
public class WechatSchema implements ApplicationRunner {
 private final JdbcTemplate jdbc;
 public void run(ApplicationArguments args) {
  jdbc.execute("CREATE TABLE IF NOT EXISTS t_wechat_account (identity_key VARCHAR(255) NOT NULL, audience VARCHAR(16) NOT NULL, account_id BIGINT NOT NULL, PRIMARY KEY(identity_key,audience))");
  jdbc.execute("CREATE TABLE IF NOT EXISTS t_wechat_flow (token_hash VARCHAR(64) PRIMARY KEY, kind VARCHAR(16) NOT NULL, payload VARCHAR(4000) NOT NULL, expires_at TIMESTAMP NOT NULL)");
 }
}
