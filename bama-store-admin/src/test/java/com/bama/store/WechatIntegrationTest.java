package com.bama.store;
import com.bama.store.service.*;
import com.bama.store.config.WechatProperties;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.http.MediaType;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:wechat_test;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE","logging.level.root=WARN","logging.level.com.bama.store=WARN","mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"})
@ActiveProfiles("h2") @AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
class WechatIntegrationTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired WechatFlows flows;
 @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
 @MockBean WechatClient client;
 @MockBean SmsSender sms;
 @Autowired CustomerAuthService customers;
 @Autowired SmsChallenges challenges;
 @Autowired StaffWechatInvitations invitations;
 @Autowired StaffWechatMembership staffMembership;
 final Map<String,String> received=new HashMap<>();
 static final java.util.concurrent.atomic.AtomicLong phones=new java.util.concurrent.atomic.AtomicLong(13910000000L);
 @org.junit.jupiter.api.BeforeEach void smsSetup() {
  jdbc.update("DELETE FROM t_sms_challenge");
  when(sms.ready()).thenReturn(true);
  doAnswer(invocation->{received.put(invocation.getArgument(0),invocation.getArgument(1));return null;}).when(sms).send(anyString(),anyString());
 }
 JsonNode call(String path,Object body) throws Exception {var r=MockMvcRequestBuilders.post("/api/wechat/"+path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));return json.readTree(mvc.perform(r).andReturn().getResponse().getContentAsByteArray());}
 JsonNode data(JsonNode r){assertThat(r.path("code").asInt()).as(r.toString()).isEqualTo(200);return r.path("data");}
 String mock(String openid,String unionid){String code=UUID.randomUUID().toString();when(client.exchange("MINI",code)).thenReturn(new WechatClient.Identity("test-mini",openid,unionid));return code;}
 JsonNode pending(String code) throws Exception {return data(call("mini",Map.of("audience","CUSTOMER","code",code)));}
 JsonNode bindCustomer(JsonNode pending,String phone) throws Exception {
  String ticket=pending.path("bindTicket").asText();
  String challenge=data(call("customer/sms",Map.of("ticket",ticket,"phone",phone))).path("challenge").asText();
  return data(call("customer/bind",Map.of("ticket",ticket,"phone",phone,"challenge",challenge,"code",received.get(phone)))).path("account");
 }
 JsonNode customer(String code) throws Exception {
  var result=pending(code);
  return result.path("bindRequired").asBoolean()?bindCustomer(result,Long.toString(phones.incrementAndGet())):result.path("account");
 }
 @Test void miniCodeRequiresStaffStoreAccessAndNeverReturnsCredentials() throws Exception {
  var login=MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"phone\":\"13800000000\",\"password\":\"admin123\"}");
  String admin=data(json.readTree(mvc.perform(login).andReturn().getResponse().getContentAsByteArray())).path("token").asText();
  when(client.miniCode(1L)).thenReturn("data:image/png;base64,dGVzdA==");
  var get=MockMvcRequestBuilders.get("/api/store/1/mini-code").header("Authorization","Bearer "+admin);
  var result=mvc.perform(get).andReturn().getResponse();
  var code=data(json.readTree(result.getContentAsByteArray()));
  assertThat(code.path("image").asText()).startsWith("data:image/png;base64,");
  assertThat(code.toString()).doesNotContain("access_token","secret","session_key");
  assertThat(result.getHeader("Cache-Control")).isEqualTo("no-store");
  assertThat(json.readTree(mvc.perform(MockMvcRequestBuilders.get("/api/store/1/mini-code")).andReturn().getResponse().getContentAsByteArray()).path("code").asInt()).isEqualTo(401);
  String customer=customer(mock(UUID.randomUUID().toString(),"")).path("token").asText();
  assertThat(json.readTree(mvc.perform(MockMvcRequestBuilders.get("/api/store/1/mini-code").header("Authorization","Bearer "+customer)).andReturn().getResponse().getContentAsByteArray()).path("code").asInt()).isEqualTo(403);
  assertThat(json.readTree(mvc.perform(MockMvcRequestBuilders.get("/api/store/999999/mini-code").header("Authorization","Bearer "+admin)).andReturn().getResponse().getContentAsByteArray()).path("code").asInt()).isEqualTo(403);
  verify(client,times(1)).miniCode(1L);
  verify(client,never()).miniCode(999999L);
 }
 @Test void customerCreationIsIdempotentAndUnionIdentityUnifiesApps() throws Exception {
  String union=UUID.randomUUID().toString();String code=mock("open-"+union,union);
  JsonNode first=customer(code);
  JsonNode second=customer(code);
  assertThat(second.path("memberId")).isEqualTo(first.path("memberId"));
  String another=mock("another-"+union,union);
  assertThat(data(call("mini",Map.of("audience","CUSTOMER","code",another))).path("account").path("memberId")).isEqualTo(first.path("memberId"));
  assertThat(first.toString()).doesNotContain("openid","session_key","password");
 }
 long freshStaff(String phone) {
  String number="WX"+UUID.randomUUID().toString().replace("-","").substring(0,16);
  jdbc.update("INSERT INTO t_staff(staff_no,name,phone,password,store_id,status,deleted) SELECT ?,?,?,password,1,1,0 FROM t_staff WHERE phone='13800000001'",number,"扫码员工",phone);
  return jdbc.queryForObject("SELECT id FROM t_staff WHERE phone=?",Long.class,phone);
 }
 @Test void staffMemberBackfillPreservesExistingMemberWalletAndIsIdempotent() {
  String phone=Long.toString(phones.incrementAndGet());long staffId=freshStaff(phone);
  Long memberId=customers.createWechatMember(null);
  String key="APP:"+UUID.randomUUID();
  jdbc.update("INSERT INTO t_wechat_account(identity_key,audience,account_id) VALUES(?,'STAFF',?)",key,staffId);
  jdbc.update("INSERT INTO t_wechat_account(identity_key,audience,account_id) VALUES(?,'CUSTOMER',?)",key,memberId);
  jdbc.update("UPDATE t_member SET level='GOLD',points=88 WHERE id=?",memberId);
  jdbc.update("UPDATE t_member_account SET balance=268,total_recharge=500,total_consume=232 WHERE member_id=?",memberId);
  assertThat(staffMembership.synchronize(staffId)).isEqualTo(memberId);
  assertThat(staffMembership.synchronize(staffId)).isEqualTo(memberId);
  var member=jdbc.queryForMap("SELECT name,phone,level,points FROM t_member WHERE id=?",memberId);
  assertThat(member.get("name")).isEqualTo("扫码员工");assertThat(member.get("phone")).isEqualTo(phone);
  assertThat(member.get("level")).isEqualTo("GOLD");assertThat(((Number)member.get("points")).intValue()).isEqualTo(88);
  assertThat(jdbc.queryForObject("SELECT balance FROM t_member_account WHERE member_id=?",java.math.BigDecimal.class,memberId)).isEqualByComparingTo("268");
 }
 @Test void memberPhoneCollisionRollsBackEmployeeBindingAndInvitationRemainsValid() throws Exception {
  String phone=Long.toString(phones.incrementAndGet());long staffId=freshStaff(phone);
  Long existing=customers.createWechatMember(phone);
  String ticket=invitations.create(staffId),code=mock(UUID.randomUUID().toString(),"");
  assertThat(call("staff/bind",Map.of("ticket",ticket,"phone",phone,"code",code)).path("code").asInt()).isNotEqualTo(200);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_wechat_account WHERE audience='STAFF' AND account_id=?",Integer.class,staffId)).isZero();
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_wechat_flow WHERE token_hash=?",Integer.class,WechatFlows.hash(ticket))).isEqualTo(1);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_wechat_account WHERE audience='CUSTOMER' AND account_id=?",Integer.class,existing)).isZero();
 }
 @Test void staffQrBindingChecksPhoneOneTimeTicketAndWechatOwnership() throws Exception {
  String phone=Long.toString(phones.incrementAndGet());long staffId=freshStaff(phone);
  String ticket=invitations.create(staffId);
  String openid=UUID.randomUUID().toString(), code=mock(openid,"");
  assertThat(call("staff/bind",Map.of("staffId",staffId,"phone",phone,"code",code)).path("code").asInt()).isNotEqualTo(200);
  assertThat(call("staff/bind",Map.of("ticket",ticket,"phone","13800000000","code",code)).path("code").asInt()).isNotEqualTo(200);
  verify(client,never()).exchange("MINI",code);
  var body=Map.of("ticket",ticket,"phone",phone,"code",code);
  var account=data(call("staff/bind",body)).path("account");
    assertThat(account.path("staffId").asLong()).isEqualTo(staffId);
    var member=jdbc.queryForMap("SELECT id,name FROM t_member WHERE phone=? AND deleted=0",phone);
    assertThat(member.get("name")).isEqualTo("扫码员工");
    assertThat(jdbc.queryForObject("SELECT balance FROM t_member_account WHERE member_id=?",java.math.BigDecimal.class,member.get("id"))).isEqualByComparingTo("0");
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_wechat_account WHERE audience='CUSTOMER' AND account_id=?",Integer.class,member.get("id"))).isPositive();
  assertThat(call("staff/bind",body).path("code").asInt()).isNotEqualTo(200);
  assertThat(data(call("mini",Map.of("audience","STAFF","code",code))).path("account").path("staffId").asLong()).isEqualTo(staffId);
  String otherPhone=Long.toString(phones.incrementAndGet());long other=freshStaff(otherPhone);
  assertThat(call("staff/bind",Map.of("ticket",invitations.create(other),"phone",otherPhone,"code",code)).path("code").asInt()).isNotEqualTo(200);
  assertThatThrownBy(()->invitations.create(staffId)).hasMessageContaining("已绑定");
 }
 @Test void refreshedAndExpiredInvitationsCannotBind() throws Exception {
  String phone=Long.toString(phones.incrementAndGet());long id=freshStaff(phone);
  String old=invitations.create(id), fresh=invitations.create(id);
  assertThat(call("staff/bind",Map.of("ticket",old,"phone",phone,"code","unused")).path("code").asInt()).isNotEqualTo(200);
  jdbc.update("UPDATE t_wechat_flow SET expires_at=? WHERE token_hash=?",java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(1)),WechatFlows.hash(fresh));
  assertThat(call("staff/bind",Map.of("ticket",fresh,"phone",phone,"code","unused")).path("code").asInt()).isNotEqualTo(200);
  verify(client,never()).exchange("MINI","unused");
 }
 @Test void staffQrRequiresManagementPermissionAndActiveEmployee() throws Exception {
  var login=MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"phone\":\"13800000000\",\"password\":\"admin123\"}");
  String token=data(json.readTree(mvc.perform(login).andReturn().getResponse().getContentAsByteArray())).path("token").asText();
  String phone=Long.toString(phones.incrementAndGet());long id=freshStaff(phone);
  when(client.staffBindCode(anyString())).thenReturn("data:image/png;base64,dGVzdA==");
  String path="/api/staff/"+id+"/wechat-code";
  assertThat(json.readTree(mvc.perform(MockMvcRequestBuilders.get(path)).andReturn().getResponse().getContentAsByteArray()).path("code").asInt()).isEqualTo(401);
  var response=mvc.perform(MockMvcRequestBuilders.get(path).header("Authorization","Bearer "+token)).andReturn().getResponse();
  assertThat(data(json.readTree(response.getContentAsByteArray())).path("image").asText()).startsWith("data:image/png");
  assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
  var captured=org.mockito.ArgumentCaptor.forClass(String.class);
  verify(client,times(1)).staffBindCode(captured.capture());
  jdbc.update("UPDATE t_staff SET status=0 WHERE id=?",id);
  try {
   assertThat(json.readTree(mvc.perform(MockMvcRequestBuilders.get(path).header("Authorization","Bearer "+token)).andReturn().getResponse().getContentAsByteArray()).path("code").asInt()).isNotEqualTo(200);
   assertThat(call("staff/bind",Map.of("ticket",captured.getValue(),"phone",phone,"code","disabled")).path("code").asInt()).isNotEqualTo(200);
   verify(client,never()).exchange("MINI","disabled");
  } finally {jdbc.update("UPDATE t_staff SET status=1 WHERE id=?",id);}
  verify(client,times(1)).staffBindCode(anyString());
 }
 @Test void staffMustBindExistingCredentialsAndTicketCannotBeReplayed() throws Exception {
  String code=mock(UUID.randomUUID().toString(),"");
  JsonNode pending=data(call("mini",Map.of("audience","STAFF","code",code)));
  assertThat(pending.path("bindRequired").asBoolean()).isTrue();assertThat(pending.has("account")).isFalse();
  var body=Map.of("ticket",pending.path("bindTicket").asText(),"phone","13800000001","password","123456");
  JsonNode account=data(call("bind",body)).path("account");
  assertThat(account.path("permissions").toString()).doesNotContain("store:all");
  assertThat(call("bind",body).path("code").asInt()).isNotEqualTo(200);
  assertThat(data(call("mini",Map.of("audience","STAFF","code",code))).path("account").path("staffId")).isEqualTo(account.path("staffId"));
  // The same WeChat may also be a customer; it never inherits staff permissions.
  assertThat(data(call("mini",Map.of("audience","CUSTOMER","code",code))).path("account").has("permissions")).isFalse();
 }
 @Test void invalidBindingDoesNotGrantAccessAndConsumesTicket() throws Exception {
  String code=mock(UUID.randomUUID().toString(),"");String ticket=data(call("mini",Map.of("audience","STAFF","code",code))).path("bindTicket").asText();
  assertThat(call("bind",Map.of("ticket",ticket,"phone","13800000000","password","wrong")).path("code").asInt()).isNotEqualTo(200);
  assertThat(call("bind",Map.of("ticket",ticket,"phone","13800000000","password","admin123")).path("code").asInt()).isNotEqualTo(200);
 }
 @Test void disabledConfigurationAndOAuthStateFailClosed() throws Exception {
  assertThatThrownBy(()->new WechatClient(new WechatProperties(),json).exchange("MINI","code")).hasMessageContaining("尚未配置");
  String state=flows.create("STATE",Map.of("audience","STAFF","browser",WechatFlows.hash("expected")));
  assertThat(call("exchange",Map.of("state",state,"code","forged")).path("code").asInt()).isNotEqualTo(200);
  verify(client,never()).exchange("WEB","forged");
  assertThat(call("mini",Map.of("audience","ADMIN","code","forged")).path("code").asInt()).isNotEqualTo(200);
 }
 @Test void disabledCustomerCannotUseWechatToReturn() throws Exception {
  String code=mock(UUID.randomUUID().toString(),"");
  long id=customer(code).path("memberId").asLong();
  jdbc.update("UPDATE t_member SET status=0 WHERE id=?",id);
  assertThat(call("mini",Map.of("audience","CUSTOMER","code",code)).path("code").asInt()).isNotEqualTo(200);
 }
 @Test void caseSensitiveWechatIdentifiersDoNotMerge() throws Exception {
  String suffix=UUID.randomUUID().toString();String a=mock("Open"+suffix,""),b=mock("open"+suffix,"");
  var first=customer(a).path("memberId");
  var second=customer(b).path("memberId");
  assertThat(first).isNotEqualTo(second);
 }
 @Test void firstLoginNeverIssuesTokenOrCreatesMemberBeforeVerification() throws Exception {
  int before=jdbc.queryForObject("SELECT COUNT(*) FROM t_member",Integer.class);
  var result=pending(mock(UUID.randomUUID().toString(),""));
  assertThat(result.path("bindRequired").asBoolean()).isTrue();
  assertThat(result.has("account")).isFalse();
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_member",Integer.class)).isEqualTo(before);
  assertThat(call("customer/bind",Map.of("ticket",result.path("bindTicket").asText(),"phone","13922220000","challenge","forged","code","123456")).path("code").asInt()).isNotEqualTo(200);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_member",Integer.class)).isEqualTo(before);
 }
 @Test void verifiedExistingMemberKeepsItsBalanceAndSecondLoginSendsNoSms() throws Exception {
  String phone=Long.toString(phones.incrementAndGet());
  long id=customers.createWechatMember(phone);
  jdbc.update("UPDATE t_member_account SET balance=321.45 WHERE member_id=?",id);
  String code=mock(UUID.randomUUID().toString(),"");
  assertThat(bindCustomer(pending(code),phone).path("memberId").asLong()).isEqualTo(id);
  clearInvocations(sms);
  assertThat(pending(code).path("account").path("memberId").asLong()).isEqualTo(id);
  verify(sms,never()).send(anyString(),anyString());
  assertThat(jdbc.queryForObject("SELECT balance FROM t_member_account WHERE member_id=?",java.math.BigDecimal.class,id)).isEqualByComparingTo("321.45");
 }
 @Test void codesAreBoundToTicketAndPhoneAndLockAfterFiveFailures() throws Exception {
  String phone=Long.toString(phones.incrementAndGet());
  String ticket=pending(mock(UUID.randomUUID().toString(),"")).path("bindTicket").asText();
  String challenge=data(call("customer/sms",Map.of("ticket",ticket,"phone",phone))).path("challenge").asText();
  String value=received.get(phone);
  String wrong=value.equals("000000")?"111111":"000000";
  assertThat(challenges.verify(ticket,"13999998888",challenge,value)).isFalse();
  assertThat(challenges.verify(WechatFlows.random(),phone,challenge,value)).isFalse();
  for(int n=0;n<3;n++)assertThat(challenges.verify(ticket,phone,challenge,wrong)).isFalse();
  assertThat(challenges.verify(ticket,phone,challenge,value)).isFalse();
  assertThat(jdbc.queryForObject("SELECT attempts FROM t_sms_challenge WHERE token_hash=?",Integer.class,WechatFlows.hash(challenge))).isEqualTo(5);
  assertThat(jdbc.queryForObject("SELECT code_hash FROM t_sms_challenge WHERE token_hash=?",String.class,WechatFlows.hash(challenge))).doesNotContain(value);
 }
 @Test void verificationAndBindingCannotBeReplayed() throws Exception {
  String phone=Long.toString(phones.incrementAndGet());
  String ticket=pending(mock(UUID.randomUUID().toString(),"")).path("bindTicket").asText();
  String challenge=data(call("customer/sms",Map.of("ticket",ticket,"phone",phone))).path("challenge").asText();
  var body=Map.of("ticket",ticket,"phone",phone,"challenge",challenge,"code",received.get(phone));
  data(call("customer/bind",body));
  assertThat(call("customer/bind",body).path("code").asInt()).isNotEqualTo(200);
  assertThat(challenges.verify(ticket,phone,challenge,received.get(phone))).isFalse();
 }
 @Test void expiredCodeAndExpiredTicketCannotBind() throws Exception {
  String phone=Long.toString(phones.incrementAndGet());
  String ticket=pending(mock(UUID.randomUUID().toString(),"")).path("bindTicket").asText();
  String challenge=data(call("customer/sms",Map.of("ticket",ticket,"phone",phone))).path("challenge").asText();
  jdbc.update("UPDATE t_sms_challenge SET expires_at=?",java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(1)));
  assertThat(challenges.verify(ticket,phone,challenge,received.get(phone))).isFalse();
  jdbc.update("UPDATE t_wechat_flow SET expires_at=? WHERE token_hash=?",java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(1)),WechatFlows.hash(ticket));
  assertThat(call("customer/sms",Map.of("ticket",ticket,"phone",phone)).path("code").asInt()).isNotEqualTo(200);
 }
 @Test void smsDisabledProviderFailureAndCooldownFailClosed() throws Exception {
  String phone=Long.toString(phones.incrementAndGet());
  String ticket=pending(mock(UUID.randomUUID().toString(),"")).path("bindTicket").asText();
  when(sms.ready()).thenReturn(false);
  assertThat(call("customer/sms",Map.of("ticket",ticket,"phone",phone)).path("code").asInt()).isNotEqualTo(200);
  verify(sms,never()).send(anyString(),anyString());
  when(sms.ready()).thenReturn(true);
  doThrow(new com.bama.store.common.BusinessException("短信发送失败")).when(sms).send(eq(phone),anyString());
  assertThat(call("customer/sms",Map.of("ticket",ticket,"phone",phone)).path("code").asInt()).isNotEqualTo(200);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_sms_challenge WHERE ready=1",Integer.class)).isZero();
  assertThat(call("customer/sms",Map.of("ticket",ticket,"phone",phone)).path("code").asInt()).isNotEqualTo(200);
  verify(sms,times(1)).send(eq(phone),anyString());
 }
 @Test void differentWechatCannotTakeOverAlreadyBoundMember() throws Exception {
  String phone=Long.toString(phones.incrementAndGet());
  long id=bindCustomer(pending(mock(UUID.randomUUID().toString(),"")),phone).path("memberId").asLong();
  jdbc.update("DELETE FROM t_sms_challenge");
  String ticket=pending(mock(UUID.randomUUID().toString(),"")).path("bindTicket").asText();
  String challenge=data(call("customer/sms",Map.of("ticket",ticket,"phone",phone))).path("challenge").asText();
  assertThat(call("customer/bind",Map.of("ticket",ticket,"phone",phone,"challenge",challenge,"code",received.get(phone))).path("code").asInt()).isNotEqualTo(200);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_wechat_account WHERE audience='CUSTOMER' AND account_id=?",Integer.class,id)).isEqualTo(1);
 }
 @Test void legacyWechatMemberIsPreservedAndConflictingAccountsAreNotMerged() throws Exception {
  var legacy=customers.register("legacy_"+phones.incrementAndGet(),"password123","老顾客");
  long id=((Number)legacy.get("memberId")).longValue();
  String openid=UUID.randomUUID().toString();
  jdbc.update("INSERT INTO t_wechat_account(identity_key,audience,account_id) VALUES(?,'CUSTOMER',?)","APP:"+WechatFlows.hash("test-mini:"+openid),id);
  jdbc.update("UPDATE t_member_account SET balance=77 WHERE member_id=?",id);
  String code=mock(openid,"");
  String phone=Long.toString(phones.incrementAndGet());
  long other=customers.createWechatMember(phone);
  String ticket=pending(code).path("bindTicket").asText();
  String challenge=data(call("customer/sms",Map.of("ticket",ticket,"phone",phone))).path("challenge").asText();
  assertThat(call("customer/bind",Map.of("ticket",ticket,"phone",phone,"challenge",challenge,"code",received.get(phone))).path("code").asInt()).isNotEqualTo(200);
  assertThat(jdbc.queryForObject("SELECT balance FROM t_member_account WHERE member_id=?",java.math.BigDecimal.class,id)).isEqualByComparingTo("77");
  assertThat(customers.requireActive(other).getPhone()).isEqualTo(phone);
  jdbc.update("DELETE FROM t_sms_challenge");
  assertThat(bindCustomer(pending(code),Long.toString(phones.incrementAndGet())).path("memberId").asLong()).isEqualTo(id);
 }
 @Test void frozenMemberCannotBeClaimedWithVerifiedPhone() throws Exception {
  String phone=Long.toString(phones.incrementAndGet());long id=customers.createWechatMember(phone);
  jdbc.update("UPDATE t_member SET status=0 WHERE id=?",id);
  String ticket=pending(mock(UUID.randomUUID().toString(),"")).path("bindTicket").asText();
  String challenge=data(call("customer/sms",Map.of("ticket",ticket,"phone",phone))).path("challenge").asText();
  assertThat(call("customer/bind",Map.of("ticket",ticket,"phone",phone,"challenge",challenge,"code",received.get(phone))).path("code").asInt()).isNotEqualTo(200);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_wechat_phone_verified WHERE member_id=?",Integer.class,id)).isZero();
 }
 @Test void concurrentCodeConsumptionSucceedsOnlyOnce() throws Exception {
  String phone=Long.toString(phones.incrementAndGet());String ticket=pending(mock(UUID.randomUUID().toString(),"")).path("bindTicket").asText();
  String challenge=data(call("customer/sms",Map.of("ticket",ticket,"phone",phone))).path("challenge").asText();
  String code=received.get(phone);
  var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
  try {
   var results=executor.invokeAll(List.of(()->challenges.verify(ticket,phone,challenge,code),()->challenges.verify(ticket,phone,challenge,code)));
   int successes=0;for(var future:results)if(Boolean.TRUE.equals(future.get()))successes++;
   assertThat(successes).isEqualTo(1);
  } finally {executor.shutdownNow();}
 }
 @Test void validOAuthStateIsBoundToBrowserAndConsumedOnce() throws Exception {
  String browser=WechatFlows.random();String state=flows.create("STATE",Map.of("audience","CUSTOMER","browser",WechatFlows.hash(browser)));
  when(client.exchange("WEB","valid-web-code")).thenReturn(new WechatClient.Identity("test-web",UUID.randomUUID().toString(),""));
  var request=MockMvcRequestBuilders.post("/api/wechat/exchange").cookie(new jakarta.servlet.http.Cookie("wx_login",browser)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("state",state,"code","valid-web-code")));
  assertThat(data(json.readTree(mvc.perform(request).andReturn().getResponse().getContentAsByteArray())).path("bindRequired").asBoolean()).isTrue();
  assertThat(json.readTree(mvc.perform(request).andReturn().getResponse().getContentAsByteArray()).path("code").asInt()).isNotEqualTo(200);
  verify(client,times(1)).exchange("WEB","valid-web-code");
 }
}
