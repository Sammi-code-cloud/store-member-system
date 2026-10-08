package com.bama.store.config;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Component @Order(200) @RequiredArgsConstructor
public class BannerDefaults implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    @Transactional public void run(ApplicationArguments args) throws Exception {
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_banner_initialized(store_id BIGINT PRIMARY KEY, " + AuditColumns.definition(jdbc) + ")");
        try(var stream=getClass().getResourceAsStream("/banners/tea-welcome.png")) {
            if(stream==null)return;
            String image="data:image/png;base64,"+Base64.getEncoder().encodeToString(stream.readAllBytes());
            for(Long store:jdbc.queryForList("SELECT id FROM t_store WHERE deleted=0 AND id NOT IN (SELECT store_id FROM t_banner_initialized)",Long.class)) {
                if(jdbc.queryForObject("SELECT COUNT(*) FROM t_banner WHERE store_id=?",Integer.class,store)==0) {
                    jdbc.update("INSERT INTO t_banner(store_id,title,image_data,sort_order,status,target,version) VALUES(?,?,?,0,1,'rooms',?)",store,"好茶相伴 · 自在小坐",image,UUID.randomUUID().toString());
                    try(var room=getClass().getResourceAsStream("/banners/tea-room.jpg")) {
                        if(room!=null)jdbc.update("INSERT INTO t_banner(store_id,title,image_data,sort_order,status,target,version) VALUES(?,?,?,1,1,'rooms',?)",store,"寻一间茶室 · 与知己共饮","data:image/jpeg;base64,"+Base64.getEncoder().encodeToString(room.readAllBytes()),UUID.randomUUID().toString());
                    }
                }
                jdbc.update("INSERT INTO t_banner_initialized(store_id) VALUES(?)",store);
            }
        }
    }
}
