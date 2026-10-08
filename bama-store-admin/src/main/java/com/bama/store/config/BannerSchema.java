package com.bama.store.config;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component @Order(-40) @RequiredArgsConstructor
public class BannerSchema implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public void run(ApplicationArguments args) {
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_banner(id BIGINT AUTO_INCREMENT PRIMARY KEY,store_id BIGINT NOT NULL,title VARCHAR(60) NOT NULL,image_data LONGTEXT NOT NULL,sort_order INT NOT NULL DEFAULT 0,status INT NOT NULL DEFAULT 1,target VARCHAR(16) NOT NULL DEFAULT 'none',version VARCHAR(36) NOT NULL, " + AuditColumns.definition(jdbc) + ")");
    }
}
