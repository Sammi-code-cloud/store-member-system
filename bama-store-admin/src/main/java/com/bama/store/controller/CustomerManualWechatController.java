package com.bama.store.controller;

import com.bama.store.common.Result;
import com.bama.store.common.BusinessException;
import com.bama.store.service.CustomerManualWechatLogin;
import com.bama.store.service.WechatClient;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/wechat/customer") @RequiredArgsConstructor
public class CustomerManualWechatController {
    private final WechatClient client;
    private final CustomerManualWechatLogin login;
    public record Request(String code,String phone,String name) {}
    @PostMapping("/manual-login")
    public Result<?> login(@RequestBody Request body,HttpServletResponse response){
        response.setHeader("Cache-Control","no-store");
        if(body.phone()!=null&&!body.phone().isBlank()&&!body.phone().matches("1[3-9]\\d{9}"))throw new BusinessException("请输入正确的11位手机号");
        if(body.code()==null||body.code().isBlank()||body.code().length()>256)throw new BusinessException("请重新发起微信登录");
        return Result.success(login.login(client.exchange("MINI",body.code()),body.phone(),body.name()));
    }
}
