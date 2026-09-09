package com.bama.store.service;
import com.bama.store.entity.AuditLog;
import com.bama.store.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.DateTimeFormatter;

/** Expire only unanswered requests; row locks serialize with staff confirmation. */
@Service @RequiredArgsConstructor
public class ReservationExpiry {
 private final JdbcTemplate jdbc;
 private final AuditLogMapper audits;
 @Transactional public int expire(){return expireAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));}
 @Transactional public int expireAt(LocalDateTime now){
  var day=java.sql.Date.valueOf(now.toLocalDate());String time=now.format(DateTimeFormatter.ofPattern("HH:mm"));
  var rows=jdbc.queryForList("SELECT id,order_no,store_id FROM t_reservation WHERE deleted=0 AND status='PENDING' AND (reserve_date<? OR (reserve_date=? AND start_time<=?)) ORDER BY id LIMIT 500 FOR UPDATE",day,day,time);
  int count=0;
  for(var row:rows){
   int changed=jdbc.update("UPDATE t_reservation SET status='REJECTED',cancel_reason=?,update_time=? WHERE id=? AND status='PENDING' AND deleted=0","预约开始时间已过，系统自动拒绝",java.sql.Timestamp.valueOf(now),row.get("id"));
   if(changed==0)continue;
   AuditLog log=new AuditLog();log.setActor("系统");log.setStoreId(((Number)row.get("store_id")).longValue());log.setAction("预约超时自动拒绝");log.setTarget(String.valueOf(row.get("order_no")));log.setDetail("待确认 → 已拒绝：预约开始时间已过");audits.insert(log);count++;
  }
  return count;
 }
}
