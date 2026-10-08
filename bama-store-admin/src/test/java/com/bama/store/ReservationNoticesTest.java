package com.bama.store;

import com.bama.store.config.WxPusherProperties;
import com.bama.store.entity.Reservation;
import com.bama.store.security.LoginCustomer;
import com.bama.store.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:reservation_notices;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "bama.wxpusher.poll-ms=3600000", "logging.level.root=ERROR", "logging.level.com.bama.store=ERROR",
        "mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"})
@ActiveProfiles("h2")
@org.springframework.test.context.jdbc.Sql(statements = "UPDATE t_business_dictionary SET dict_value='1' WHERE dict_key='business_enabled'")
class ReservationNoticesTest {
    @Autowired ReservationNotices notices;
    @Autowired ReservationService reservations;
    @Autowired WxPusherProperties config;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @MockBean WxPusherClient client;
    @MockBean com.bama.store.config.ReservationNoticeSchedule schedule;
    @org.springframework.boot.test.mock.mockito.SpyBean BookingNotices bookingNotices;

    @BeforeEach void setup() {
        jdbc.update("DELETE FROM t_reservation_notice");
        config.setEnabled(true); config.setAppToken("AT_test");
        jdbc.update("DELETE FROM t_staff_wxpusher");
        jdbc.update("UPDATE t_staff SET status=1 WHERE id IN (1,2)");
        jdbc.update("INSERT INTO t_staff_wxpusher(store_id,staff_id,uid) VALUES(1,1,'UID_one'),(1,2,'UID_two'),(2,1,'UID_other')");
    }
    @AfterEach void cleanup() { config.setEnabled(false); SecurityContextHolder.clearContext(); }
    Reservation sample() {
        Reservation r = new Reservation();
        r.setOrderNo("RS" + UUID.randomUUID().toString().replace("-", ""));
        r.setStoreId(1L); r.setRoomId(1L); r.setMemberId(1L); r.setRoomName("测试茶室");
        r.setReserveDate(BookingRules.today().plusDays(1)); r.setStartTime("12:00");
        r.setHours(BigDecimal.ONE); r.setGuests(2); r.setContactName("测试顾客"); r.setContactPhone("13800001111");
        return r;
    }
    void queue(Reservation r) { new TransactionTemplate(transactions).executeWithoutResult(tx -> notices.enqueue(r)); due(); }
    int count(String status) { return jdbc.queryForObject("SELECT COUNT(*) FROM t_reservation_notice WHERE status=?", Integer.class, status); }
    void due() { jdbc.update("UPDATE t_reservation_notice SET next_attempt=?", Timestamp.from(Instant.now().minusSeconds(1))); }
    void accept() throws Exception { doReturn(new WxPusherClient.Delivery(true,"123",null)).when(client).send(anyString(), anyString()); }

