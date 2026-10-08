package com.bama.store;

import com.bama.store.service.*;
import com.bama.store.dto.*;
import com.bama.store.vo.ChargeResultVo;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:charge_once;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=20000","logging.level.root=ERROR","logging.level.com.bama.store=ERROR","mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"})
@ActiveProfiles("h2")
@org.springframework.test.context.jdbc.Sql(statements = "UPDATE t_business_dictionary SET dict_value='1' WHERE dict_key='business_enabled'")
class ChargeIdempotencyTest {
 @Autowired com.bama.store.config.WalletSmsProperties smsConfig;
 @Test void moneyOperationsDoNotQueueSmsEvenWithLegacySmsConfiguration() throws Exception {
  smsConfig.setEnabled(true);smsConfig.setAccessKeyId("test");smsConfig.setAccessKeySecret("test");smsConfig.setSignName("test");smsConfig.setChargeTemplate("charge");smsConfig.setRechargeTemplate("recharge");
  try {
   jdbc.update("DELETE FROM t_sms_wallet_notice");charge(manual("sms-once"));charge(manual("sms-once"));
   var recharge=new RechargeRequest();recharge.setMemberId(1L);recharge.setAmount(new BigDecimal("50"));recharge.setGiftAmount(new BigDecimal("5"));service.recharge(recharge,1L,1L);
   assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_sms_wallet_notice",Integer.class)).isZero();
   assertThat(jdbc.queryForObject("SELECT balance FROM t_member_account WHERE member_id=1",BigDecimal.class)).isEqualByComparingTo("145.00");
  } finally {smsConfig.setEnabled(false);jdbc.update("DELETE FROM t_sms_wallet_notice");}
 }
 @Autowired com.bama.store.config.WalletNoticeProperties noticeConfig;
 @Test void successfulMoneyChangesEnqueueOneNoticePerOperation() {
  noticeConfig.setEnabled(true);
  noticeConfig.getRecharge().setId("test-recharge");noticeConfig.getCharge().setId("test-charge");
  noticeConfig.getRecharge().setFields(Map.of("amount","amount1"));noticeConfig.getCharge().setFields(Map.of("amount","amount1"));
  jdbc.update("DELETE FROM t_wx_wallet_notice");
  try {
   charge(manual("notice-once"));charge(manual("notice-once"));
   assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_wx_wallet_notice",Integer.class)).isEqualTo(1);
   var recharge=new RechargeRequest();recharge.setMemberId(1L);recharge.setAmount(new BigDecimal("50"));recharge.setGiftAmount(new BigDecimal("5"));
   service.recharge(recharge,1L,1L);
   assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_wx_wallet_notice",Integer.class)).isEqualTo(2);
   assertThat(balance()).isEqualByComparingTo("145");
  } finally {noticeConfig.setEnabled(false);jdbc.update("DELETE FROM t_wx_wallet_notice");}
 }
 @Autowired AccountService service; @Autowired JdbcTemplate jdbc; @SpyBean AuditService audit;
 @BeforeEach void setup(){reset(audit);jdbc.update("DELETE FROM t_charge_request");jdbc.update("DELETE FROM t_consume_item");jdbc.update("DELETE FROM t_consume_order");jdbc.update("DELETE FROM t_wallet_txn");jdbc.update("UPDATE t_member SET status=1,discount=80 WHERE id=1");jdbc.update("UPDATE t_member_account SET balance=100,total_consume=0,version=0 WHERE member_id=1");}
 ChargeConfirmRequest manual(String no){var r=new ChargeConfirmRequest();r.setMemberId(1L);r.setAmount(new BigDecimal("10.00"));r.setBizNo(no);return r;}
 ChargeConfirmRequest detail(String no,String name){var r=new ChargeConfirmRequest();r.setMemberId(1L);r.setBizNo(no);var i=new ChargeItemDto();i.setItemType("PRODUCT");i.setItemName(name);i.setPrice(new BigDecimal("10"));i.setQuantity(1);r.setItems(List.of(i));return r;}
 ChargeResultVo charge(ChargeConfirmRequest r){return service.charge(r,1L,1L,"店长");}
 BigDecimal balance(){return jdbc.queryForObject("SELECT balance FROM t_member_account WHERE member_id=1",BigDecimal.class);}
 @Test void simultaneousRetriesDebitExactlyOnce() throws Exception {
  var pool=Executors.newFixedThreadPool(8);var ready=new CountDownLatch(8);var start=new CountDownLatch(1);
  try{var futures=new ArrayList<Future<ChargeResultVo>>();for(int i=0;i<8;i++)futures.add(pool.submit(()->{ready.countDown();start.await();return charge(manual("same_parallel"));}));assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();start.countDown();
   var numbers=new HashSet<String>();for(var f:futures)numbers.add(f.get(30,TimeUnit.SECONDS).getOrderNo());assertThat(numbers).hasSize(1);
   assertThat(balance()).isEqualByComparingTo("90");assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_consume_order",Integer.class)).isEqualTo(1);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_wallet_txn WHERE type='CONSUME'",Integer.class)).isEqualTo(1);
  }finally{start.countDown();pool.shutdownNow();}
 }
 @Test void detailRetryReturnsOriginalAmountAfterDiscountChanges(){var first=charge(detail("detail_retry","茶叶"));jdbc.update("UPDATE t_member SET discount=90 WHERE id=1");var second=charge(detail("detail_retry","茶叶"));assertThat(second).isEqualTo(first);assertThat(balance()).isEqualByComparingTo("92");}
 @Test void sameKeyChangedItemsOrAmountIsRejected(){charge(detail("changed","茶叶"));assertThatThrownBy(()->charge(detail("changed","茶室"))).isInstanceOf(com.bama.store.common.BusinessException.class);assertThatThrownBy(()->charge(manual("changed"))).isInstanceOf(com.bama.store.common.BusinessException.class);assertThat(balance()).isEqualByComparingTo("92");}
 @Test void missingBusinessNumberCannotDebit(){assertThatThrownBy(()->charge(detail(null,"茶叶"))).isInstanceOf(com.bama.store.common.BusinessException.class);assertThat(balance()).isEqualByComparingTo("100");}
 @Test void failedTransactionRollsBackClaimAndMoneyThenRetrySucceeds(){doThrow(new IllegalStateException("模拟落库失败")).when(audit).record(eq("消费扣款"),any(),anyString());assertThatThrownBy(()->charge(manual("rollback"))).isInstanceOf(IllegalStateException.class);assertThat(balance()).isEqualByComparingTo("100");assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_charge_request",Integer.class)).isZero();assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_consume_order",Integer.class)).isZero();reset(audit);charge(manual("rollback"));assertThat(balance()).isEqualByComparingTo("90");}
 @Test void legacyReceiptsIncludingArchivedFlowCannotDebitAgain(){var first=charge(manual("legacy"));jdbc.update("DELETE FROM t_charge_request WHERE biz_no='legacy'");jdbc.update("UPDATE t_wallet_txn SET deleted=1 WHERE biz_no='legacy'");var second=charge(manual("legacy"));assertThat(second.getOrderNo()).isEqualTo(first.getOrderNo());assertThat(balance()).isEqualByComparingTo("90");assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_charge_request WHERE result_json IS NOT NULL",Integer.class)).isEqualTo(1);}
 @Test void independentBusinessNumbersStillAllowLegitimateRepeatPurchases(){charge(manual("first"));charge(manual("second"));assertThat(balance()).isEqualByComparingTo("80");}
}
