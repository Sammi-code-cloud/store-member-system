package com.bama.store;

import com.bama.store.config.*;
import com.bama.store.service.*;
import com.bama.store.entity.Member;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.UUID;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletSmsNoticesTest {
    JdbcTemplate jdbc;TransactionTemplate tx;WalletSmsProperties config;WalletSmsSender sender;WalletSmsNotices notices;Member member;
    @BeforeEach void setup() throws Exception {
        var ds=new DriverManagerDataSource("jdbc:h2:mem:sms_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
        jdbc=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        jdbc.execute("CREATE TABLE t_member(id BIGINT PRIMARY KEY,phone VARCHAR(32),status INT,deleted INT)");
        jdbc.update("INSERT INTO t_member VALUES(1,'13800000001',1,0)");new WalletSmsSchema(jdbc).run(null);
        config=new WalletSmsProperties();config.setEnabled(true);config.setAccessKeyId("test");config.setAccessKeySecret("test");config.setSignName("测试签名");config.setChargeTemplate("SMS_512120638");config.setRechargeTemplate("SMS_512405620");
        sender=mock(WalletSmsSender.class);when(sender.send(anyString(),anyString(),anyString(),anyString(),anyString())).thenReturn(new WalletSmsSender.Result("OK","provider-test"));
        notices=new WalletSmsNotices(jdbc,config,sender,new ObjectMapper());
        member=new Member();member.setId(1L);member.setName("张三");member.setPhone("13800000001");
    }
    void enqueue(String kind) {notices.enqueue(kind,"order-1",member,1L,new BigDecimal("100"),new BigDecimal("220"));}
    String status(){return jdbc.queryForObject("SELECT status FROM t_sms_wallet_notice",String.class);}
    @Test void exactRechargePayloadCommitsOnceAndSendsOnce() throws Exception {
        tx.executeWithoutResult(s->{enqueue("RECHARGE");enqueue("RECHARGE");verifyNoInteractions(sender);});
        var row=jdbc.queryForMap("SELECT * FROM t_sms_wallet_notice");
        assertThat(row.get("template_code")).isEqualTo("SMS_512405620");
        var json=new ObjectMapper().readTree((String)row.get("payload"));assertThat(json.size()).isEqualTo(3);
        assertThat(json.path("name").asText()).isEqualTo("张三");assertThat(json.path("amount").asText()).isEqualTo("100.00");assertThat(json.path("balance").asText()).isEqualTo("220.00");
        notices.dispatch();notices.dispatch();assertThat(status()).isEqualTo("ACCEPTED");verify(sender,times(1)).send(eq(member.getPhone()),eq("测试签名"),eq("SMS_512405620"),anyString(),eq("RECHARGE:order-1"));
    }
    @Test void chargeUsesItsOwnTemplateAndPositiveDeduction() throws Exception {
        tx.executeWithoutResult(s->enqueue("CHARGE"));notices.dispatch();
        verify(sender).send(anyString(),anyString(),eq("SMS_512120638"),contains("100.00"),eq("CHARGE:order-1"));
    }
    @Test void rolledBackTransactionNeverSends() {
        tx.executeWithoutResult(s->{enqueue("CHARGE");s.setRollbackOnly();});notices.dispatch();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_sms_wallet_notice",Integer.class)).isZero();verifyNoInteractions(sender);
    }
    @Test void disabledAndMissingConfigDoNotQueue() {
        config.setEnabled(false);tx.executeWithoutResult(s->enqueue("CHARGE"));config.setEnabled(true);config.setAccessKeySecret("");tx.executeWithoutResult(s->enqueue("CHARGE"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_sms_wallet_notice",Integer.class)).isZero();
    }
    @Test void invalidOrChangedPhoneDoesNotReceiveFinancialData() {
        member.setPhone("");tx.executeWithoutResult(s->enqueue("CHARGE"));notices.dispatch();assertThat(status()).isEqualTo("SKIPPED");verifyNoInteractions(sender);
        jdbc.update("DELETE FROM t_sms_wallet_notice");member.setPhone("13800000001");tx.executeWithoutResult(s->enqueue("CHARGE"));jdbc.update("UPDATE t_member SET phone='13800000002'");notices.dispatch();assertThat(status()).isEqualTo("SKIPPED");verifyNoInteractions(sender);
    }
    @Test void providerRejectAndTimeoutAreRecordedWithoutAutomaticRetry() throws Exception {
        when(sender.send(anyString(),anyString(),anyString(),anyString(),anyString())).thenReturn(new WalletSmsSender.Result("isv.BUSINESS_LIMIT_CONTROL",null));
        tx.executeWithoutResult(s->enqueue("CHARGE"));notices.dispatch();notices.dispatch();assertThat(status()).isEqualTo("REJECTED");verify(sender,times(1)).send(anyString(),anyString(),anyString(),anyString(),anyString());
        jdbc.update("DELETE FROM t_sms_wallet_notice");reset(sender);when(sender.send(anyString(),anyString(),anyString(),anyString(),anyString())).thenThrow(new java.io.IOException("timeout"));
        tx.executeWithoutResult(s->enqueue("CHARGE"));notices.dispatch();notices.dispatch();assertThat(status()).isEqualTo("UNKNOWN");verify(sender,times(1)).send(anyString(),anyString(),anyString(),anyString(),anyString());
    }
    @Test void crashedSendIsMarkedUnknownAndNotRepeated() {
        tx.executeWithoutResult(s->enqueue("CHARGE"));jdbc.update("UPDATE t_sms_wallet_notice SET status='SENDING',update_time=?",LocalDateTime.now().minusMinutes(6));
        notices.dispatch();assertThat(status()).isEqualTo("UNKNOWN");verifyNoInteractions(sender);
    }
}
