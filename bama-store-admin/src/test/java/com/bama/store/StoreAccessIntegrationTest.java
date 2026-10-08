package com.bama.store;

import com.bama.store.service.BookingRules;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:store_access;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE","logging.level.root=ERROR","logging.level.com.bama.store=ERROR","mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"})
@ActiveProfiles("h2")
@AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
@Transactional
@org.springframework.test.context.jdbc.Sql(statements = "UPDATE t_business_dictionary SET dict_value='1' WHERE dict_key='business_enabled'")
class StoreAccessIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.bama.store.service.WxPusherRecipients noticeRecipients;

    @Test void wxpusherSettingsAreAuthorizedStoreScopedAndImmediatelyApplied() throws Exception {
        String admin=login();
        data(call("PUT","/api/staff/2/wxpusher",admin,1L,Map.of("uid"," UID_cashier ")));
        assertThat(noticeRecipients.recipients(1L)).containsExactly("UID_cashier");
        var staff=data(call("GET","/api/staff",admin,1L,null)).path("records");
        assertThat(java.util.stream.StreamSupport.stream(staff.spliterator(),false)
                .filter(s -> s.path("id").asLong()==2).findFirst().orElseThrow().path("wxpusherUid").asText()).isEqualTo("UID_cashier");
        assertThat(call("PUT","/api/staff/2/wxpusher",admin,1L,Map.of("uid","invalid")).path("code").asInt()).isEqualTo(400);
        String cashier=data(call("POST","/api/auth/login",null,null,Map.of("phone","13800000001","password","123456"))).path("token").asText();
        assertThat(call("PUT","/api/staff/2/wxpusher",cashier,1L,Map.of("uid","UID_hijack")).path("code").asInt()).isEqualTo(403);
        long other=branch(admin);
        assertThat(call("PUT","/api/staff/2/wxpusher",admin,other,Map.of("uid","UID_wrongstore")).path("code").asInt()).isEqualTo(403);
        assertThat(noticeRecipients.recipients(other)).isEmpty();
        data(call("PUT","/api/staff/2/status?status=0",admin,1L,null));
        assertThat(noticeRecipients.recipients(1L)).isEmpty();
        data(call("PUT","/api/staff/2/status?status=1",admin,1L,null));
        assertThat(noticeRecipients.recipients(1L)).containsExactly("UID_cashier");
        data(call("PUT","/api/staff/2/wxpusher",admin,1L,Map.of("uid","")));
        assertThat(noticeRecipients.recipients(1L)).isEmpty();
    }

    @Test void wxpusherRecipientsRequireCurrentMembershipAndDeduplicateUid() throws Exception {
        String admin=login(); long other=branch(admin);
        jdbc.update("INSERT INTO t_staff_wxpusher(store_id,staff_id,uid) VALUES(?,2,'UID_cashier')",other);
        assertThat(noticeRecipients.recipients(other)).isEmpty();
        jdbc.update("INSERT INTO t_staff_store(staff_id,store_id) VALUES(2,?)",other);
        assertThat(noticeRecipients.recipients(other)).containsExactly("UID_cashier");
        jdbc.update("DELETE FROM t_staff_store WHERE staff_id=2 AND store_id=?",other);
        assertThat(noticeRecipients.recipients(other)).isEmpty();
        jdbc.update("INSERT INTO t_staff_wxpusher(store_id,staff_id,uid) VALUES(1,1,'UID_shared'),(1,2,'UID_shared')");
        assertThat(noticeRecipients.recipients(1L)).containsExactly("UID_shared");
    }
    JsonNode call(String method,String path,String token,Long store,Object body) throws Exception {
        var request=MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method),path).contentType(MediaType.APPLICATION_JSON);
        if(token!=null)request.header("Authorization","Bearer "+token);
        if(store!=null)request.header("X-Store-Id",store);
        if(body!=null)request.content(json.writeValueAsBytes(body));
        return json.readTree(mvc.perform(request).andReturn().getResponse().getContentAsByteArray());
    }
    JsonNode data(JsonNode response){assertThat(response.path("code").asInt()).as(response.toString()).isEqualTo(200);return response.path("data");}
    String login() throws Exception {return data(call("POST","/api/auth/login",null,null,Map.of("phone","13800000000","password","admin123"))).path("token").asText();}
    long branch(String token) throws Exception {return data(call("POST","/api/store",token,null,Map.of("name","营业校验测试店","status",1,"openTime","09:00","closeTime","22:00"))).asLong();}

    @Test void pausedStoreBlocksEveryAdminBusinessRouteButAllowsSwitchingAndReopening() throws Exception {
        String token=login();long branch=branch(token);
        data(call("PUT","/api/store/"+branch+"/status",token,branch,Map.of("status",0)));
        var before=jdbc.queryForList("SELECT * FROM t_member_account ORDER BY id");
        String[][] requests={
            {"GET","/api/rooms"},{"POST","/api/rooms"},{"DELETE","/api/rooms/1"},
            {"GET","/api/products"},{"POST","/api/products"},{"PUT","/api/products/1/status?status=1"},
            {"GET","/api/reservations"},{"POST","/api/reservations"},{"POST","/api/reservations/1/verify"},
            {"POST","/api/charge/resolve?payCode=test"},{"POST","/api/charge/confirm"},{"POST","/api/account/recharge"},
            {"GET","/api/staff"},{"POST","/api/staff"},{"GET","/api/staff/1/wechat-code"},
            {"GET","/api/members"},{"PUT","/api/members/1"},{"GET","/api/dashboard"},{"GET","/api/reports"},
            {"GET","/api/staff/transactions"},{"GET","/api/transactions"},{"GET","/api/audit-logs"},{"GET","/api/banners"},{"POST","/api/banners"},
            {"GET","/api/roles"},{"GET","/api/permissions"},{"GET","/api/store/"+branch},{"PUT","/api/store/"+branch}
        };
        for(var route:requests){
            var response=call(route[0],route[1],token,branch,Map.of());
            assertThat(response.path("code").asInt()).as(route[0]+" "+route[1]+response).isEqualTo(400);
            assertThat(response.path("message").asText()).contains("门店已暂停营业或已删除");
        }
        assertThat(jdbc.queryForList("SELECT * FROM t_member_account ORDER BY id")).isEqualTo(before);
        data(call("GET","/api/store",token,null,null));
        data(call("GET","/api/auth/me",token,branch,null));
        data(call("GET","/api/rooms",token,1L,null));
        data(call("PUT","/api/store/"+branch+"/status",token,branch,Map.of("status",1)));
        data(call("GET","/api/rooms",token,branch,null));
    }

    @Test void omittedHeaderAndDeletedHomeStoreCannotBypassGuard() throws Exception {
        String token=login();
        jdbc.update("UPDATE t_store SET status=0 WHERE id=1");
        assertThat(call("GET","/api/rooms",token,null,null).path("code").asInt()).isEqualTo(400);
        jdbc.update("UPDATE t_store SET status=1,deleted=1 WHERE id=1");
        assertThat(call("POST","/api/account/recharge",token,null,Map.of()).path("code").asInt()).isEqualTo(400);
        assertThat(call("GET","/api/rooms",token,1L,null).path("code").asInt()).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_member WHERE deleted=0",Integer.class)).isPositive();
    }

    @Test void customerBookingChecksRoomsActualStoreDespiteForgedStoreIdAndKeepsCustomerHistory() throws Exception {
        String admin=login();long branch=branch(admin);
        long room=data(call("POST","/api/rooms",admin,branch,Map.of("name","测试包间","priceHour",100,"status",1))).asLong();
        var customer=data(call("POST","/api/customer/auth/register",null,null,Map.of("username","store_guard_customer","password","customer123","name","测试顾客")));
        String token=customer.path("token").asText();long member=customer.path("memberId").asLong();
        var booking=Map.of("roomId",room,"storeId",1,"reserveDate",BookingRules.today().plusDays(1).toString(),"startTime","14:00","hours",2,"contactName","测试顾客","contactPhone","13900001234");
        jdbc.update("UPDATE t_store SET status=0 WHERE id=?",branch);
        assertThat(call("POST","/api/customer/reserve",token,null,booking).path("message").asText()).contains("门店已暂停营业或已删除");
        jdbc.update("UPDATE t_store SET status=1,deleted=1 WHERE id=?",branch);
        assertThat(call("POST","/api/customer/reserve",token,null,booking).path("message").asText()).contains("门店已暂停营业或已删除");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_reservation WHERE room_id=?",Integer.class,room)).isZero();
        data(call("GET","/api/customer/"+member,token,null,null));
        data(call("GET","/api/customer/"+member+"/reservations",token,null,null));
    }
}
