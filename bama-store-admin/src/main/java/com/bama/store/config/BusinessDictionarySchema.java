package com.bama.store.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;

@Component
@Order(-29)
@RequiredArgsConstructor
public class BusinessDictionarySchema implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (var connection = dataSource.getConnection()) {
            boolean h2 = "H2".equals(connection.getMetaData().getDatabaseProductName());
            jdbc.execute("CREATE TABLE IF NOT EXISTS t_business_dictionary ("
                    + "dict_key VARCHAR(64) PRIMARY KEY COMMENT '字典键', "
                    + "dict_value VARCHAR(16) NOT NULL COMMENT '字典值', "
                    + AuditColumns.definition(jdbc) + ")"
                    + (h2 ? "" : " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局业务字典'"));
            // Initialization never resets a saved value, including an archived entry.
            if (h2) {
                jdbc.update("INSERT INTO t_business_dictionary(dict_key,dict_value) "
                        + "SELECT 'business_enabled','0' WHERE NOT EXISTS "
                        + "(SELECT 1 FROM t_business_dictionary WHERE dict_key='business_enabled')");
            } else {
                jdbc.update("INSERT INTO t_business_dictionary(dict_key,dict_value) VALUES ('business_enabled','0') "
                        + "ON DUPLICATE KEY UPDATE dict_key=dict_key");
            }
        }
    }
}
