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
 JsonNode call(String path,Object body) throws Exception {var r=MockMvcRequestBuilders.post("/api/wechat/"+path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));return json.readTree(mvc.perform(r).andReturn().getResponse().getContentAsByteArray());}
 JsonNode data(JsonNode r){assertThat(r.path("code").asInt()).as(r.toString()).isEqualTo(200);return r.path("data");}
 String mock(String openid,String unionid){String code=UUID.randomUUID().toString();when(client.exchange("MINI",code)).thenReturn(new WechatClient.Identity("test-mini",openid,unionid));return code;}
 @Test void customerCreationIsIdempotentAndUnionIdentityUnifiesApps() throws Exception {
  String union=UUID.randomUUID().toString();String code=mock("open-"+union,union);
  JsonNode first=data(call("mini",Map.of("audience","CUSTOMER","code",code))).path("account");
  JsonNode second=data(call("mini",Map.of("audience","CUSTOMER","code",code))).path("account");
  assertThat(second.path("memberId")).isEqualTo(first.path("memberId"));
  String another=mock("another-"+union,union);
  assertThat(data(call("mini",Map.of("audience","CUSTOMER","code",another))).path("account").path("memberId")).isEqualTo(first.path("memberId"));
  assertThat(first.toString()).doesNotContain("openid","session_key","password");
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
  long id=data(call("mini",Map.of("audience","CUSTOMER","code",code))).path("account").path("memberId").asLong();
  jdbc.update("UPDATE t_member SET status=0 WHERE id=?",id);
  assertThat(call("mini",Map.of("audience","CUSTOMER","code",code)).path("code").asInt()).isNotEqualTo(200);
 }
 @Test void caseSensitiveWechatIdentifiersDoNotMerge() throws Exception {
  String suffix=UUID.randomUUID().toString();String a=mock("Open"+suffix,""),b=mock("open"+suffix,"");
  var first=data(call("mini",Map.of("audience","CUSTOMER","code",a))).path("account").path("memberId");
  var second=data(call("mini",Map.of("audience","CUSTOMER","code",b))).path("account").path("memberId");
  assertThat(first).isNotEqualTo(second);
 }
 @Test void validOAuthStateIsBoundToBrowserAndConsumedOnce() throws Exception {
  String browser=WechatFlows.random();String state=flows.create("STATE",Map.of("audience","CUSTOMER","browser",WechatFlows.hash(browser)));
  when(client.exchange("WEB","valid-web-code")).thenReturn(new WechatClient.Identity("test-web",UUID.randomUUID().toString(),""));
  var request=MockMvcRequestBuilders.post("/api/wechat/exchange").cookie(new jakarta.servlet.http.Cookie("wx_login",browser)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("state",state,"code","valid-web-code")));
  assertThat(data(json.readTree(mvc.perform(request).andReturn().getResponse().getContentAsByteArray())).path("account").path("token").asText()).isNotBlank();
  assertThat(json.readTree(mvc.perform(request).andReturn().getResponse().getContentAsByteArray()).path("code").asInt()).isNotEqualTo(200);
  verify(client,times(1)).exchange("WEB","valid-web-code");
 }
}
