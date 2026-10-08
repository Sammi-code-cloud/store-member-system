package com.bama.store;

import com.bama.store.config.BusinessDictionarySchema;
import com.bama.store.dto.ChargeConfirmRequest;
import com.bama.store.dto.RechargeRequest;
import com.bama.store.entity.Reservation;
import com.bama.store.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:business_dictionary;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=10000", "logging.level.root=ERROR", "logging.level.com.bama.store=ERROR", "mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"})
@ActiveProfiles("h2")
@AutoConfigureMockMvc
class BusinessDictionaryTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired BusinessDictionary dictionary;
    @Autowired BusinessDictionarySchema schema;
    @Autowired AccountService accounts;
    @Autowired ReservationService reservations;
    @Autowired CustomerAuthService customers;
    @Autowired CustomerManualWechatLogin manualLogin;
    @Autowired WechatAccounts wechatAccounts;
    @Autowired StaffWechatMembership staffMembership;
    @Autowired StaffWechatInvitations invitations;
    @Autowired MemberEnrollment enrollment;
    @Autowired CustomerWechatBinding smsBinding;
    @Autowired BookingNotices bookingNotices;
    @Autowired WalletNotices walletNotices;
    @Autowired com.bama.store.config.StaffMembershipBackfill backfill;
    @Autowired PlatformTransactionManager transactions;
    String admin;

    @BeforeEach void setup() throws Exception {
        jdbc.update("UPDATE t_business_dictionary SET dict_value='0',deleted=0 WHERE dict_key='business_enabled'");
        admin = login("13800000000", "admin123");
    }

    JsonNode call(String method, String path, String token, Object body) throws Exception {
        var request = MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method), path);
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        return json.readTree(mvc.perform(request).andReturn().getResponse().getContentAsByteArray());
    }
    String login(String phone, String password) throws Exception {
        var result=call("POST", "/api/auth/login", null, Map.of("phone",phone,"password",password));
        assertThat(result.path("code").asInt()).isEqualTo(200);
        return result.path("data").path("token").asText();
    }
    @Test void onlyHeadOfficeCanChangeTheGlobalSwitchAndInvalidInputCannotEnableIt() throws Exception {
        String cashier=login("13800000001","123456");
        assertThat(call("PUT","/api/business-dictionary",cashier,Map.of("enabled",true)).path("code").asInt()).isEqualTo(403);
        assertThat(call("GET","/api/business-dictionary",cashier,null).path("code").asInt()).isEqualTo(403);
        assertThat(call("PUT","/api/business-dictionary",null,Map.of("enabled",true)).path("code").asInt()).isEqualTo(401);
        assertThat(call("PUT","/api/business-dictionary",admin,Map.of()).path("code").asInt()).isNotEqualTo(200);
        assertThat(dictionary.enabled()).isFalse();
        assertThat(call("PUT","/api/business-dictionary",admin,Map.of("enabled",true)).path("data").path("enabled").asBoolean()).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_audit_log WHERE action='修改业务总开关' AND staff_id=1",Integer.class)).isPositive();
        assertThat(call("PUT","/api/business-dictionary",admin,Map.of("enabled",false)).path("data").path("enabled").asBoolean()).isFalse();
    }

    @Test void closedBlocksServiceWritesWithoutChangingMoneyOrReservations() {
        var balance=jdbc.queryForObject("SELECT balance FROM t_member_account WHERE member_id=1",BigDecimal.class);
        var count=jdbc.queryForObject("SELECT COUNT(*) FROM t_reservation",Integer.class);
        var txns=jdbc.queryForObject("SELECT COUNT(*) FROM t_wallet_txn",Integer.class);
        assertThatThrownBy(()->accounts.recharge(new RechargeRequest(),1L,1L)).hasMessageContaining("系统正在维护");
        assertThatThrownBy(()->accounts.charge(new ChargeConfirmRequest(),1L,1L,"测试")).hasMessageContaining("系统正在维护");
        assertThatThrownBy(()->reservations.create(new Reservation())).hasMessageContaining("系统正在维护");
        assertThatThrownBy(()->reservations.confirm(1L)).hasMessageContaining("系统正在维护");
        assertThatThrownBy(()->reservations.reschedule(1L,BookingRules.today(),"14:00")).hasMessageContaining("系统正在维护");
        assertThat(jdbc.queryForObject("SELECT balance FROM t_member_account WHERE member_id=1",BigDecimal.class)).isEqualByComparingTo(balance);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_reservation",Integer.class)).isEqualTo(count);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_wallet_txn",Integer.class)).isEqualTo(txns);
    }

    @Test void directHttpWritesCannotBypassGateButPublicDisplayRemainsAvailable() throws Exception {
        dictionary.update(true);
        var customer=call("POST","/api/customer/auth/register",null,Map.of("username","gate_"+UUID.randomUUID().toString().substring(0,8),"password","customer123","name","测试会员")).path("data");
        dictionary.update(false);
        String customerToken=customer.path("token").asText();
        assertThat(call("PUT","/api/business-dictionary",customerToken,Map.of("enabled",true)).path("code").asInt()).isEqualTo(403);
        assertThat(call("POST","/api/customer/reserve",customerToken,Map.of("roomId",1)).path("message").asText()).contains("系统正在维护");
        assertThat(call("POST","/api/reservations",admin,Map.of("roomId",1,"memberId",1)).path("message").asText()).contains("系统正在维护");
        var recharge=Map.of("memberId",1,"amount",10);
        assertThat(call("POST","/api/account/recharge",admin,recharge).path("message").asText()).contains("系统正在维护");
        assertThat(call("POST","/api/charge/confirm",admin,Map.of("memberId",1,"amount",1,"bizNo","gate-test")).path("message").asText()).contains("系统正在维护");
        assertThat(call("GET","/api/customer/stores",null,null).path("code").asInt()).isEqualTo(200);
        assertThat(call("GET","/api/customer/rooms?storeId=1",null,null).path("code").asInt()).isEqualTo(200);
        assertThat(call("GET","/api/members/1/account",admin,null).path("code").asInt()).isEqualTo(200);
        call("PUT","/api/business-dictionary",admin,Map.of("enabled",true));
        assertThat(call("POST","/api/account/recharge",admin,recharge).path("code").asInt()).isEqualTo(200);
        call("PUT","/api/business-dictionary",admin,Map.of("enabled",false));
        assertThat(call("POST","/api/account/recharge",admin,recharge).path("message").asText()).contains("系统正在维护");
    }

    @Test void globalSwitchRemainsManageableWhenCurrentStoreIsPaused() throws Exception {
        jdbc.update("UPDATE t_store SET status=0 WHERE id=1");
        try {
            assertThat(call("GET","/api/business-dictionary",admin,null).path("code").asInt()).isEqualTo(200);
            assertThat(call("PUT","/api/business-dictionary",admin,Map.of("enabled",true)).path("code").asInt()).isEqualTo(200);
        } finally { jdbc.update("UPDATE t_store SET status=1 WHERE id=1"); }
    }

    @Test void missingArchivedOrInvalidValuesFailClosedAndRestartPreservesChoice() throws Exception {
        jdbc.update("DELETE FROM t_business_dictionary WHERE dict_key='business_enabled'");
        assertThat(dictionary.enabled()).isFalse();
        assertThatThrownBy(()->accounts.recharge(new RechargeRequest(),1L,1L)).hasMessageContaining("系统正在维护");
        schema.run(null);
        assertThat(dictionary.enabled()).isFalse();
        jdbc.update("UPDATE t_business_dictionary SET dict_value='1' WHERE dict_key='business_enabled'");
        schema.run(null);
        assertThat(dictionary.enabled()).isTrue();
        jdbc.update("UPDATE t_business_dictionary SET dict_value='garbage' WHERE dict_key='business_enabled'");
        assertThatThrownBy(()->accounts.recharge(new RechargeRequest(),1L,1L)).hasMessageContaining("系统正在维护");
        jdbc.update("UPDATE t_business_dictionary SET dict_value='1',deleted=1 WHERE dict_key='business_enabled'");
        assertThat(dictionary.enabled()).isFalse();
        assertThatThrownBy(()->accounts.recharge(new RechargeRequest(),1L,1L)).hasMessageContaining("系统正在维护");
    }

    @Test void closingWaitsForInFlightTransactionAndBlocksLaterWrites() throws Exception {
        jdbc.update("UPDATE t_business_dictionary SET dict_value='1' WHERE dict_key='business_enabled'");
        var locked=new CountDownLatch(1);var release=new CountDownLatch(1);var closing=new CountDownLatch(1);
        var pool=Executors.newFixedThreadPool(2);
        try {
            var first=pool.submit(()->new TransactionTemplate(transactions).executeWithoutResult(s->{
                dictionary.requireEnabled();locked.countDown();
                try { if(!release.await(5,TimeUnit.SECONDS)) throw new IllegalStateException("Timed out"); }
                catch(InterruptedException e){throw new RuntimeException(e);}
            }));
            assertThat(locked.await(5,TimeUnit.SECONDS)).isTrue();
            var close=pool.submit(()->{closing.countDown();dictionary.update(false);});
            assertThat(closing.await(5,TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(()->close.get(200,TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown();first.get(5,TimeUnit.SECONDS);close.get(5,TimeUnit.SECONDS);
            assertThatThrownBy(()->accounts.recharge(new RechargeRequest(),1L,1L)).hasMessageContaining("系统正在维护");
        } finally { release.countDown();pool.shutdownNow(); }
    }

    Map<String,Integer> writeCounts() {
        var result=new LinkedHashMap<String,Integer>();
        for(String table:List.of("t_member","t_member_account","t_wechat_account","t_wechat_flow","t_wechat_phone_verified","t_sms_challenge","t_wx_booking_receiver","t_wx_wallet_receiver"))
            result.put(table,jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class));
        return result;
    }

    @Test void closedBlocksAllRegistrationBindingAndSubscriptionEntrypointsWithoutNewRows() throws Exception {
        var before=writeCounts();
        var identity=new WechatClient.Identity("test-app",UUID.randomUUID().toString(),"");
        assertThat(call("POST","/api/customer/auth/register",null,Map.of("username","new_user","password","customer123","name","新顾客")).path("message").asText()).contains("系统正在维护");
        assertThatThrownBy(()->customers.createWechatMember("13900001111")).hasMessageContaining("系统正在维护");
        assertThatThrownBy(()->manualLogin.login(identity,"13900001111","新顾客")).hasMessageContaining("系统正在维护");
        assertThatThrownBy(()->manualLogin.login(identity,null,null)).hasMessageContaining("系统正在维护");
        assertThatThrownBy(()->wechatAccounts.login(identity,"CUSTOMER")).hasMessageContaining("系统正在维护");
        assertThatThrownBy(()->wechatAccounts.login(identity,"STAFF")).hasMessageContaining("系统正在维护");
        assertThatThrownBy(()->wechatAccounts.bind(identity,1L)).hasMessageContaining("系统正在维护");
        assertThatThrownBy(()->wechatAccounts.bindCustomer("old-ticket","13900001111")).hasMessageContaining("系统正在维护");
        assertThatThrownBy(()->invitations.create(1L)).hasMessageContaining("系统正在维护");
        assertThatThrownBy(()->invitations.bind("old-ticket","13900001111","code")).hasMessageContaining("系统正在维护");
        assertThatThrownBy(()->enrollment.code(1L)).hasMessageContaining("系统正在维护");
        assertThatThrownBy(()->enrollment.bind("old-ticket","13900001111","code")).hasMessageContaining("系统正在维护");
        assertThatThrownBy(()->smsBinding.send("old-ticket","13900001111","127.0.0.1")).hasMessageContaining("系统正在维护");
        assertThatThrownBy(()->smsBinding.bind("old-ticket","13900001111","challenge","123456")).hasMessageContaining("系统正在维护");
        assertThatThrownBy(()->staffMembership.synchronize(1L)).hasMessageContaining("系统正在维护");
        assertThatThrownBy(()->bookingNotices.register(1L,"code")).hasMessageContaining("系统正在维护");
        assertThatThrownBy(()->walletNotices.register(1L,"code")).hasMessageContaining("系统正在维护");
        assertThat(call("POST","/api/wechat/bind",null,Map.of("ticket","old-ticket","phone","13800000000","password","admin123")).path("message").asText()).contains("系统正在维护");
        backfill.run(null);
        assertThat(bookingNotices.settings()).containsEntry("enabled",false).containsEntry("templateIds",List.of());
        assertThat(walletNotices.settings()).containsEntry("enabled",false).containsEntry("templateIds",List.of());
        assertThat(writeCounts()).isEqualTo(before);
    }

    @Test void existingLoginsRemainUsableWithoutAutomaticallyAddingWechatLinks() throws Exception {
        dictionary.update(true);
        String username="existing_"+UUID.randomUUID().toString().substring(0,8);
        var registered=customers.register(username,"customer123","已有用户");
        String openId=UUID.randomUUID().toString();
        var identity=new WechatClient.Identity("existing-app",openId,"");
        Long memberId=((Number)customers.register("13900002222","customer123","已有微信用户").get("memberId")).longValue();
        jdbc.update("INSERT INTO t_wechat_account(identity_key,audience,account_id) VALUES(?,'CUSTOMER',?)","APP:"+WechatFlows.hash("existing-app:"+openId),memberId);
        jdbc.update("INSERT INTO t_wechat_phone_verified(member_id,phone,verified_at) VALUES(?,?,CURRENT_TIMESTAMP)",memberId,"13900002222");
        jdbc.update("INSERT INTO t_wechat_account(identity_key,audience,account_id) VALUES(?,'STAFF',1)","APP:"+WechatFlows.hash("staff-app:"+openId));
        dictionary.update(false);
        var before=writeCounts();
        assertThat(customers.login(username,"customer123").get("memberId")).isEqualTo(registered.get("memberId"));
        var withNewUnion=new WechatClient.Identity("existing-app",openId,"unseen-union");
        assertThat(((Map<?,?>)manualLogin.login(withNewUnion,null,null).get("account")).get("memberId")).isEqualTo(memberId);
        assertThat(((Map<?,?>)manualLogin.login(withNewUnion,"13900002222","不应改名").get("account")).get("memberId")).isEqualTo(memberId);
        assertThat(wechatAccounts.login(withNewUnion,"CUSTOMER")).containsEntry("bindRequired",false);
        assertThat(wechatAccounts.login(new WechatClient.Identity("staff-app",openId,"unseen-staff-union"),"STAFF")).containsEntry("bindRequired",false);
        assertThat(login("13800000000","admin123")).isNotBlank();
        assertThat(jdbc.queryForObject("SELECT name FROM t_member WHERE id=?",String.class,memberId)).isEqualTo("已有微信用户");
        assertThat(writeCounts()).isEqualTo(before);
    }

    @Test void administratorCannotCreateMembersWhileClosedAndRegistrationReturnsAfterReopening() throws Exception {
        var result=call("POST","/api/members",admin,Map.of("name","后台维护","phone","13900003333"));
        assertThat(result.path("message").asText()).contains("系统正在维护");
        dictionary.update(true);
        assertThat(call("POST","/api/members",admin,Map.of("name","恢复新增","phone","13900003333")).path("code").asInt()).isEqualTo(200);
        assertThat(customers.register("reopen_"+UUID.randomUUID().toString().substring(0,8),"customer123","恢复注册")).containsKey("memberId");
        int membersBeforeWechatLogin=jdbc.queryForObject("SELECT COUNT(*) FROM t_member",Integer.class);
        var pending=manualLogin.login(new WechatClient.Identity("reopen-app",UUID.randomUUID().toString(),""),"13900004444","恢复微信注册");
        assertThat(pending).containsEntry("bindRequired",true);
        assertThat((String)pending.get("bindTicket")).isNotBlank();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_member",Integer.class)).isEqualTo(membersBeforeWechatLogin);
    }

    @Test void closedDoesNotConsumeBindingTicketAndOpenPreservesSingleUseOnBadPassword() throws Exception {
        dictionary.update(true);
        String ticket=(String)wechatAccounts.login(new WechatClient.Identity("ticket-app",UUID.randomUUID().toString(),""),"STAFF").get("bindTicket");
        dictionary.update(false);
        var request=Map.of("ticket",ticket,"phone","13800000000","password","wrong-password");
        assertThat(call("POST","/api/wechat/bind",null,request).path("message").asText()).contains("系统正在维护");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_wechat_flow WHERE token_hash=?",Integer.class,WechatFlows.hash(ticket))).isEqualTo(1);
        dictionary.update(true);
        assertThat(call("POST","/api/wechat/bind",null,request).path("code").asInt()).isNotEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_wechat_flow WHERE token_hash=?",Integer.class,WechatFlows.hash(ticket))).isZero();
    }
    @Test void closedBlocksAllAdministrativeCreatesAndEdits() throws Exception {
        var paths=List.of("/api/members","/api/staff","/api/store","/api/rooms","/api/products","/api/banners","/api/rooms/1/closures");
        var tables=List.of("t_member","t_member_account","t_staff","t_store","t_tea_room","t_product","t_banner","t_room_closure");
        var before=new LinkedHashMap<String,Integer>();
        for(String table:tables) before.put(table,jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class));
        for(String path:paths) {
            var response=call("POST",path,admin,Map.of("name","新增测试","phone","13900005555","password","staff123","roleIds",List.of(1),"priceHour",100));
            assertThat(response.path("message").asText()).as(path).contains("系统正在维护");
        }
        for(String table:tables) assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class)).as(table).isEqualTo(before.get(table));
        dictionary.update(true);
        long id=call("POST","/api/products",admin,Map.of("name","开关测试商品")).path("data").asLong();
        assertThat(id).isPositive();
        dictionary.update(false);
        assertThat(call("POST","/api/products",admin,Map.of("id",id,"name","修改已有商品")).path("message").asText()).contains("系统正在维护");
        assertThat(jdbc.queryForObject("SELECT name FROM t_product WHERE id=?",String.class,id)).isEqualTo("开关测试商品");
        dictionary.update(true);
        assertThat(call("POST","/api/products",admin,Map.of("id",id,"name","修改已有商品")).path("code").asInt()).isEqualTo(200);
        dictionary.update(false);
        assertThat(jdbc.queryForObject("SELECT name FROM t_product WHERE id=?",String.class,id)).isEqualTo("修改已有商品");
        for(String path:List.of("/api/products","/api/rooms","/api/banners")) {
            assertThat(call("POST",path,admin,Map.of("id",999999999L,"name","伪造编辑","title","伪造编辑","status",1,"sortOrder",0,"target","none")).path("code").asInt()).as(path).isNotEqualTo(200);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_product",Integer.class)).isEqualTo(before.get("t_product")+1);
    }

    @Test void closedBlocksUpdatesDeletesAndReservationTransitionsWithoutChangingExistingData() throws Exception {
        var tables=List.of("t_member","t_staff","t_staff_role","t_staff_store","t_staff_wxpusher","t_store","t_product","t_tea_room","t_banner","t_reservation","t_room_closure");
        var before=new LinkedHashMap<String,List<Map<String,Object>>>();
        for(String table:tables) before.put(table,jdbc.queryForList("SELECT * FROM "+table));
        for(String path:List.of("/api/members/1","/api/staff/1","/api/staff/1/password","/api/staff/1/wxpusher","/api/store/1","/api/store/1/status","/api/reservations/1/schedule"))
            assertThat(call("PUT",path,admin,Map.of("name","禁止修改","status",0,"password","newpassword123","uid","","date","2026-09-12","startTime","10:00")).path("message").asText()).as(path).contains("系统正在维护");
        for(String path:List.of("/api/products/1/status","/api/staff/1/status")) {
            var result=mvc.perform(MockMvcRequestBuilders.put(path).param("status","0").header("Authorization","Bearer "+admin)).andReturn();
            assertThat(json.readTree(result.getResponse().getContentAsByteArray()).path("message").asText()).as(path).contains("系统正在维护");
        }
        for(String path:List.of("/api/products/1","/api/rooms/1","/api/rooms/1/closures/1","/api/banners/1","/api/store/1"))
            assertThat(call("DELETE",path,admin,null).path("message").asText()).as(path).contains("系统正在维护");
        for(String action:List.of("cancel","reject","verify","complete")) {
            String path="/api/reservations/1/"+action;
            assertThat(call("POST",path,admin,Map.of("reason","关闭期间")).path("message").asText()).as(path).contains("系统正在维护");
        }
        for(String table:tables) assertThat(jdbc.queryForList("SELECT * FROM "+table)).as(table).isEqualTo(before.get(table));
        assertThat(call("GET","/api/members",admin,null).path("code").asInt()).isEqualTo(200);
        assertThat(call("PUT","/api/business-dictionary",admin,Map.of("enabled",true)).path("data").path("enabled").asBoolean()).isTrue();
        assertThat(call("PUT","/api/members/1",admin,Map.of("name","恢复编辑","remark","","status",1)).path("code").asInt()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT name FROM t_member WHERE id=1",String.class)).isEqualTo("恢复编辑");
    }

}
