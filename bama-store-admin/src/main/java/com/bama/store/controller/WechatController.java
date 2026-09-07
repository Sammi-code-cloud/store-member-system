package com.bama.store.controller;
import com.bama.store.common.*;
import com.bama.store.config.WechatProperties;
import com.bama.store.service.*;
import com.bama.store.dto.LoginRequest;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/wechat") @RequiredArgsConstructor
public class WechatController {
 private final WechatProperties config;private final WechatClient client;private final WechatFlows flows;private final WechatAccounts accounts;private final AuthService auth;
 public record MiniRequest(String code,String audience) {}
 public record ExchangeRequest(String code,String state) {}
 public record BindRequest(String ticket,String phone,String password) {}
 private final CustomerWechatBinding customerBinding;
 public record SmsRequest(String ticket,String phone) {}
 public record CustomerBindRequest(String ticket,String phone,String challenge,String code) {}
 @PostMapping("/customer/sms") public Result<?> sms(@RequestBody SmsRequest body,HttpServletRequest request) {
  return Result.success(customerBinding.send(body.ticket(),body.phone(),request.getRemoteAddr()));
 }
 @PostMapping("/customer/bind") public Result<?> customerBind(@RequestBody CustomerBindRequest body) {
  return Result.success(customerBinding.bind(body.ticket(),body.phone(),body.challenge(),body.code()));
 }
 @ModelAttribute public void noCache(HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
 private String audience(String value){if(!"STAFF".equals(value)&&!"CUSTOMER".equals(value))throw new BusinessException("登录端类型无效");return value;}
 @GetMapping("/config") public Result<?> config(){return Result.success(Map.of("miniEnabled",config.miniReady(),"webEnabled",config.webReady(),"message","微信应用尚未开通或配置，请使用账号密码登录"));}
 @PostMapping("/mini") public Result<?> mini(@RequestBody MiniRequest body){String target=audience(body.audience());return Result.success(accounts.login(client.exchange("MINI",body.code()),target));}
 @GetMapping("/start") public Result<?> start(@RequestParam String audience,HttpServletResponse response){
  String target=audience(audience);if(!config.webReady())throw new BusinessException("网站微信登录尚未配置，请先使用账号密码登录");
  String browser=WechatFlows.random();String state=flows.create("STATE",Map.of("audience",target,"browser",WechatFlows.hash(browser)));
  response.addHeader("Set-Cookie",ResponseCookie.from("wx_login",browser).httpOnly(true).secure(true).sameSite("Lax").path("/api/wechat").maxAge(300).build().toString());
  String url="https://open.weixin.qq.com/connect/qrconnect?appid="+WechatClient.enc(config.getWebAppId())+"&redirect_uri="+WechatClient.enc(config.getWebRedirectUri())+"&response_type=code&scope=snsapi_login&state="+state+"#wechat_redirect";
  return Result.success(Map.of("url",url));
 }
 @PostMapping("/exchange") public Result<?> exchange(@RequestBody ExchangeRequest body,@CookieValue(name="wx_login",defaultValue="") String browser){
  var state=flows.consume(body.state(),"STATE");
  if(browser.isBlank()||!state.get("browser").equals(WechatFlows.hash(browser)))throw new BusinessException("微信登录校验失败，请从当前页面重新发起");
  return Result.success(accounts.login(client.exchange("WEB",body.code()),state.get("audience")));
 }
 @PostMapping("/bind") public Result<?> bind(@RequestBody BindRequest body){
  var ticket=flows.consume(body.ticket(),"BIND");
  if(body.phone()==null||body.password()==null||body.phone().length()>20||body.password().length()>64)throw new BusinessException("请填写员工账号密码");
  LoginRequest request=new LoginRequest();request.setPhone(body.phone());request.setPassword(body.password());
  var verified=auth.login(request);
  return Result.success(accounts.bind(new WechatClient.Identity(ticket.get("app"),ticket.get("openid"),ticket.get("unionid")),verified.getStaffId()));
 }
}
