package com.bama.store.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;

@Component @Order(-30) @RequiredArgsConstructor
public class ChargeIdempotencySchema implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    public void run(ApplicationArguments args) throws Exception {
        try(var connection=dataSource.getConnection()) {
            boolean h2="H2".equals(connection.getMetaData().getDatabaseProductName());
            jdbc.execute("CREATE TABLE IF NOT EXISTS t_charge_request (biz_no VARCHAR(64) PRIMARY KEY, request_hash VARCHAR(64) NOT NULL, result_json TEXT, " + AuditColumns.definition(jdbc) + ")"
                    +(h2?"":" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员扣款幂等请求及原始回执'"));
        }
    }
}
