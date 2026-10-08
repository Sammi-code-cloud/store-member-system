package com.bama.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

@Service @RequiredArgsConstructor
public class WxPusherRecipients {
    private final JdbcTemplate jdbc;

    public List<String> recipients(long storeId) {
        return jdbc.queryForList("SELECT DISTINCT w.uid FROM t_staff_wxpusher w "
                + "JOIN t_staff s ON s.id=w.staff_id WHERE w.store_id=? AND s.deleted=0 AND s.status=1 "
                + "AND (s.store_id=w.store_id OR EXISTS (SELECT 1 FROM t_staff_store ss "
                + "WHERE ss.staff_id=s.id AND ss.store_id=w.store_id))", String.class, storeId);
    }
}
