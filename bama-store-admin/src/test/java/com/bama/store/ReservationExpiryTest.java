package com.bama.store;
import com.bama.store.service.ReservationExpiry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:expiry;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE","logging.level.root=ERROR","logging.level.com.bama.store=ERROR","mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"}) @ActiveProfiles("h2")
class ReservationExpiryTest {
 @Autowired ReservationExpiry expiry;@Autowired JdbcTemplate jdbc;
 String add(String status,String date,String time){String order="EXP-"+UUID.randomUUID().toString().substring(0,16);jdbc.update("INSERT INTO t_reservation(order_no,member_id,room_id,store_id,reserve_date,start_time,hours,amount,status,deleted) VALUES(?,1,1,1,?,?,1,100,?,0)",order,date,time,status);return order;}
 String status(String order){return jdbc.queryForObject("SELECT status FROM t_reservation WHERE order_no=?",String.class,order);}
 @Test void onlyElapsedPendingRequestsExpireAndRetriesDoNotDuplicateAudit(){
  String past=add("PENDING","2099-03-08","23:00"),boundary=add("PENDING","2099-03-09","12:00"),future=add("PENDING","2099-03-09","12:01"),nextDay=add("PENDING","2099-03-10","00:00"),confirmed=add("WAITING","2099-03-09","11:00"),using=add("USING","2099-03-09","11:00");
  var now=LocalDateTime.of(2099,3,9,12,0);expiry.expireAt(now);expiry.expireAt(now);
  assertThat(status(past)).isEqualTo("REJECTED");assertThat(status(boundary)).isEqualTo("REJECTED");assertThat(status(future)).isEqualTo("PENDING");assertThat(status(nextDay)).isEqualTo("PENDING");assertThat(status(confirmed)).isEqualTo("WAITING");assertThat(status(using)).isEqualTo("USING");
  assertThat(jdbc.queryForObject("SELECT cancel_reason FROM t_reservation WHERE order_no=?",String.class,boundary)).contains("系统自动拒绝");
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_audit_log WHERE target=? AND actor='系统'",Integer.class,boundary)).isEqualTo(1);
 }
 @Test void concurrentSweepsWriteOneTransition()throws Exception {
  String order=add("PENDING","2098-03-09","12:00");var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
  try{var calls=List.<java.util.concurrent.Callable<Integer>>of(()->expiry.expireAt(LocalDateTime.of(2098,3,9,13,0)),()->expiry.expireAt(LocalDateTime.of(2098,3,9,13,0)));for(var result:executor.invokeAll(calls))result.get();}finally{executor.shutdownNow();}
  assertThat(status(order)).isEqualTo("REJECTED");assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_audit_log WHERE target=?",Integer.class,order)).isEqualTo(1);
 }
}
