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
  jdbc.execute("CREATE TABLE IF NOT EXISTS t_wechat_phone_verified (member_id BIGINT PRIMARY KEY, phone VARCHAR(20) NOT NULL UNIQUE, verified_at TIMESTAMP NOT NULL)");
  jdbc.execute("CREATE TABLE IF NOT EXISTS t_sms_guard (id INT PRIMARY KEY)");
  if(jdbc.queryForObject("SELECT COUNT(*) FROM t_sms_guard WHERE id=1",Integer.class)==0) jdbc.update("INSERT INTO t_sms_guard(id) VALUES(1)");
  jdbc.execute("CREATE TABLE IF NOT EXISTS t_sms_challenge (token_hash VARCHAR(64) PRIMARY KEY, ticket_hash VARCHAR(64) NOT NULL, phone_hash VARCHAR(64) NOT NULL, identity_hash VARCHAR(64) NOT NULL, ip_hash VARCHAR(64) NOT NULL, code_hash VARCHAR(100) NOT NULL, created_at TIMESTAMP NOT NULL, expires_at TIMESTAMP NOT NULL, attempts INT NOT NULL DEFAULT 0, ready INT NOT NULL DEFAULT 0, consumed INT NOT NULL DEFAULT 0)");
 }
}
