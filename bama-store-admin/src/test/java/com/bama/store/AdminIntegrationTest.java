package com.bama.store;

import com.bama.store.config.AdminSchemaUpgrade;
import com.bama.store.service.BookingRules;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"logging.level.root=WARN", "logging.level.com.bama.store=WARN", "mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"})
@ActiveProfiles("h2")
@AutoConfigureMockMvc(print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
class AdminIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AdminSchemaUpgrade upgrade;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    private String admin;

    @BeforeEach void login() throws Exception {
        admin = data(call("POST", "/api/auth/login", null, Map.of("phone", "13800000000", "password", "admin123"))).path("token").asText();
    }
    private String unique() { return "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16); }

    @Test void branchLifecycleRequiresHeadOfficeAndUpdatesPublicVisibility() throws Exception {
        long id=data(call("POST","/api/store",admin,Map.of("name",unique(),"status",1,"openTime","10:00","closeTime","22:00"))).asLong();
        String manager=data(call("POST","/api/auth/login",null,Map.of("phone","13800000001","password","123456"))).path("token").asText();
        assertThat(call("PUT","/api/store/"+id+"/status",manager,Map.of("status",0)).path("code").asInt()).isEqualTo(403);
        assertThat(call("DELETE","/api/store/"+id,manager,null).path("code").asInt()).isEqualTo(403);
        assertThat(call("DELETE","/api/store/"+id,null,null).path("code").asInt()).isEqualTo(401);
        assertThat(call("DELETE","/api/store/"+id,admin,null).path("message").asText()).contains("先停用");
        assertThat(call("PUT","/api/store/"+id+"/status",admin,Map.of("status",2)).path("code").asInt()).isNotEqualTo(200);
        data(call("PUT","/api/store/"+id+"/status",admin,Map.of("status",0)));
        var adminStores=data(call("GET","/api/store",admin,null));
        assertThat(java.util.stream.StreamSupport.stream(adminStores.spliterator(),false).anyMatch(s->s.path("id").asLong()==id)).isFalse();
        var publicStores=data(call("GET","/api/customer/stores",null,null));
        assertThat(java.util.stream.StreamSupport.stream(publicStores.spliterator(),false).anyMatch(s->s.path("id").asLong()==id)).isFalse();
        data(call("PUT","/api/store/"+id+"/status",admin,Map.of("status",1)));
        assertThat(java.util.stream.StreamSupport.stream(data(call("GET","/api/store",admin,null)).spliterator(),false).anyMatch(s->s.path("id").asLong()==id)).isTrue();
        assertThat(jdbc.queryForObject("SELECT status FROM t_store WHERE id=?",Integer.class,id)).isEqualTo(1);
        data(call("PUT","/api/store/"+id+"/status",admin,Map.of("status",0)));
        data(call("DELETE","/api/store/"+id,admin,null));
        assertThat(jdbc.queryForObject("SELECT deleted FROM t_store WHERE id=?",Integer.class,id)).isEqualTo(1);
        assertThat(call("PUT","/api/store/"+id+"/status",admin,Map.of("status",1)).path("code").asInt()).isNotEqualTo(200);
        assertThat(call("DELETE","/api/store/"+id,admin,null).path("code").asInt()).isNotEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_audit_log WHERE action='删除分店' AND target=?",Integer.class,Long.toString(id))).isEqualTo(1);
    }

    @Test void pausedBranchesWithReferencesCannotBeDeletedEvenAfterRoomSoftDeletion() throws Exception {
        long id=data(call("POST","/api/store",admin,Map.of("name",unique(),"status",0,"openTime","10:00","closeTime","22:00"))).asLong();
        jdbc.update("INSERT INTO t_staff_store(staff_id,store_id) VALUES(1,?)",id);
        assertThat(call("DELETE","/api/store/"+id,admin,null).path("message").asText()).contains("关联");
        jdbc.update("DELETE FROM t_staff_store WHERE store_id=?",id);
        jdbc.update("INSERT INTO t_tea_room(name,store_id,deleted) VALUES(?,?,1)","历史包间",id);
        var balance=jdbc.queryForObject("SELECT SUM(balance) FROM t_member_account",java.math.BigDecimal.class);
        assertThat(call("DELETE","/api/store/"+id,admin,null).path("message").asText()).contains("关联");
        assertThat(jdbc.queryForObject("SELECT deleted FROM t_store WHERE id=?",Integer.class,id)).isZero();
        assertThat(jdbc.queryForObject("SELECT SUM(balance) FROM t_member_account",java.math.BigDecimal.class)).isEqualByComparingTo(balance);
    }

    @Test @org.springframework.transaction.annotation.Transactional
    void lastBranchCannotBeDeleted() throws Exception {
        jdbc.update("UPDATE t_store SET deleted=1 WHERE id<>1");
        jdbc.update("UPDATE t_store SET status=0 WHERE id=1");
        assertThat(call("DELETE","/api/store/1",admin,null).path("message").asText()).contains("最后一家");
        assertThat(jdbc.queryForObject("SELECT deleted FROM t_store WHERE id=1",Integer.class)).isZero();
    }

    @Test void assignedBranchesLimitReadsWritesAndRevocationAppliesToExistingToken() throws Exception {
        long branch = data(call("POST", "/api/store", admin, Map.of("name", unique(), "status", 1, "openTime", "10:00", "closeTime", "22:00"))).asLong();
        long other = data(call("POST", "/api/store", admin, Map.of("name", unique(), "status", 1, "openTime", "10:00", "closeTime", "22:00"))).asLong();
        String phone = "139" + String.format("%08d", Math.abs(UUID.randomUUID().getLeastSignificantBits() % 100000000));
        long id = data(call("POST", "/api/staff", admin, Map.of("name", "跨店店长", "phone", phone, "password", "employee123", "roleIds", List.of(1), "storeIds", List.of(1, branch)))).asLong();
        String token = data(call("POST", "/api/auth/login", null, Map.of("phone", phone, "password", "employee123"))).path("token").asText();
        var visible = data(call("GET", "/api/store", token, null));
        assertThat(visible.size()).isEqualTo(2);
        assertThat(data(branchCall("GET", "/api/store", token, branch, null))).isEqualTo(visible);
        assertThat(data(branchCall("GET", "/api/staff?keyword=" + phone, admin, branch, null)).path("total").asInt()).isEqualTo(1);
        long homeRoom = room();
        long branchRoom = data(branchCall("POST", "/api/rooms", token, branch, Map.of("name", "授权分店茶室", "priceHour", 100, "status", 1))).asLong();
        var rooms = data(branchCall("GET", "/api/rooms", token, branch, null));
        assertThat(rooms.size()).isEqualTo(1);
        assertThat(rooms.get(0).path("id").asLong()).isEqualTo(branchRoom);
        assertThat(branchCall("DELETE", "/api/rooms/" + homeRoom, token, branch, null).path("code").asInt()).isEqualTo(403);
        for (String path : List.of("/api/rooms", "/api/products", "/api/reservations", "/api/transactions", "/api/reports", "/api/staff"))
            assertThat(branchCall("GET", path, token, other, null).path("code").asInt()).isEqualTo(403);
        assertThat(branchCall("POST", "/api/rooms", token, other, Map.of("name", "越权写入", "priceHour", 100)).path("code").asInt()).isEqualTo(403);
        // Even a store manager cannot grant a branch outside their own scope.
        assertThat(call("PUT", "/api/staff/" + id, token, Map.of("name", "跨店店长", "phone", phone, "roleIds", List.of(1), "storeIds", List.of(1, other))).path("code").asInt()).isNotEqualTo(200);
        assertThat(call("PUT", "/api/staff/" + id, admin, Map.of("name", "跨店店长", "phone", phone, "roleIds", List.of(1), "storeIds", List.of())).path("code").asInt()).isNotEqualTo(200);
        assertThat(call("PUT", "/api/staff/" + id, admin, Map.of("name", "跨店店长", "phone", phone, "roleIds", List.of(1), "storeIds", List.of(branch))).path("code").asInt()).isNotEqualTo(200);
        // Profile-only clients preserve grants; repeat upgrades must also preserve grants.
        data(call("PUT", "/api/staff/" + id, admin, Map.of("name", "跨店店长", "phone", phone, "roleIds", List.of(1))));
        upgrade.run(null);
        data(branchCall("GET", "/api/rooms", token, branch, null));
        data(call("PUT", "/api/staff/" + id, admin, Map.of("name", "跨店店长", "phone", phone, "roleIds", List.of(1), "storeIds", List.of(1))));
        assertThat(branchCall("GET", "/api/rooms", token, branch, null).path("code").asInt()).isEqualTo(403);
        assertThat(data(call("GET", "/api/store", token, null)).size()).isEqualTo(1);
        assertThat(data(branchCall("GET", "/api/staff?keyword=" + phone, admin, branch, null)).path("total").asInt()).isZero();
    }
    @Test void bannersSupportImagesVisibilityAndStoreIsolation() throws Exception {
        var pixels=new java.awt.image.BufferedImage(200,100,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var png=new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(pixels,"png",png);
        String image="data:image/png;base64,"+Base64.getEncoder().encodeToString(png.toByteArray());
        String title=unique();
        var body=new HashMap<String,Object>(Map.of("title",title,"image",image,"sortOrder",2,"status",1,"target","rooms"));
        data(call("POST","/api/banners",admin,body));
        var list=data(call("GET","/api/banners",admin,null));
        JsonNode row=null;
        for(var entry:list)if(entry.path("title").asText().equals(title))row=entry;
        assertThat(row).isNotNull();
        long id=row.path("id").asLong();
        assertThat(data(call("GET","/api/customer/banners?storeId=1",null,null)).toString()).contains(title);
        var response=mvc.perform(MockMvcRequestBuilders.get(row.path("imageUrl").asText())).andReturn().getResponse();
        assertThat(response.getContentType()).isEqualTo("image/png");
        assertThat(javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(response.getContentAsByteArray())).getWidth()).isEqualTo(200);
        long branch=data(call("POST","/api/store",admin,Map.of("name",unique(),"status",1,"openTime","10:00","closeTime","22:00"))).asLong();
        assertThat(data(call("GET","/api/customer/banners?storeId="+branch,null,null)).toString()).doesNotContain(title);
        body.put("id",id);
        assertThat(branchCall("POST","/api/banners",admin,branch,body).path("code").asInt()).isEqualTo(403);
        assertThat(branchCall("DELETE","/api/banners/"+id,admin,branch,null).path("code").asInt()).isEqualTo(403);
        body.remove("image"); body.put("status",0);
        data(call("POST","/api/banners",admin,body));
        assertThat(data(call("GET","/api/customer/banners?storeId=1",null,null)).toString()).doesNotContain(title);
        body.put("image","data:image/png;base64,ZmFrZQ==");
        assertThat(call("POST","/api/banners",admin,body).path("code").asInt()).isNotEqualTo(200);
        assertThat(call("POST","/api/banners",null,body).path("code").asInt()).isEqualTo(401);
        String customer=register(unique()).path("token").asText();
        assertThat(call("GET","/api/banners",customer,null).path("code").asInt()).isEqualTo(403);
        data(call("DELETE","/api/banners/"+id,admin,null));
        assertThat(mvc.perform(MockMvcRequestBuilders.get("/api/banner-images/"+id)).andReturn().getResponse().getStatus()).isEqualTo(404);
    }
    private JsonNode call(String method, String path, String token, Object body) throws Exception {
        var request = MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method), path).contentType(MediaType.APPLICATION_JSON);
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null) request.content(json.writeValueAsBytes(body));
        return json.readTree(mvc.perform(request).andReturn().getResponse().getContentAsByteArray());
    }
    private JsonNode data(JsonNode result) {
        assertThat(result.path("code").asInt()).as(result.toString()).isEqualTo(200); return result.path("data");
    }

    private JsonNode branchCall(String method, String path, String token, long storeId, Object body) throws Exception {
        var request = MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method), path)
                .contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token).header("X-Store-Id", storeId);
        if (body != null) request.content(json.writeValueAsBytes(body));
        return json.readTree(mvc.perform(request).andReturn().getResponse().getContentAsByteArray());
    }

    @Test void branchesHaveIndependentRoomsBookingsAndStaffPermissions() throws Exception {
        String name = unique();
        var store = Map.of("name", name, "status", 1, "openTime", "10:00", "closeTime", "22:00");
        long branchId = data(call("POST", "/api/store", admin, store)).asLong();
        long firstRoom = room();
        long secondRoom = data(branchCall("POST", "/api/rooms", admin, branchId, Map.of("name", "分店独立包间", "priceHour", 180, "status", 1))).asLong();
        JsonNode rooms = data(branchCall("GET", "/api/rooms", admin, branchId, null));
        assertThat(rooms.size()).isEqualTo(1);
        assertThat(rooms.get(0).path("id").asLong()).isEqualTo(secondRoom);
        JsonNode publicRooms = data(call("GET", "/api/customer/rooms?storeId=" + branchId, null, null));
        assertThat(publicRooms.size()).isEqualTo(1);
        assertThat(publicRooms.get(0).path("id").asLong()).isEqualTo(secondRoom);
        assertThat(branchCall("DELETE", "/api/rooms/" + firstRoom, admin, branchId, null).path("code").asInt()).isEqualTo(403);
        LocalDate day = BookingRules.today().plusDays(1);
        data(call("POST", "/api/reservations", admin, booking(firstRoom, 1, day, "14:00")));
        data(branchCall("POST", "/api/reservations", admin, branchId, booking(secondRoom, 1, day, "14:00")));
        assertThat(data(branchCall("GET", "/api/reservations", admin, branchId, null)).path("total").asInt()).isEqualTo(1);
        String phone = "139" + String.format("%08d", Math.abs(UUID.randomUUID().getLeastSignificantBits() % 100000000));
        data(branchCall("POST", "/api/staff", admin, branchId, Map.of("name", "分店店长", "phone", phone, "password", "employee123", "roleIds", List.of(1))));
        String manager = data(call("POST", "/api/auth/login", null, Map.of("phone", phone, "password", "employee123"))).path("token").asText();
        assertThat(data(call("GET", "/api/store", manager, null)).size()).isEqualTo(1);
        assertThat(data(call("GET", "/api/rooms", manager, null)).size()).isEqualTo(1);
        assertThat(branchCall("GET", "/api/rooms", manager, 1, null).path("code").asInt()).isEqualTo(403);
        assertThat(call("PUT", "/api/store/1", manager, store).path("code").asInt()).isEqualTo(403);
        assertThat(call("POST", "/api/store", manager, store).path("code").asInt()).isEqualTo(403);
        JsonNode customer = register(unique());
        String customerToken = customer.path("token").asText();
        data(call("POST", "/api/customer/reserve", customerToken, booking(secondRoom, 1, day, "17:00")));
        JsonNode history = data(call("GET", "/api/customer/" + customer.path("memberId").asLong() + "/reservations", customerToken, null));
        assertThat(history.get(0).path("storeName").asText()).isEqualTo(name);
        assertThat(history.get(0).path("storeId").asLong()).isEqualTo(branchId);
        var paused = new HashMap<String,Object>(store); paused.put("status", 0);
        var memberBefore = jdbc.queryForMap("SELECT * FROM t_member WHERE id=?", customer.path("memberId").asLong());
        var accountBefore = jdbc.queryForList("SELECT * FROM t_member_account WHERE member_id=?", customer.path("memberId").asLong());
        data(branchCall("PUT", "/api/store/" + branchId, admin, branchId, paused));
        assertThat(data(call("GET", "/api/customer/stores", null, null)).toString()).doesNotContain(name);
        assertThat(call("GET", "/api/customer/rooms?storeId=" + branchId, null, null).path("code").asInt()).isNotEqualTo(200);
        assertThat(call("POST", "/api/customer/reserve", customerToken, booking(secondRoom, 1, day, "19:00")).path("code").asInt()).isNotEqualTo(200);
        var slots = data(call("GET", "/api/customer/rooms/" + secondRoom + "/slots?date=" + day, null, null));
        assertThat(slots).isNotEmpty();
        for (JsonNode slot : slots) assertThat(slot.path("available").asBoolean()).isFalse();
        assertThat(jdbc.queryForMap("SELECT * FROM t_member WHERE id=?", customer.path("memberId").asLong())).isEqualTo(memberBefore);
        assertThat(jdbc.queryForList("SELECT * FROM t_member_account WHERE member_id=?", customer.path("memberId").asLong())).isEqualTo(accountBefore);
        assertThat(data(call("GET", "/api/customer/" + customer.path("memberId").asLong() + "/reservations", customerToken, null))).isEqualTo(history);
        data(call("POST", "/api/customer/reserve", customerToken, booking(firstRoom, 1, day, "19:00")));
    }

    @Test void branchManagerCannotResetHeadquartersPasswordOrGrantHeadquartersRole() throws Exception {
        String phone = "139" + String.format("%08d", Math.abs(UUID.randomUUID().getLeastSignificantBits() % 100000000));
        data(call("POST", "/api/staff", admin, Map.of("name", "普通店长", "phone", phone, "password", "employee123", "roleIds", List.of(1))));
        String manager = data(call("POST", "/api/auth/login", null, Map.of("phone", phone, "password", "employee123"))).path("token").asText();
        long adminId = data(call("GET", "/api/auth/me", admin, null)).path("staffId").asLong();
        assertThat(call("PUT", "/api/staff/" + adminId + "/password", manager, Map.of("password", "hijack1234")).path("code").asInt()).isEqualTo(403);
        assertThat(call("PUT", "/api/staff/" + adminId + "/status?status=0", manager, null).path("code").asInt()).isEqualTo(403);
        long hqRole = 0;
        for (JsonNode role : data(call("GET", "/api/roles", admin, null))) if ("HEADQUARTERS".equals(role.path("code").asText())) hqRole=role.path("id").asLong();
        String anotherPhone = "139" + String.format("%08d", Math.abs(UUID.randomUUID().getLeastSignificantBits() % 100000000));
        assertThat(call("POST", "/api/staff", manager, Map.of("name", "越权账号", "phone", anotherPhone, "password", "employee123", "roleIds", List.of(hqRole))).path("code").asInt()).isNotEqualTo(200);
    }
    private JsonNode register(String username) throws Exception {
        return data(call("POST", "/api/customer/auth/register", null, Map.of("username", username, "password", "customer123", "name", "测试顾客")));
    }
    private long room() throws Exception {
        return data(call("POST", "/api/rooms", admin, Map.of("name", unique(), "priceHour", 100, "openTime", "09:00", "closeTime", "22:00", "minHours", 1, "advanceDays", 30, "status", 1))).asLong();
    }
    private Map<String,Object> booking(long roomId, long memberId, LocalDate date, String time) {
        return new HashMap<>(Map.of("roomId", roomId, "memberId", memberId, "reserveDate", date.toString(), "startTime", time, "hours", 2, "contactPhone", "13900001111"));
    }
    private long findBooking(String orderNo) throws Exception {
        return data(call("GET", "/api/reservations?keyword=" + orderNo, admin, null)).path("records").get(0).path("id").asLong();
    }

    @Test void registrationCreatesOneAccountAndCustomerTokensCannotImpersonateOthers() throws Exception {
        String username = unique(); JsonNode first = register(username); long id = first.path("memberId").asLong(); String token = first.path("token").asText();
        JsonNode login = data(call("POST", "/api/customer/auth/login", null, Map.of("username", username.toUpperCase(Locale.ROOT), "password", "customer123")));
        assertThat(login.path("memberId").asLong()).isEqualTo(id);
        assertThat(call("POST", "/api/customer/auth/register", null, Map.of("username", username, "password", "customer123", "name", "重复")).path("code").asInt()).isNotEqualTo(200);
        JsonNode list = data(call("GET", "/api/members?keyword=" + username, admin, null));
        assertThat(list.path("total").asInt()).isEqualTo(1);
        assertThat(list.toString()).doesNotContain("password", "$2a$");
        assertThat(list.path("records").get(0).path("lastLoginTime").asText()).isNotBlank();
        assertThat(data(call("GET", "/api/members/" + id + "/account", admin, null)).path("balance").decimalValue()).isZero();
        assertThat(call("GET", "/api/customer/1", token, null).path("code").asInt()).isEqualTo(403);
        assertThat(call("GET", "/api/customer/" + id, null, null).path("code").asInt()).isEqualTo(401);
        assertThat(call("GET", "/api/staff", token, null).path("code").asInt()).isEqualTo(403);
        assertThat(call("POST", "/api/paycode/generate?memberId=1", token, null).path("code").asInt()).isEqualTo(403);
        data(call("PUT", "/api/members/" + id, admin, Map.of("name", "测试顾客", "remark", "停用测试", "status", 0)));
        assertThat(call("GET", "/api/customer/" + id, token, null).path("code").asInt()).isNotEqualTo(200);
    }

    @Test void intervalOverlapCancellationAndPriceSnapshot() throws Exception {
        long roomId = room(); LocalDate day = BookingRules.today().plusDays(1);
        String order = data(call("POST", "/api/reservations", admin, booking(roomId, 1, day, "14:00"))).asText(); long id = findBooking(order);
        assertThat(call("POST", "/api/reservations", admin, booking(roomId, 1, day, "15:00")).path("code").asInt()).isNotEqualTo(200);
        data(call("POST", "/api/reservations", admin, booking(roomId, 1, day, "16:00")));
        data(call("POST", "/api/reservations/" + id + "/cancel", admin, Map.of("reason", "测试取消")));
        String again = data(call("POST", "/api/reservations", admin, booking(roomId, 1, day, "14:00"))).asText(); long againId = findBooking(again);
        data(call("POST", "/api/rooms", admin, Map.of("id", roomId, "name", "新房间名称", "priceHour", 300, "openTime", "09:00", "closeTime", "22:00")));
        data(call("PUT", "/api/reservations/" + againId + "/schedule", admin, Map.of("date", day.plusDays(1).toString(), "startTime", "14:00")));
        JsonNode updated = data(call("GET", "/api/reservations?keyword=" + again, admin, null)).path("records").get(0);
        assertThat(updated.path("amount").decimalValue()).isEqualByComparingTo("200");
        assertThat(updated.path("roomName").asText()).isNotEqualTo("新房间名称");
        assertThat(call("DELETE", "/api/rooms/" + roomId, admin, null).path("code").asInt()).isNotEqualTo(200);
    }

    @Test void parallelOverlappingBookingsOnlyOneSucceeds() throws Exception {
        long roomId = room(); LocalDate day = BookingRules.today().plusDays(2);
        ExecutorService pool = Executors.newFixedThreadPool(2); CountDownLatch ready = new CountDownLatch(2), go = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (String time : List.of("14:00", "15:00")) futures.add(pool.submit(() -> { ready.countDown(); go.await(); return call("POST", "/api/reservations", admin, booking(roomId, 1, day, time)).path("code").asInt(); }));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue(); go.countDown();
            List<Integer> results = new ArrayList<>(); for (var f : futures) results.add(f.get(15, TimeUnit.SECONDS));
            assertThat(results.stream().filter(c -> c == 200).count()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    @Test void closuresDisabledRoomsAndBadDurationsAreRejected() throws Exception {
        long roomId = room(); LocalDate day = BookingRules.today().plusDays(1);
        data(call("POST", "/api/rooms/" + roomId + "/closures", admin, Map.of("closureDate", day.toString(), "startTime", "14:00", "endTime", "16:00", "reason", "清洁维护")));
        assertThat(call("POST", "/api/reservations", admin, booking(roomId, 1, day, "15:00")).path("code").asInt()).isNotEqualTo(200);
        JsonNode slots = data(call("GET", "/api/customer/rooms/" + roomId + "/slots?date=" + day + "&hours=2", null, null));
        for (JsonNode slot : slots) if (slot.path("time").asText().equals("13:00")) assertThat(slot.path("available").asBoolean()).isFalse();
        long closureId = data(call("GET", "/api/rooms/" + roomId + "/closures", admin, null)).get(0).path("id").asLong();
        data(call("DELETE", "/api/rooms/" + roomId + "/closures/" + closureId, admin, null));
        var invalid = booking(roomId, 1, day, "15:00"); invalid.put("hours", -1);
        assertThat(call("POST", "/api/reservations", admin, invalid).path("code").asInt()).isNotEqualTo(200);
        data(call("POST", "/api/rooms", admin, Map.of("id", roomId, "name", "暂停房间", "priceHour", 100, "status", 0)));
        assertThat(call("POST", "/api/reservations", admin, booking(roomId, 1, day, "15:00")).path("code").asInt()).isNotEqualTo(200);
    }

    @Test void staffEditingAndDisablingApplyToExistingToken() throws Exception {
        String phone = "139" + String.format("%08d", Math.abs(UUID.randomUUID().getLeastSignificantBits() % 100000000));
        long id = data(call("POST", "/api/staff", admin, Map.of("name", "新员工", "phone", phone, "password", "employee123", "roleIds", List.of(2), "storeId", 999))).asLong();
        String token = data(call("POST", "/api/auth/login", null, Map.of("phone", phone, "password", "employee123"))).path("token").asText();
        data(call("PUT", "/api/staff/" + id, admin, Map.of("name", "调整后员工", "phone", phone, "roleIds", List.of(3))));
        assertThat(call("GET", "/api/members", token, null).path("code").asInt()).isEqualTo(403);
        JsonNode row = data(call("GET", "/api/staff?keyword=" + phone, admin, null)).path("records").get(0);
        assertThat(row.path("storeId").asInt()).isEqualTo(1);
        assertThat(row.path("roleNames").toString()).contains("茶艺师");
        assertThat(row.toString()).doesNotContain("password", "$2a$");
        data(call("PUT", "/api/staff/" + id + "/status?status=0", admin, null));
        assertThat(call("GET", "/api/reservations", token, null).path("code").asInt()).isEqualTo(401);
    }

    @Test void employeeRecordsIncludeRechargeGiftAndConsumptionAndEnforceScope() throws Exception {
        long id=register(unique()).path("memberId").asLong();
        String phone="136"+String.format("%08d",Math.abs(UUID.randomUUID().getLeastSignificantBits()%100000000));
        jdbc.update("UPDATE t_member SET phone=? WHERE id=?",phone,id);
        var cashierSession=data(call("POST","/api/auth/login",null,Map.of("phone","13800000001","password","123456")));
        String cashier=cashierSession.path("token").asText();
        long operator=cashierSession.path("staffId").asLong();
        data(call("POST","/api/account/recharge",cashier,Map.of("memberId",id,"amount",100,"giftAmount",20)));
        data(call("POST","/api/charge/confirm",cashier,Map.of("memberId",id,"amount",12.34,"bizNo",unique())));
        data(call("POST","/api/account/recharge",admin,Map.of("memberId",id,"amount",5)));
        var mine=data(call("GET","/api/staff/transactions?keyword="+phone,cashier,null));
        assertThat(mine.path("total").asInt()).isEqualTo(3);
        var types=new java.util.HashSet<String>();
        for(var row:mine.path("records")) {
            types.add(row.path("type").asText());
            assertThat(row.path("staffId").asLong()).isEqualTo(operator);
            assertThat(row.path("memberPhone").asText()).isEqualTo(phone);
            assertThat(row.path("staffName").asText()).isNotBlank();
            assertThat(row.path("storeName").asText()).isNotBlank();
            assertThat(row.path("createTime").asText()).isNotBlank();
            assertThat(row.path("bizNo").asText()).isNotBlank();
        }
        assertThat(types).containsExactlyInAnyOrder("RECHARGE","GIFT","CONSUME");
        assertThat(data(call("GET","/api/staff/transactions?keyword="+phone+"&type=CONSUME",cashier,null)).path("total").asInt()).isEqualTo(1);
        assertThat(call("GET","/api/staff/transactions?scope=store",cashier,null).path("code").asInt()).isEqualTo(403);
        assertThat(call("GET","/api/staff/transactions",null,null).path("code").asInt()).isEqualTo(401);
        assertThat(data(call("GET","/api/staff/transactions?keyword="+phone+"&scope=store",admin,null)).path("total").asInt()).isEqualTo(4);
        long other=data(call("POST","/api/store",admin,Map.of("name",unique(),"status",1,"openTime","10:00","closeTime","22:00"))).asLong();
        assertThat(data(branchCall("GET","/api/staff/transactions?scope=store&keyword="+phone,admin,other,null)).path("total").asInt()).isZero();
        assertThat(call("GET","/api/staff/transactions?startDate=2026-09-09&endDate=2026-09-08",cashier,null).path("code").asInt()).isNotEqualTo(200);
    }

    @Test void phonePaymentUsesExactAmountAndRetriesWithoutDoubleDebit() throws Exception {
        long id=register(unique()).path("memberId").asLong();
        String phone="137"+String.format("%08d",Math.abs(UUID.randomUUID().getLeastSignificantBits()%100000000));
        jdbc.update("UPDATE t_member SET phone=?,discount=80 WHERE id=?",phone,id);
        data(call("POST","/api/account/recharge",admin,Map.of("memberId",id,"amount",100)));
        assertThat(data(call("GET","/api/charge/member?phone="+phone,admin,null)).path("memberId").asLong()).isEqualTo(id);
        assertThat(call("GET","/api/charge/member?phone="+phone,null,null).path("code").asInt()).isEqualTo(401);
        assertThat(call("GET","/api/charge/member?phone=137",admin,null).path("code").asInt()).isNotEqualTo(200);
        var body=Map.of("memberId",id,"expectedPhone",phone,"amount",12.34,"bizNo",unique());
        var first=data(call("POST","/api/charge/confirm",admin,body));
        var retry=data(call("POST","/api/charge/confirm",admin,body));
        assertThat(first.path("payAmount").decimalValue()).isEqualByComparingTo("12.34");
        assertThat(retry.path("orderNo").asText()).isEqualTo(first.path("orderNo").asText());
        assertThat(data(call("GET","/api/members/"+id+"/account",admin,null)).path("balance").decimalValue()).isEqualByComparingTo("87.66");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_consume_order WHERE member_id=?",Integer.class,id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_wallet_txn WHERE member_id=? AND type='CONSUME'",Integer.class,id)).isEqualTo(1);
        var detail=data(call("POST","/api/charge/confirm",admin,Map.of("memberId",id,"bizNo",unique(),"items",List.of(Map.of("itemType","PRODUCT","itemName","茶叶","price",10,"quantity",1)))));
        assertThat(detail.path("payAmount").decimalValue()).isEqualByComparingTo("8.00");
    }

    @Test void manualPaymentRejectsInvalidAmountsAndChangedPhoneWithoutDebiting() throws Exception {
        long id=register(unique()).path("memberId").asLong();
        data(call("POST","/api/account/recharge",admin,Map.of("memberId",id,"amount",100)));
        for(Object amount:List.of(-1,0,1.001,100000000,101)) {
            assertThat(call("POST","/api/charge/confirm",admin,Map.of("memberId",id,"amount",amount,"bizNo",unique())).path("code").asInt()).isNotEqualTo(200);
        }
        assertThat(call("POST","/api/charge/confirm",admin,Map.of("memberId",id,"amount",10)).path("code").asInt()).isNotEqualTo(200);
        assertThat(call("POST","/api/charge/confirm",admin,Map.of("memberId",id,"amount",10,"expectedPhone","wrong","bizNo",unique())).path("code").asInt()).isNotEqualTo(200);
        assertThat(call("POST","/api/charge/confirm",admin,Map.of("memberId",id,"amount",10,"bizNo",unique(),"items",List.of(Map.of("itemType","PRODUCT","itemName","茶","price",10,"quantity",1)))).path("code").asInt()).isNotEqualTo(200);
        assertThat(data(call("GET","/api/members/"+id+"/account",admin,null)).path("balance").decimalValue()).isEqualByComparingTo("100");
        jdbc.update("UPDATE t_member SET status=0 WHERE id=?",id);
        assertThat(call("POST","/api/charge/confirm",admin,Map.of("memberId",id,"amount",10,"bizNo",unique())).path("code").asInt()).isNotEqualTo(200);
    }

    @Test void rechargeIsSeparatedFromGiftAndConsumptionAndAudited() throws Exception {
        long id = register(unique()).path("memberId").asLong();
        String today = BookingRules.today().toString();
        JsonNode before = data(call("GET", "/api/reports?startDate=" + today + "&endDate=" + today, admin, null));
        data(call("POST", "/api/account/recharge", admin, Map.of("memberId", id, "amount", 100, "giftAmount", 20, "remark", "测试充值")));
        JsonNode report = data(call("GET", "/api/reports?startDate=" + today + "&endDate=" + today, admin, null));
        assertThat(report.path("rechargeAmount").decimalValue().subtract(before.path("rechargeAmount").decimalValue())).isEqualByComparingTo("100");
        assertThat(report.path("giftAmount").decimalValue().subtract(before.path("giftAmount").decimalValue())).isEqualByComparingTo("20");
        assertThat(report.path("consumeAmount").decimalValue()).isEqualByComparingTo(before.path("consumeAmount").decimalValue());
        assertThat(data(call("GET", "/api/members/" + id + "/account", admin, null)).path("balance").decimalValue()).isEqualByComparingTo("120");
        assertThat(data(call("GET", "/api/transactions?memberId=" + id, admin, null)).path("total").asInt()).isEqualTo(2);
        assertThat(data(call("GET", "/api/audit-logs", admin, null)).path("records").toString()).contains("代客充值").doesNotContain("customer123", "admin123");
        assertThat(call("POST", "/api/account/recharge", admin, Map.of("memberId", id, "amount", 100, "giftAmount", -20)).path("code").asInt()).isNotEqualTo(200);
        assertThat(call("GET", "/api/reports?startDate=2020-01-01&endDate=2026-01-01", admin, null).path("code").asInt()).isNotEqualTo(200);
    }

    @Test void upgradeCanBeRepeatedWithoutLosingData() throws Exception {
        String username = unique(); register(username); upgrade.run(null);
        assertThat(data(call("GET", "/api/members?keyword=" + username, admin, null)).path("total").asInt()).isEqualTo(1);
    }

    @Test void verificationStateMachineAndCustomerOwnership() throws Exception {
        long roomId = room(); JsonNode customer = register(unique());
        String token = customer.path("token").asText(); long memberId = customer.path("memberId").asLong();
        String order = data(call("POST", "/api/customer/reserve", token, booking(roomId, 1, BookingRules.today().plusDays(1), "15:00"))).asText();
        long id = findBooking(order);
        JsonNode row = data(call("GET", "/api/reservations?keyword=" + order, admin, null)).path("records").get(0);
        assertThat(row.path("memberId").asLong()).isEqualTo(memberId);
        assertThat(call("POST", "/api/reservations/" + id + "/verify", admin, null).path("code").asInt()).isNotEqualTo(200);
        assertThat(call("POST", "/api/reservations/" + id + "/complete", admin, null).path("code").asInt()).isNotEqualTo(200);
        String other = register(unique()).path("token").asText();
        assertThat(call("POST", "/api/customer/reservations/" + id + "/cancel", other, Map.of("reason", "越权取消")).path("code").asInt()).isEqualTo(403);
        data(call("POST", "/api/customer/reservations/" + id + "/cancel", token, Map.of("reason", "顾客取消")));
        assertThat(call("POST", "/api/reservations/" + id + "/verify", admin, null).path("code").asInt()).isNotEqualTo(200);
        assertThat(data(call("GET", "/api/audit-logs?keyword=" + order, admin, null)).path("total").asInt()).isEqualTo(2);
    }

    @Test void customerApplicationsRequireStaffApprovalAndRejectionReleasesSlot() throws Exception {
        long roomId=room(); var customer=register(unique()); String token=customer.path("token").asText();
        var day=BookingRules.today().plusDays(1);
        var input=new HashMap<String,Object>(booking(roomId,1,day,"15:00"));
        input.put("status","WAITING"); input.put("source","STAFF");
        String order=data(call("POST","/api/customer/reserve",token,input)).asText(); long id=findBooking(order);
        var row=data(call("GET","/api/reservations?keyword="+order,admin,null)).path("records").get(0);
        assertThat(row.path("status").asText()).isEqualTo("PENDING");
        assertThat(row.path("source").asText()).isEqualTo("CUSTOMER");
        assertThat(call("POST","/api/customer/reserve",token,input).path("code").asInt()).isNotEqualTo(200);
        assertThat(call("POST","/api/reservations/"+id+"/verify",admin,null).path("code").asInt()).isNotEqualTo(200);
        assertThat(call("POST","/api/reservations/"+id+"/confirm",token,null).path("code").asInt()).isEqualTo(403);
        long branch=data(call("POST","/api/store",admin,Map.of("name",unique(),"status",1,"openTime","10:00","closeTime","22:00"))).asLong();
        assertThat(branchCall("POST","/api/reservations/"+id+"/confirm",admin,branch,null).path("code").asInt()).isEqualTo(403);
        assertThat(call("POST","/api/reservations/"+id+"/reject",admin,Map.of("reason","")).path("code").asInt()).isNotEqualTo(200);
        data(call("POST","/api/reservations/"+id+"/reject",admin,Map.of("reason","暂时无法接待")));
        var mine=data(call("GET","/api/customer/"+customer.path("memberId").asLong()+"/reservations",token,null)).get(0);
        assertThat(mine.path("status").asText()).isEqualTo("REJECTED");
        assertThat(mine.path("cancelReason").asText()).isEqualTo("暂时无法接待");
        assertThat(data(call("GET","/api/customer/rooms/"+roomId+"/reserved?date="+day,null,null)).size()).isZero();
        long second=findBooking(data(call("POST","/api/customer/reserve",token,input)).asText());
        data(call("POST","/api/reservations/"+second+"/confirm",admin,null));
        assertThat(data(call("GET","/api/customer/"+customer.path("memberId").asLong()+"/reservations",token,null)).get(0).path("status").asText()).isEqualTo("WAITING");
        assertThat(call("POST","/api/reservations/"+second+"/confirm",admin,null).path("code").asInt()).isNotEqualTo(200);
        assertThat(call("POST","/api/reservations/"+second+"/reject",admin,Map.of("reason","再次拒绝")).path("code").asInt()).isNotEqualTo(200);
        assertThat(call("POST","/api/customer/reserve",token,input).path("code").asInt()).isNotEqualTo(200);
        data(call("POST","/api/customer/reservations/"+second+"/cancel",token,Map.of("reason","行程变化")));
        long third=findBooking(data(call("POST","/api/customer/reserve",token,input)).asText());
        data(call("POST","/api/customer/reservations/"+third+"/cancel",token,Map.of("reason","撤回申请")));
        assertThat(call("POST","/api/reservations/"+third+"/confirm",admin,null).path("code").asInt()).isNotEqualTo(200);
    }

    @Test void concurrentApprovalAndRejectionOnlyOneWins() throws Exception {
        long roomId=room(); String token=register(unique()).path("token").asText();
        String order=data(call("POST","/api/customer/reserve",token,booking(roomId,1,BookingRules.today().plusDays(1),"15:00"))).asText();long id=findBooking(order);
        var pool=Executors.newFixedThreadPool(2); var gate=new CountDownLatch(1);
        try {
            var approve=pool.submit(()->{gate.await();return call("POST","/api/reservations/"+id+"/confirm",admin,null).path("code").asInt();});
            var reject=pool.submit(()->{gate.await();return call("POST","/api/reservations/"+id+"/reject",admin,Map.of("reason","无法接待")).path("code").asInt();});
            gate.countDown();
            assertThat((approve.get(10,TimeUnit.SECONDS)==200?1:0)+(reject.get(10,TimeUnit.SECONDS)==200?1:0)).isEqualTo(1);
        } finally {pool.shutdownNow();}
    }

    @Test void bookingContactIsPrivateAndReservationKeepsContactSnapshot() throws Exception {
        var customer=register(unique()); String token=customer.path("token").asText(); long memberId=customer.path("memberId").asLong();
        jdbc.update("UPDATE t_member SET phone=? WHERE id=?","13811112222",memberId);
        assertThat(data(call("GET","/api/customer/booking-contact",token,null)).path("phone").asText()).isEqualTo("13811112222");
        assertThat(call("GET","/api/customer/booking-contact",null,null).path("code").asInt()).isEqualTo(401);
        String other=register(unique()).path("token").asText();
        assertThat(data(call("GET","/api/customer/booking-contact?memberId="+memberId,other,null)).path("phone").asText()).isEmpty();
        var input=booking(room(),memberId,BookingRules.today().plusDays(1),"15:00");
        input.put("contactName"," 李女士 ");input.put("contactPhone","13911112222");input.put("remark"," 4人到店，请准备热水 ");
        String order=data(call("POST","/api/customer/reserve",token,input)).asText();
        var row=data(call("GET","/api/reservations?keyword="+order,admin,null)).path("records").get(0);
        assertThat(row.path("contactName").asText()).isEqualTo("李女士");
        assertThat(row.path("contactPhone").asText()).isEqualTo("13911112222");
        assertThat(row.path("remark").asText()).isEqualTo("4人到店，请准备热水");
        assertThat(row.path("status").asText()).isEqualTo("PENDING");
        assertThat(data(call("GET","/api/customer/booking-contact",token,null)).path("phone").asText()).isEqualTo("13811112222");
        var blank=booking(room(),memberId,BookingRules.today().plusDays(1),"15:00");blank.remove("contactPhone");
        assertThat(call("POST","/api/customer/reserve",other,blank).path("code").asInt()).isNotEqualTo(200);
        blank.put("contactPhone","138****2222");
        assertThat(call("POST","/api/customer/reserve",token,blank).path("code").asInt()).isNotEqualTo(200);
        blank.remove("contactPhone");
        String fallback=data(call("POST","/api/customer/reserve",token,blank)).asText();
        assertThat(data(call("GET","/api/reservations?keyword="+fallback,admin,null)).path("records").get(0).path("contactPhone").asText()).isEqualTo("13811112222");
    }
}
