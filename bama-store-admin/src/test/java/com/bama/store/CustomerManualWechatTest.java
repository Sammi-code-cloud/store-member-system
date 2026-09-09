package com.bama.store;

import com.bama.store.service.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:manual_login;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE","logging.level.root=ERROR","logging.level.com.bama.store=ERROR","mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"})
@ActiveProfiles("h2") @AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
class CustomerManualWechatTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc;
 @MockBean WechatClient wechat;
 String code; String phone;
 @BeforeEach void setup(){code=UUID.randomUUID().toString();phone="135"+String.format("%08d",Math.abs(UUID.randomUUID().getLeastSignificantBits()%100000000));when(wechat.exchange("MINI",code)).thenReturn(new WechatClient.Identity("app",code,""));}
 JsonNode login(String phone) throws Exception{return json.readTree(mvc.perform(post("/api/wechat/customer/manual-login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("code",code,"phone",phone,"name","测试称呼")))).andReturn().getResponse().getContentAsByteArray());}
 @Test void newWechatCreatesMemberAndRepeatedLoginPreservesWallet() throws Exception {
  var first=login(phone);assertThat(first.path("code").asInt()).isEqualTo(200);long id=first.at("/data/account/memberId").asLong();
  assertThat(jdbc.queryForObject("SELECT balance FROM t_member_account WHERE member_id=?",java.math.BigDecimal.class,id)).isEqualByComparingTo("0");
  jdbc.update("UPDATE t_member_account SET balance=268 WHERE member_id=?",id);
  jdbc.update("UPDATE t_member SET points=88,level='GOLD' WHERE id=?",id);
  var again=login(phone);assertThat(again.at("/data/account/memberId").asLong()).isEqualTo(id);
  assertThat(jdbc.queryForObject("SELECT balance FROM t_member_account WHERE member_id=?",java.math.BigDecimal.class,id)).isEqualByComparingTo("268");
  assertThat(jdbc.queryForObject("SELECT points FROM t_member WHERE id=?",Integer.class,id)).isEqualTo(88);
  assertThat(jdbc.queryForObject("SELECT name FROM t_member WHERE id=?",String.class,id)).isEqualTo("测试称呼");
 }
 @Test void anotherWechatCannotClaimExistingPhoneOrMoney() throws Exception {
  var first=login(phone);long id=first.at("/data/account/memberId").asLong();
  jdbc.update("UPDATE t_member_account SET balance=300 WHERE member_id=?",id);
  code=UUID.randomUUID().toString();when(wechat.exchange("MINI",code)).thenReturn(new WechatClient.Identity("app",code,""));
  assertThat(login(phone).path("code").asInt()).isEqualTo(400);
  assertThat(jdbc.queryForObject("SELECT balance FROM t_member_account WHERE member_id=?",java.math.BigDecimal.class,id)).isEqualByComparingTo("300");
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_member WHERE phone=?",Integer.class,phone)).isEqualTo(1);
 }
 @Test void wrongPhoneOrDisabledMemberCannotLogin() throws Exception {
  long id=login(phone).at("/data/account/memberId").asLong();
  assertThat(login("13400001234").path("code").asInt()).isEqualTo(400);
  jdbc.update("UPDATE t_member SET status=0 WHERE id=?",id);
  assertThat(login(phone).path("code").asInt()).isEqualTo(400);
 }
 @Test void legacyWechatMemberCanFillPhoneWithoutChangingIdentity() throws Exception {
  jdbc.update("INSERT INTO t_wechat_account(identity_key,audience,account_id) VALUES(?,'CUSTOMER',1)",WechatFlows.hash("unused"));
  jdbc.update("INSERT INTO t_wechat_account(identity_key,audience,account_id) VALUES(?,'CUSTOMER',1)","APP:"+WechatFlows.hash("app:"+code));
  var result=login("13800006620");assertThat(result.path("code").asInt()).isEqualTo(200);assertThat(result.at("/data/account/memberId").asLong()).isEqualTo(1);
  assertThat(jdbc.queryForObject("SELECT balance FROM t_member_account WHERE member_id=1",java.math.BigDecimal.class)).isEqualByComparingTo("2860");
 }
 @Test void invalidPhoneNeverExchangesWechatCode() throws Exception {
  assertThat(login("123").path("code").asInt()).isEqualTo(400);verify(wechat,never()).exchange(anyString(),anyString());
 }

 JsonNode adminCall(String path,Map<String,Object> body,String token) throws Exception {
  var request=post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
  if(token!=null)request.header("Authorization","Bearer "+token);
  return json.readTree(mvc.perform(request).andReturn().getResponse().getContentAsByteArray());
 }
 String admin() throws Exception {return adminCall("/api/auth/login",Map.of("phone","13800000000","password","admin123"),null).at("/data/token").asText();}
 @Test void adminCreatesMemberThenQrBindsOriginalWalletOnce() throws Exception {
  String token=admin();
  var created=adminCall("/api/members",Map.of("name","线下会员","phone",phone),token);
  assertThat(created.path("code").asInt()).isEqualTo(200);long id=created.at("/data/id").asLong();
  jdbc.update("UPDATE t_member_account SET balance=200,total_recharge=200 WHERE member_id=?",id);
  when(wechat.memberBindCode(anyString())).thenReturn("data:image/png;base64,test");
  assertThat(adminCall("/api/members/"+id+"/wechat-code",Map.of(),token).path("code").asInt()).isEqualTo(200);
  var capture=org.mockito.ArgumentCaptor.forClass(String.class);verify(wechat).memberBindCode(capture.capture());String ticket=capture.getValue();
  String route="/api/wechat/customer/member-bind";
  assertThat(adminCall(route,Map.of("ticket",ticket,"phone","13300001234","code",code),null).path("code").asInt()).isEqualTo(400);
  var result=adminCall(route,Map.of("ticket",ticket,"phone",phone,"code",code),null);
  assertThat(result.path("code").asInt()).isEqualTo(200);assertThat(result.at("/data/account/memberId").asLong()).isEqualTo(id);
  assertThat(jdbc.queryForObject("SELECT balance FROM t_member_account WHERE member_id=?",java.math.BigDecimal.class,id)).isEqualByComparingTo("200");
  assertThat(adminCall(route,Map.of("ticket",ticket,"phone",phone,"code",code),null).path("code").asInt()).isEqualTo(400);
  assertThat(adminCall("/api/members/"+id+"/wechat-code",Map.of(),token).at("/data/bound").asBoolean()).isTrue();
  assertThat(login(phone).at("/data/account/memberId").asLong()).isEqualTo(id);
 }
 @Test void memberQrRefreshRevokesOldCodeAndExistingWechatCannotBeRebound() throws Exception {
  long original=login(phone).at("/data/account/memberId").asLong();
  String other="132"+String.format("%08d",Math.abs(UUID.randomUUID().getLeastSignificantBits()%100000000)),token=admin();
  long id=adminCall("/api/members",Map.of("name","另一会员","phone",other),token).at("/data/id").asLong();
  when(wechat.memberBindCode(anyString())).thenReturn("data:image/png;base64,test");
  adminCall("/api/members/"+id+"/wechat-code",Map.of(),token);adminCall("/api/members/"+id+"/wechat-code",Map.of(),token);
  var capture=org.mockito.ArgumentCaptor.forClass(String.class);verify(wechat,times(2)).memberBindCode(capture.capture());
  for(String ticket:capture.getAllValues())assertThat(adminCall("/api/wechat/customer/member-bind",Map.of("ticket",ticket,"phone",other,"code",code),null).path("code").asInt()).isEqualTo(400);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_wechat_account WHERE audience='CUSTOMER' AND account_id=?",Integer.class,id)).isZero();
  assertThat(login(phone).at("/data/account/memberId").asLong()).isEqualTo(original);
 }
 @Test void manualMemberCreationRequiresPermissionAndRejectsDuplicatePhone() throws Exception {
  assertThat(adminCall("/api/members",Map.of("name","测试","phone",phone),null).path("code").asInt()).isEqualTo(401);
  String token=admin();assertThat(adminCall("/api/members",Map.of("name","测试","phone",phone),token).path("code").asInt()).isEqualTo(200);
  assertThat(adminCall("/api/members",Map.of("name","重复","phone",phone),token).path("code").asInt()).isEqualTo(400);
 }

 @Test void returningWechatLogsInWithoutPhoneAndPreservesAccount() throws Exception {
  long id=login(phone).at("/data/account/memberId").asLong();
  jdbc.update("UPDATE t_member_account SET balance=157 WHERE member_id=?",id);
  var again=adminCall("/api/wechat/customer/manual-login",Map.of("code",code),null);
  assertThat(again.path("code").asInt()).isEqualTo(200);
  assertThat(again.at("/data/account/memberId").asLong()).isEqualTo(id);
  assertThat(again.at("/data/account/token").asText()).isNotBlank();
  assertThat(jdbc.queryForObject("SELECT balance FROM t_member_account WHERE member_id=?",java.math.BigDecimal.class,id)).isEqualByComparingTo("157");
  jdbc.update("UPDATE t_member SET status=0 WHERE id=?",id);
  assertThat(login("").path("code").asInt()).isEqualTo(400);
 }
 @Test void unknownWechatRequestsPhoneWithoutCreatingMember() throws Exception {
  int before=jdbc.queryForObject("SELECT COUNT(*) FROM t_member",Integer.class);
  var result=login("");
  assertThat(result.at("/data/manualPhoneRequired").asBoolean()).isTrue();
  assertThat(result.at("/data/account").isMissingNode()).isTrue();
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_member",Integer.class)).isEqualTo(before);
 }
 @Test void boundMemberWithoutPhoneMustCompleteProfile() throws Exception {
  long id=login(phone).at("/data/account/memberId").asLong();
  jdbc.update("UPDATE t_member SET phone=NULL WHERE id=?",id);
  assertThat(login("").at("/data/manualPhoneRequired").asBoolean()).isTrue();
  assertThat(login(phone).at("/data/account/memberId").asLong()).isEqualTo(id);
  assertThat(login("").at("/data/account/memberId").asLong()).isEqualTo(id);
 }

 @Test void desktopEndpointsAllowOnlyBoundStaffAndBrowserCanCollect() throws Exception {
  when(wechat.desktopLoginCode(anyString())).thenReturn("data:image/png;base64,test");
  var created=adminCall("/api/wechat/desktop/create",Map.of(),null);
  assertThat(created.path("code").asInt()).isEqualTo(200);
  String ticket=created.at("/data/ticket").asText(),secret=created.at("/data/secret").asText();
  assertThat(adminCall("/api/wechat/desktop/poll",Map.of("ticket",ticket,"secret",secret),null).at("/data/status").asText()).isEqualTo("WAITING");
  jdbc.update("INSERT INTO t_wechat_account(identity_key,audience,account_id) VALUES(?,'STAFF',1)","APP:"+WechatFlows.hash("app:"+code));
  assertThat(adminCall("/api/wechat/desktop/confirm",Map.of("ticket",ticket,"code",code),null).path("code").asInt()).isEqualTo(200);
  var result=adminCall("/api/wechat/desktop/poll",Map.of("ticket",ticket,"secret",secret),null);
  assertThat(result.at("/data/status").asText()).isEqualTo("CONFIRMED");
  assertThat(result.at("/data/account/staffId").asLong()).isEqualTo(1);
 }
}
