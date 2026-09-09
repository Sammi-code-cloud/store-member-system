package com.bama.store.controller;
import com.bama.store.common.Result;
import com.bama.store.service.DesktopWechatLogin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/wechat/desktop") @RequiredArgsConstructor
public class DesktopWechatController {
 private final DesktopWechatLogin login;
 public record Request(String ticket,String secret,String code) {}
 @ModelAttribute public void noCache(HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
 @PostMapping("/create") public Result<?> create(HttpServletRequest request){return Result.success(login.create(request.getRemoteAddr()));}
 @PostMapping("/poll") public Result<?> poll(@RequestBody Request r){return Result.success(login.poll(r.ticket(),r.secret()));}
 @PostMapping("/cancel") public Result<?> cancel(@RequestBody Request r){login.cancel(r.ticket(),r.secret());return Result.success(null);}
 @PostMapping("/confirm") public Result<?> confirm(@RequestBody Request r){login.confirm(r.ticket(),r.code());return Result.success(null);}
}
