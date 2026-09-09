package com.bama.store;

import com.bama.store.config.WechatProperties;
import com.bama.store.service.WechatClient;
import com.bama.store.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.net.http.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WechatCodeEnvironmentTest {
    @Test void trialStaffCodeDoesNotChangeCustomerReleaseCode() throws Exception {
        var config=new WechatProperties();config.setMiniAppId("test-app");config.setMiniSecret("test-secret");
        var json=new ObjectMapper();var client=new WechatClient(config,json);
        var http=mock(HttpClient.class);
        ReflectionTestUtils.setField(client,"http",http);
        ReflectionTestUtils.setField(client,"staffCodeEnvironment","trial");
        ReflectionTestUtils.setField(client,"miniAccessToken","test-token");
        ReflectionTestUtils.setField(client,"miniTokenExpiresAt",Long.MAX_VALUE);
        @SuppressWarnings("unchecked") HttpResponse<byte[]> response=mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.headers()).thenReturn(HttpHeaders.of(Map.of("Content-Type",List.of("image/png")),(a,b)->true));
        when(response.body()).thenReturn(new byte[]{(byte)137,80,78,71,13,10,26,10,0});
        when(http.send(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any())).thenReturn(response);
        assertThat(client.staffBindCode("abcdefghijklmnopqrstuv")).startsWith("data:image/png;base64,");
        client.miniCode(1L);
        var requests=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(http,times(2)).send(requests.capture(),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any());
        var bodies=new ArrayList<com.fasterxml.jackson.databind.JsonNode>();
        for(var request:requests.getAllValues()) {
            var subscriber=HttpResponse.BodySubscribers.ofByteArray();
            request.bodyPublisher().orElseThrow().subscribe(new java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer>() {
                public void onSubscribe(java.util.concurrent.Flow.Subscription subscription){subscriber.onSubscribe(subscription);}
                public void onNext(java.nio.ByteBuffer item){subscriber.onNext(List.of(item));}
                public void onError(Throwable error){subscriber.onError(error);}
                public void onComplete(){subscriber.onComplete();}
            });
            bodies.add(json.readTree(subscriber.getBody().toCompletableFuture().get()));
        }
        assertThat(bodies.get(0).path("env_version").asText()).isEqualTo("trial");
        assertThat(bodies.get(0).path("check_path").asBoolean()).isFalse();
        assertThat(bodies.get(0).path("page").asText()).isEqualTo("pages/staff/login");
        assertThat(bodies.get(0).path("scene").asText()).isEqualTo("b=abcdefghijklmnopqrstuv");
        assertThat(bodies.get(1).path("env_version").asText()).isEqualTo("release");
        assertThat(bodies.get(1).path("check_path").asBoolean()).isTrue();
    }

    @Test void invalidEnvironmentIsRejectedBeforeCallingWechat() {
        var config=new WechatProperties();config.setMiniAppId("test-app");config.setMiniSecret("test-secret");
        var client=new WechatClient(config,new ObjectMapper());
        ReflectionTestUtils.setField(client,"staffCodeEnvironment","typo");
        assertThatThrownBy(()->client.staffBindCode("abcdefghijklmnopqrstuv"))
                .isInstanceOf(BusinessException.class).hasMessage("小程序码版本配置无效");
    }
}