    @Test void customerBookingQueuesAndOuterRollbackRemovesBoth() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new LoginCustomer(1L), null, List.of()));
        final String[] order = new String[1];
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            order[0] = reservations.create(sample());
            assertThat(count("NEW")).isEqualTo(1);
            verifyNoInteractions(client);
            tx.setRollbackOnly();
        });
        assertThat(count("NEW")).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_reservation WHERE order_no=?", Integer.class, order[0])).isZero();
        String committed = reservations.create(sample());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_reservation_notice WHERE event_key=?", Integer.class, committed)).isEqualTo(1);
        assertThatThrownBy(() -> reservations.create(sample())).hasMessageContaining("已有预约");
        assertThat(count("NEW")).isEqualTo(1);
        jdbc.update("DELETE FROM t_reservation WHERE order_no=?", committed);
    }

    @Test void partialFailureRetriesOnlyFailedStaffAndNeverOtherStore() throws Exception {
        queue(sample());
        when(client.send(eq("UID_one"), anyString())).thenReturn(new WxPusherClient.Delivery(true,"1",null));
        when(client.send(eq("UID_two"), anyString())).thenReturn(new WxPusherClient.Delivery(false,null,"API_1001"), new WxPusherClient.Delivery(true,"2",null));
        notices.dispatch();
        assertThat(count("ACCEPTED")).isEqualTo(1); assertThat(count("PENDING")).isEqualTo(1);
        due(); notices.dispatch(); notices.dispatch();
        assertThat(count("ACCEPTED")).isEqualTo(2);
        verify(client, times(1)).send(eq("UID_one"), contains("测试茶室"));
        verify(client, times(2)).send(eq("UID_two"), anyString());
        verify(client, never()).send(eq("UID_other"), anyString());
    }

    @Test void memberNotificationIsTriggeredOnlyByFirstStaffConfirmation() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new LoginCustomer(1L),null,List.of()));
        String order=reservations.create(sample());
        verify(bookingNotices,never()).confirmed(any());
        long id=jdbc.queryForObject("SELECT id FROM t_reservation WHERE order_no=?",Long.class,order);
        var staff=new com.bama.store.security.LoginStaff(1L,"BM","员工","13800000000",1L,Set.of("reservation:manage"),Set.of(1L));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(staff,null,List.of()));
        reservations.confirm(id);
        assertThatThrownBy(()->reservations.confirm(id)).hasMessageContaining("待确认");
        verify(bookingNotices,times(1)).confirmed(any());
        jdbc.update("DELETE FROM t_reservation WHERE id=?",id);
    }

    @Test void disabledAndMissingConfigRetainQueueUntilConfigured() throws Exception {
        queue(sample()); config.setEnabled(false); notices.dispatch();
        config.setEnabled(true); config.setAppToken(""); notices.dispatch();
        config.setAppToken("AT_test"); jdbc.update("DELETE FROM t_staff_wxpusher WHERE store_id=1"); notices.dispatch();
        assertThat(count("NEW")).isEqualTo(1); verifyNoInteractions(client);
        jdbc.update("INSERT INTO t_staff_wxpusher(store_id,staff_id,uid) VALUES(1,1,'UID_one')"); accept(); due(); notices.dispatch();
        assertThat(count("ACCEPTED")).isEqualTo(1);
    }

    @Test void expiredLeaseRecoversButRemovedEmployeeIsNotContacted() throws Exception {
        queue(sample()); when(client.send(anyString(),anyString())).thenThrow(new java.io.IOException("secret must not be logged"));
        notices.dispatch(); assertThat(count("PENDING")).isEqualTo(2);
        jdbc.update("UPDATE t_reservation_notice SET status='SENDING',lease_token='old' WHERE status='PENDING'");
        jdbc.update("UPDATE t_staff SET status=0 WHERE id=2"); accept(); due(); notices.dispatch();
        assertThat(count("ACCEPTED")).isEqualTo(1); assertThat(count("REMOVED")).isEqualTo(1);
        verify(client,times(1)).send(eq("UID_two"),anyString());
    }

    @Test void simultaneousWorkersClaimEachRecipientOnce() throws Exception {
        queue(sample()); accept();
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var calls=List.<java.util.concurrent.Callable<Void>>of(() -> { notices.dispatch(); return null; }, () -> { notices.dispatch(); return null; });
            for(var result:executor.invokeAll(calls)) result.get();
        } finally { executor.shutdownNow(); }
        // DATETIME precision can put a newly expanded task on the next polling boundary.
        due(); notices.dispatch();
        assertThat(count("ACCEPTED")).isEqualTo(2);
        verify(client,times(1)).send(eq("UID_one"),anyString());
        verify(client,times(1)).send(eq("UID_two"),anyString());
    }

    @Test void apiResponseMustAcceptTheSpecificRecipient() throws Exception {
        var parser = new WxPusherClient(config, new ObjectMapper());
        assertThat(parser.parse(200,"{\"code\":1000,\"data\":[{\"uid\":\"UID_one\",\"code\":1000,\"sendRecordId\":123}]}","UID_one").accepted()).isTrue();
        assertThat(parser.parse(200,"{\"code\":1000,\"data\":[{\"uid\":\"UID_one\",\"code\":1001}]}","UID_one").accepted()).isFalse();
        assertThat(parser.parse(200,"{\"code\":1000,\"data\":[{\"uid\":\"UID_other\",\"code\":1000,\"sendRecordId\":123}]}","UID_one").accepted()).isFalse();
        assertThat(parser.parse(500,"{}","UID_one").accepted()).isFalse();
        assertThat(parser.parse(200,"{\"code\":1001}","UID_one").accepted()).isFalse();
    }
}
