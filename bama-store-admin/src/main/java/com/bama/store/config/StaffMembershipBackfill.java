package com.bama.store.config;

import com.bama.store.service.StaffWechatMembership;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component @Order(100) @RequiredArgsConstructor @Slf4j
public class StaffMembershipBackfill implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final StaffWechatMembership membership;
    public void run(ApplicationArguments args) {
        int completed=0,skipped=0;
        var ids=jdbc.queryForList("SELECT DISTINCT a.account_id FROM t_wechat_account a JOIN t_staff s ON s.id=a.account_id WHERE a.audience='STAFF' AND s.status=1 AND s.deleted=0",Long.class);
        for(var id:ids) {
            try {membership.synchronize(id);completed++;}
            catch(Exception e) {skipped++;log.warn("Staff membership backfill skipped staffId={}, errorType={}",id,e.getClass().getSimpleName());}
        }
        log.info("Staff membership backfill completed={}, skipped={}",completed,skipped);
    }
}
