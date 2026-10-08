package com.bama.store.service;

import com.bama.store.config.WxPusherProperties;
import com.bama.store.entity.Reservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
public class ReservationNotices {
    private final JdbcTemplate jdbc;
    private final WxPusherProperties config;
    private final WxPusherClient client;
    private final WxPusherRecipients recipients;
    private final PlatformTransactionManager transactions;

    /** The reservation and durable notification must commit or roll back together. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(Reservation r) {
        String content = "【新房间预约】\n订单：" + r.getOrderNo()
                + "\n门店编号：" + r.getStoreId() + "\n房间：" + r.getRoomName()
                + "\n日期：" + r.getReserveDate() + "\n开始时间：" + r.getStartTime()
                + "\n时长：" + r.getHours().stripTrailingZeros().toPlainString() + "小时"
                + "\n人数：" + r.getGuests() + "\n联系人：" + r.getContactName()
                + "\n联系电话：" + Objects.toString(r.getContactPhone(), "")
                + "\n备注：" + Objects.toString(r.getRemark(), "无")
                + "\n请打开员工端或管理后台处理预约。";
        jdbc.update("INSERT INTO t_reservation_notice(event_key,store_id,payload,status,next_attempt) VALUES(?,?,?,'NEW',?)",
                r.getOrderNo(), r.getStoreId(), content, now());
    }

    public void dispatch() {
        if (!config.isEnabled()) return;
        if (config.getAppToken() == null || !config.getAppToken().startsWith("AT_")) {
            log.error("WxPusher enabled but app token missing/invalid; reservation notices remain queued");
            return;
        }
        var events = jdbc.queryForList("SELECT event_key FROM t_reservation_notice WHERE status='NEW' AND next_attempt<=? ORDER BY next_attempt LIMIT 50", now());
        for (var event : events) expand((String) event.get("event_key"));
        var pending = jdbc.queryForList("SELECT event_key FROM t_reservation_notice WHERE status IN ('PENDING','SENDING') AND next_attempt<=? ORDER BY next_attempt LIMIT 50", now());
        for (var row : pending) deliver((String) row.get("event_key"));
    }

    private void expand(String key) {
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            var event = jdbc.queryForMap("SELECT * FROM t_reservation_notice WHERE event_key=? FOR UPDATE", key);
            if (!"NEW".equals(event.get("status"))) return;
            long storeId = ((Number) event.get("store_id")).longValue();
            var uids = recipients.recipients(storeId);
            if (uids.isEmpty()) {
                jdbc.update("UPDATE t_reservation_notice SET error_code='NO_STORE_RECIPIENTS',next_attempt=? WHERE event_key=?",
                        Timestamp.from(Instant.now().plusSeconds(60)), key);
                log.error("Reservation notice {} waiting for store {} WxPusher recipients", key, storeId);
                return;
            }
            for (String uid : uids) jdbc.update("INSERT INTO t_reservation_notice(event_key,store_id,receiver_uid,payload,status,next_attempt) VALUES(?,?,?,?,'PENDING',?)",
                    key + ":" + uid, storeId, uid, event.get("payload"), now());
            jdbc.update("UPDATE t_reservation_notice SET status='EXPANDED',error_code=NULL WHERE event_key=?", key);
        });
    }

    private void deliver(String key) {
        String lease = UUID.randomUUID().toString();
        // Atomic lease handles multiple server instances and recovers interrupted sends after restart.
        if (jdbc.update("UPDATE t_reservation_notice SET status='SENDING',lease_token=?,attempts=attempts+1,next_attempt=? WHERE event_key=? AND status IN ('PENDING','SENDING') AND next_attempt<=?",
                lease, Timestamp.from(Instant.now().plusSeconds(60)), key, now()) != 1) return;
        var row = jdbc.queryForMap("SELECT * FROM t_reservation_notice WHERE event_key=?", key);
        String uid = (String) row.get("receiver_uid");
        long storeId = ((Number) row.get("store_id")).longValue();
        if (!recipients.recipients(storeId).contains(uid)) {
            jdbc.update("UPDATE t_reservation_notice SET status='REMOVED',error_code='RECIPIENT_REMOVED',lease_token=NULL WHERE event_key=? AND lease_token=?", key, lease);
            return;
        }
        WxPusherClient.Delivery result;
        try {
            result = client.send(uid, (String) row.get("payload"));
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            result = new WxPusherClient.Delivery(false, null, "TRANSPORT_ERROR");
        }
        if (result.accepted()) {
            jdbc.update("UPDATE t_reservation_notice SET status='ACCEPTED',message_id=?,error_code=NULL,lease_token=NULL WHERE event_key=? AND lease_token=?",
                    result.messageId(), key, lease);
        } else {
            int attempts = ((Number) row.get("attempts")).intValue();
            long delay = Math.min(3600, 5L * (1L << Math.min(attempts, 10)));
            jdbc.update("UPDATE t_reservation_notice SET status='PENDING',error_code=?,next_attempt=?,lease_token=NULL WHERE event_key=? AND lease_token=?",
                    result.errorCode(), Timestamp.from(Instant.now().plusSeconds(delay)), key, lease);
            log.warn("Reservation notice {} failed ({}); retry in {} seconds", key, result.errorCode(), delay);
        }
    }

    // Match MySQL DATETIME's second precision so freshly queued tasks cannot round into the future.
    private static Timestamp now() { return Timestamp.from(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)); }
}
