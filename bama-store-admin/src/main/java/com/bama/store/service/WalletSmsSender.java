package com.bama.store.service;

import com.bama.store.config.WalletSmsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class WalletSmsSender {
    private final WalletSmsProperties config;
    public record Result(String code,String bizId) {}
    public Result send(String phone,String sign,String template,String payload,String event) throws Exception {
        if (!config.ready()) throw new IllegalStateException("Wallet SMS is not configured");
        var options=new com.aliyun.teaopenapi.models.Config()
            .setAccessKeyId(config.getAccessKeyId()).setAccessKeySecret(config.getAccessKeySecret())
            .setEndpoint("dysmsapi.aliyuncs.com").setProtocol("https").setConnectTimeout(5000).setReadTimeout(10000);
        var client=new com.aliyun.dysmsapi20170525.Client(options);
        var request=new com.aliyun.dysmsapi20170525.models.SendSmsRequest()
            .setPhoneNumbers(phone).setSignName(sign).setTemplateCode(template).setTemplateParam(payload).setOutId(event);
        var response=client.sendSmsWithOptions(request,new com.aliyun.teautil.models.RuntimeOptions().setAutoretry(false));
        if (response==null || response.getBody()==null || response.getBody().getCode()==null)
            throw new IllegalStateException("Missing SMS response");
        return new Result(response.getBody().getCode(),response.getBody().getBizId());
    }
}
