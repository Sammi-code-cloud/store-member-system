package com.bama.store;
import com.bama.store.config.*;
import com.bama.store.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletNoticesTest {
    JdbcTemplate jdbc; TransactionTemplate tx; WalletNotices notices; WechatClient client;
    WalletNoticeProperties config; WechatProperties wechat;
    @BeforeEach void setup() throws Exception {
        var ds=new DriverManagerDataSource("jdbc:h2:mem:notice_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
        new ResourceDatabasePopulator(new ClassPathResource("db/schema-h2.sql")).execute(ds);
        jdbc=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        new AdminSchemaUpgrade(ds,jdbc).run(null);new WechatSchema(jdbc).run(null);new WalletNoticeSchema(jdbc).run(null);
        config=new WalletNoticeProperties();config.setEnabled(true);
        config.getRecharge().setId("test-recharge");config.getCharge().setId("test-charge");
        for(var template:List.of(config.getRecharge(),config.getCharge()))template.setFields(Map.of("amount","amount1","balance","amount2","time","time3","gift","amount4"));
        wechat=new WechatProperties();wechat.setMiniAppId("test-app");wechat.setMiniSecret("test-secret");
        var dictionary=mock(BusinessDictionary.class);when(dictionary.enabled()).thenReturn(true);
        client=mock(WechatClient.class);notices=new WalletNotices(jdbc,config,wechat,client,new ObjectMapper(),dictionary);
        jdbc.update("INSERT INTO t_member(id,member_no,phone,status) VALUES(1,'TEST','13800009999',1)");
        jdbc.update("INSERT INTO t_store(id,name,status) VALUES(1,'Test store',1)");
        jdbc.update("INSERT INTO t_wechat_account(identity_key,audience,account_id) VALUES(?,'CUSTOMER',1)","APP:"+WechatFlows.hash("test-app:openid-one"));
        when(client.exchange("MINI","valid-code")).thenReturn(new WechatClient.Identity("test-app","openid-one",""));
        notices.register(1L,"valid-code");clearInvocations(client);
    }
    void queue(String kind,String id) { notices.enqueue(kind,id,1L,1L,new BigDecimal("100"),new BigDecimal("20"),new BigDecimal("220")); }
    String status() { return jdbc.queryForObject("SELECT status FROM t_wx_wallet_notice",String.class); }
    @Test void commitsOneMessageIncludingRechargeGiftAndSkipsDuplicateEvent() throws Exception {
        tx.executeWithoutResult(s->{queue("RECHARGE","R1");queue("RECHARGE","R1");verifyNoInteractions(client);});
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_wx_wallet_notice",Integer.class)).isEqualTo(1);
        assertThat(status()).isEqualTo("PENDING");
        notices.dispatch();notices.dispatch();
        assertThat(status()).isEqualTo("SENT");
        var payload=org.mockito.ArgumentCaptor.forClass(Map.class);verify(client,times(1)).sendSubscription(payload.capture());
        assertThat(payload.getValue().get("template_id")).isEqualTo("test-recharge");
        assertThat(payload.getValue().get("data").toString()).contains("120.00","20.00","220.00");
    }
    @Test void chargeOnlyModeDoesNotConsumeSubscriptionOnRecharge() throws Exception {
        config.setRechargeEnabled(false);
        tx.executeWithoutResult(s->{queue("RECHARGE","R-off");queue("CHARGE","C-on");});
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_wx_wallet_notice",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT event_key FROM t_wx_wallet_notice",String.class)).isEqualTo("CHARGE:C-on");
        notices.dispatch();verify(client,times(1)).sendSubscription(any());
    }
    @Test void rollbackLeavesNoNotificationToSend() {
        tx.executeWithoutResult(s->{queue("CHARGE","C1");s.setRollbackOnly();});
        notices.dispatch();verifyNoInteractions(client);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_wx_wallet_notice",Integer.class)).isZero();
    }
    @Test void sharedBalanceTemplateUsesExactKeywordsAndSignedChange() throws Exception {
        var fields=Map.of("type","thing8","amount","amount1","balance","amount2","time","date4","reason","thing6");
        config.getCharge().setId(config.getRecharge().getId());
        config.getCharge().setFields(fields);config.getRecharge().setFields(fields);
        assertThat(config.ready()).isTrue();
        assertThat((List<?>)notices.settings().get("templateIds")).hasSize(1);
        tx.executeWithoutResult(s->queue("CHARGE","C-date"));notices.dispatch();
        var captor=org.mockito.ArgumentCaptor.forClass(Map.class);verify(client).sendSubscription(captor.capture());
        var data=new ObjectMapper().valueToTree(captor.getValue().get("data"));
        assertThat(data.size()).isEqualTo(5);
        assertThat(data.path("amount1").path("value").asText()).isEqualTo("-100.00");
        assertThat(data.path("amount2").path("value").asText()).isEqualTo("220.00");
        assertThat(data.path("thing8").path("value").asText()).isEqualTo("扣款");
        assertThat(data.path("thing6").path("value").asText()).isEqualTo("会员余额消费");
        assertThat(data.path("date4").path("value").asText()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }
    @Test void rejectAndUnknownAreNotAutomaticallyResent() throws Exception {
        tx.executeWithoutResult(s->queue("CHARGE","C1"));
        when(client.sendSubscription(any())).thenThrow(new java.io.IOException("uncertain"));
        notices.dispatch();notices.dispatch();assertThat(status()).isEqualTo("UNKNOWN");verify(client,times(1)).sendSubscription(any());
        jdbc.update("DELETE FROM t_wx_wallet_notice");reset(client);
        tx.executeWithoutResult(s->queue("CHARGE","C2"));when(client.sendSubscription(any())).thenReturn(43101);
        notices.dispatch();notices.dispatch();assertThat(status()).isEqualTo("REJECTED");verify(client,times(1)).sendSubscription(any());
    }

    @Test void suppliedBalanceTemplateSupportsRechargeAndChargeWithExactFields() throws Exception {
        String id="SEZA6ocdUTvr-Oep2-3PlWaZJtOQSp8TbrZRH483jIU";
        var fields=Map.of("type","thing8","amount","amount1","balance","amount2","time","date4","reason","thing6");
        config.setRechargeEnabled(true);
        for(var template:List.of(config.getRecharge(),config.getCharge())) {template.setId(id);template.setFields(fields);}
        assertThat(notices.settings().get("templateIds")).isEqualTo(List.of(id));
        tx.executeWithoutResult(s->{queue("RECHARGE","R-both");queue("CHARGE","C-both");});
        notices.dispatch();
        var captor=org.mockito.ArgumentCaptor.forClass(Map.class);verify(client,times(2)).sendSubscription(captor.capture());
        List<com.fasterxml.jackson.databind.JsonNode> payloads=new ArrayList<>();
        for(Object value:captor.getAllValues())payloads.add(new ObjectMapper().valueToTree(value));
        for(var payload:payloads) {
            assertThat(payload.path("template_id").asText()).isEqualTo(id);
            assertThat(payload.path("data").size()).isEqualTo(5);
            assertThat(payload.at("/data/amount2/value").asText()).isEqualTo("220.00");
            assertThat(payload.at("/data/date4/value").asText()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        }
        var recharge=payloads.stream().filter(v->v.at("/data/thing8/value").asText().equals("充值")).findFirst().orElseThrow();
        assertThat(recharge.at("/data/amount1/value").asText()).isEqualTo("120.00");
        assertThat(recharge.at("/data/thing6/value").asText()).isEqualTo("会员充值（含赠送）");
        var charge=payloads.stream().filter(v->v.at("/data/thing8/value").asText().equals("扣款")).findFirst().orElseThrow();
        assertThat(charge.at("/data/amount1/value").asText()).isEqualTo("-100.00");
        assertThat(charge.at("/data/thing6/value").asText()).isEqualTo("会员余额消费");
    }
    @Test void changedBindingAndDifferentAppNeverReceiveFinancialDetails() {
        tx.executeWithoutResult(s->queue("CHARGE","C1"));
        jdbc.update("DELETE FROM t_wechat_account");notices.dispatch();assertThat(status()).isEqualTo("SKIPPED");verifyNoInteractions(client);
        jdbc.update("DELETE FROM t_wx_wallet_notice");wechat.setMiniAppId("another-app");
        tx.executeWithoutResult(s->queue("RECHARGE","R2"));notices.dispatch();assertThat(status()).isEqualTo("SKIPPED");verifyNoInteractions(client);
    }
    @Test void recipientMustMatchAuthenticatedMember() {
        when(client.exchange("MINI","wrong")).thenReturn(new WechatClient.Identity("test-app","someone-else",""));
        assertThatThrownBy(()->notices.register(1L,"wrong")).isInstanceOf(com.bama.store.common.BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT open_id FROM t_wx_wallet_receiver",String.class)).isEqualTo("openid-one");
    }
    @Test void missingTemplatesDisableBothSubscriptionAndEnqueue() {
        config.getCharge().setId("");assertThat(notices.settings().get("enabled")).isEqualTo(false);
        tx.executeWithoutResult(s->queue("RECHARGE","R1"));notices.dispatch();verifyNoInteractions(client);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_wx_wallet_notice",Integer.class)).isZero();
    }
}
