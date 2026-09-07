package com.bama.store.service;

import com.bama.store.common.BusinessException;
import com.bama.store.config.WechatProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.*;
import java.time.Duration;

@Service @RequiredArgsConstructor
public class WechatClient {
 private final WechatProperties config;
 private final ObjectMapper json;
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
 public record Identity(String appId,String openId,String unionId) {}
 public static String enc(String text) { return URLEncoder.encode(text,StandardCharsets.UTF_8); }
 public Identity exchange(String channel,String code) {
  boolean mini="MINI".equals(channel);
  if(mini ? !config.miniReady() : !config.webReady()) throw new BusinessException("微信登录尚未配置，请先使用账号密码登录");
  if(code==null || code.isBlank() || code.length()>256)throw new BusinessException("微信登录凭证无效，请重新授权");
  String app=mini?config.getMiniAppId():config.getWebAppId(), secret=mini?config.getMiniSecret():config.getWebSecret();
  String url="https://api.weixin.qq.com/"+(mini?"sns/jscode2session?":"sns/oauth2/access_token?")+"appid="+enc(app)+"&secret="+enc(secret)+"&"+(mini?"js_code":"code")+"="+enc(code)+"&grant_type=authorization_code";
  try {
   var response=http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8)).GET().build(),HttpResponse.BodyHandlers.ofString());
   var body=json.readTree(response.body());
   if(response.statusCode()!=200 || body.path("errcode").asInt()!=0 || body.path("openid").asText().isBlank())throw new BusinessException("微信授权失败或凭证已过期，请重新登录");
   return new Identity(app,body.path("openid").asText(),body.path("unionid").asText(""));
  } catch(BusinessException e){throw e;} catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw new BusinessException("微信服务暂不可用，请稍后重试");}
 }
}
