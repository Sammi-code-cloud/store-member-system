package com.bama.store;
import com.bama.store.config.*;
import com.bama.store.service.*;
import com.bama.store.entity.Reservation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingNoticesTest {
    JdbcTemplate jdbc; TransactionTemplate tx; BookingNotices notices; WechatClient client;
    BookingNoticeProperties config; WechatProperties wechat;
    @BeforeEach void setup() throws Exception {
        var ds=new DriverManagerDataSource("jdbc:h2:mem:booking_notice_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
        new ResourceDatabasePopulator(new ClassPathResource("db/schema-h2.sql")).execute(ds);
        jdbc=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        new AdminSchemaUpgrade(ds,jdbc).run(null);new WechatSchema(jdbc).run(null);new BookingNoticeSchema(jdbc).run(null);
        config=new BookingNoticeProperties();config.setEnabled(true);config.setTemplateId("booking-template");
        wechat=new WechatProperties();wechat.setMiniAppId("test-app");wechat.setMiniSecret("test-secret");
        var dictionary=mock(BusinessDictionary.class);when(dictionary.enabled()).thenReturn(true);
        client=mock(WechatClient.class);notices=new BookingNotices(jdbc,config,wechat,client,new ObjectMapper(),dictionary);
        jdbc.update("INSERT INTO t_member(id,member_no,phone,status) VALUES(1,'TEST','13800009999',1)");
        jdbc.update("INSERT INTO t_store(id,name,address,phone,status) VALUES(1,'测试门店','测试地址','0592-8888888',1)");
        jdbc.update("INSERT INTO t_wechat_account(identity_key,audience,account_id) VALUES(?,'CUSTOMER',1)","APP:"+WechatFlows.hash("test-app:openid-one"));
        when(client.exchange("MINI","valid-code")).thenReturn(new WechatClient.Identity("test-app","openid-one",""));
        notices.register(1L,"valid-code");clearInvocations(client);
    }
    Reservation reservation() {
        var r=new Reservation();r.setOrderNo("RS-test");r.setMemberId(1L);r.setStoreId(1L);r.setRoomName("观山茶室");
        r.setReserveDate(LocalDate.of(2026,9,11));r.setStartTime("15:00");r.setContactPhone("13800009999");return r;
    }
    void queue() { tx.executeWithoutResult(s->notices.confirmed(reservation())); }
    String status() { return jdbc.queryForObject("SELECT status FROM t_wx_booking_notice",String.class); }
    @Test void confirmedEventIsDurableAndUsesApprovedTemplateFieldsOnlyOnce() throws Exception {
        queue();queue();verifyNoInteractions(client);
        assertThat(status()).isEqualTo("PENDING");notices.dispatch();notices.dispatch();
        assertThat(status()).isEqualTo("SENT");
        var payload=org.mockito.ArgumentCaptor.forClass(Map.class);verify(client,times(1)).sendSubscription(payload.capture());
        var tree=new ObjectMapper().valueToTree(payload.getValue());
        assertThat(tree.path("template_id").asText()).isEqualTo("booking-template");
        assertThat(tree.at("/data/phrase7/value").asText()).isEqualTo("预约成功");
        assertThat(tree.at("/data/time3/value").asText()).isEqualTo("2026-09-11 15:00");
        assertThat(tree.at("/data/thing1/value").asText()).isEqualTo("观山茶室");
        assertThat(tree.at("/data/phone_number5/value").asText()).isEqualTo("0592-8888888");
        assertThat(tree.path("data").size()).isEqualTo(5);
    }
    @Test void rolledBackConfirmationDoesNotNotify() {
        tx.executeWithoutResult(s->{notices.confirmed(reservation());s.setRollbackOnly();});
        notices.dispatch();verifyNoInteractions(client);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_wx_booking_notice",Integer.class)).isZero();
    }
    @Test void noRegistrationAndChangedIdentityCannotReceive() {
        jdbc.update("DELETE FROM t_wx_booking_receiver");queue();notices.dispatch();assertThat(status()).isEqualTo("SKIPPED");verifyNoInteractions(client);
        jdbc.update("DELETE FROM t_wx_booking_notice");notices.register(1L,"valid-code");clearInvocations(client);queue();
        jdbc.update("DELETE FROM t_wechat_account");notices.dispatch();assertThat(status()).isEqualTo("SKIPPED");verifyNoInteractions(client);
    }
    @Test void registrationCannotSpoofMemberOrApp() {
        when(client.exchange("MINI","wrong")).thenReturn(new WechatClient.Identity("test-app","someone-else",""));
        assertThatThrownBy(()->notices.register(1L,"wrong")).isInstanceOf(com.bama.store.common.BusinessException.class);
        when(client.exchange("MINI","other-app")).thenReturn(new WechatClient.Identity("another-app","openid-one",""));
        assertThatThrownBy(()->notices.register(1L,"other-app")).isInstanceOf(com.bama.store.common.BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT open_id FROM t_wx_booking_receiver",String.class)).isEqualTo("openid-one");
    }
    @Test void rejectedAndAmbiguousSendsAreNotAutomaticallyRepeated() throws Exception {
        queue();when(client.sendSubscription(any())).thenReturn(43101);notices.dispatch();notices.dispatch();
        assertThat(status()).isEqualTo("REJECTED");verify(client,times(1)).sendSubscription(any());
        jdbc.update("DELETE FROM t_wx_booking_notice");reset(client);queue();
        when(client.sendSubscription(any())).thenThrow(new java.io.IOException("ambiguous"));notices.dispatch();notices.dispatch();
        assertThat(status()).isEqualTo("UNKNOWN");verify(client,times(1)).sendSubscription(any());
    }
    @Test void disabledTemplatesDoNotAdvertiseOrEnqueue() {
        config.setTemplateId("");assertThat(notices.settings().get("enabled")).isEqualTo(false);
        queue();notices.dispatch();verifyNoInteractions(client);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_wx_booking_notice",Integer.class)).isZero();
    }
}
