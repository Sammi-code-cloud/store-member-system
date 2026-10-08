package com.bama.store.service;

import com.bama.store.config.WxPusherProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@Service @RequiredArgsConstructor
public class WxPusherClient {
    private final WxPusherProperties config;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public record Delivery(boolean accepted, String messageId, String errorCode) {}

    public Delivery send(String uid, String content) throws Exception {
        var payload = Map.of("appToken", config.getAppToken(), "content", content,
                "summary", "新房间预约，请及时处理", "contentType", 1, "uids", List.of(uid));
        var response = http.send(HttpRequest.newBuilder(URI.create("https://wxpusher.zjiecode.com/api/send/message"))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload))).build(),
                HttpResponse.BodyHandlers.ofString());
        return parse(response.statusCode(), response.body(), uid);
    }

    // Do not persist or log remote error text: it may echo credentials or customer data.
    public Delivery parse(int httpStatus, String body, String uid) throws Exception {
        if (httpStatus != 200) return new Delivery(false, null, "HTTP_" + httpStatus);
        var root = json.readTree(body);
        if (root == null || root.path("code").asInt(-1) != 1000)
            return new Delivery(false, null, "API_" + (root == null ? -1 : root.path("code").asInt(-1)));
        for (var item : root.path("data")) {
            String recordId = item.path("sendRecordId").asText("");
            if (recordId.isBlank()) recordId = item.path("messageId").asText("");
            if (uid.equals(item.path("uid").asText()) && item.path("code").asInt(-1) == 1000
                    && !recordId.isBlank())
                return new Delivery(true, recordId, null);
        }
        return new Delivery(false, null, "RECIPIENT_NOT_ACCEPTED");
    }
}
