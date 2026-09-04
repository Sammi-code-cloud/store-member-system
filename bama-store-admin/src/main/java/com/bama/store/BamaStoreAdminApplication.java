package com.bama.store;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 八马门店后台管理系统 · 启动类
 */
@SpringBootApplication
@MapperScan("com.bama.store.mapper")
public class BamaStoreAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(BamaStoreAdminApplication.class, args);
    }
}
