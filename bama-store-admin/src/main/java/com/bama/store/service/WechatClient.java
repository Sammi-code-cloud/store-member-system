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
 @org.springframework.beans.factory.annotation.Value("${bama.wechat.staff-code-environment:release}")
 private String staffCodeEnvironment="release";
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
 public record Identity(String appId,String openId,String unionId) {}
 public static String enc(String text) { return URLEncoder.encode(text,StandardCharsets.UTF_8); }
 private String miniAccessToken="";
 private long miniTokenExpiresAt;
 private synchronized String miniToken() throws Exception {
  if(System.currentTimeMillis()<miniTokenExpiresAt)return miniAccessToken;
  var payload=java.util.Map.of("grant_type","client_credential","appid",config.getMiniAppId(),"secret",config.getMiniSecret());
  var response=http.send(HttpRequest.newBuilder(URI.create("https://api.weixin.qq.com/cgi-bin/stable_token"))
    .timeout(Duration.ofSeconds(8)).header("Content-Type","application/json")
    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload))).build(),HttpResponse.BodyHandlers.ofString());
  var body=json.readTree(response.body());
  if(response.statusCode()!=200 || body.path("access_token").asText().isBlank())throw new BusinessException("无法获取小程序凭证，请检查微信应用配置及服务器 IP 白名单");
  miniAccessToken=body.path("access_token").asText();
  miniTokenExpiresAt=System.currentTimeMillis()+Math.max(0,body.path("expires_in").asLong()-120)*1000;
  return miniAccessToken;
 }
 /** The code carries a public store ID only, never a login token or a credential. */
 public String miniCode(Long storeId) {
  if(storeId==null || storeId<=0)throw new BusinessException("分店编号无效");
  return miniCode("s="+storeId,"pages/customer/entry",config.getMiniCodeEnvironment());
 }
 public String staffBindCode(String ticket) {
  if(ticket==null || !ticket.matches("[A-Za-z0-9_-]{22}"))throw new BusinessException("员工绑定码无效");
  return miniCode("b="+ticket,"pages/staff/login",staffCodeEnvironment);
 }
 public String desktopLoginCode(String ticket) {
  if(ticket==null || !ticket.matches("[A-Za-z0-9_-]{22}"))throw new BusinessException("登录码无效");
  return miniCode("l="+ticket,"pages/staff/login",staffCodeEnvironment);
 }
 public String memberBindCode(String ticket) {
  if(ticket==null || !ticket.matches("[A-Za-z0-9_-]{22}"))throw new BusinessException("会员绑定码无效");
  return miniCode("m="+ticket,"pages/customer/login",staffCodeEnvironment);
 }
 private String miniCode(String scene,String page,String env) {
  if(!config.miniReady())throw new BusinessException("微信小程序尚未配置，无法生成小程序码");
  if(!java.util.Set.of("release","trial","develop").contains(env))throw new BusinessException("小程序码版本配置无效");
  try {
   // WeChat's published-page check does not apply to uploaded trial/development pages.
   var payload=java.util.Map.of("scene",scene,"page",page,"env_version",env,"check_path","release".equals(env),"width",430);
   var response=http.send(HttpRequest.newBuilder(URI.create("https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token="+enc(miniToken())))
     .timeout(Duration.ofSeconds(8)).header("Content-Type","application/json")
     .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload))).build(),HttpResponse.BodyHandlers.ofByteArray());
   byte[] bytes=response.body();
   if(response.statusCode()!=200 || bytes.length>1024*1024)throw new BusinessException("小程序码生成失败，请稍后重试");
   if(response.headers().firstValue("Content-Type").orElse("").contains("json")) {
    int error=json.readTree(bytes).path("errcode").asInt();
    if(error==40001 || error==40014 || error==42001) { synchronized(this){miniTokenExpiresAt=0;} }
    throw new BusinessException(error==41030 ? ("release".equals(env) ? "正式版缺少扫码入口，请发布包含入口的小程序版本，或由管理员切换体验版测试" : "体验或开发版本缺少扫码入口，请上传正确版本并检查页面路径") : "小程序码生成失败，请检查发布版本和微信应用配置后重试");
   }
   boolean png=bytes.length>8 && bytes[0]==(byte)137 && bytes[1]==80 && bytes[2]==78 && bytes[3]==71;
   boolean jpeg=bytes.length>3 && bytes[0]==(byte)255 && bytes[1]==(byte)216 && bytes[2]==(byte)255;
   if(!png&&!jpeg)throw new BusinessException("微信未返回有效的小程序码，请检查小程序发布状态");
   return "data:image/"+(png?"png":"jpeg")+";base64,"+java.util.Base64.getEncoder().encodeToString(bytes);
  } catch(BusinessException e){throw e;} catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw new BusinessException("微信服务暂不可用，请稍后重试");}
 }
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
