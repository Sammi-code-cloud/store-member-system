package com.bama.store.service;

import com.bama.store.common.BusinessException;
import com.bama.store.config.SmsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;

/** Only the server calls Aliyun; never log requests, codes or provider exception payloads. */
@Service @RequiredArgsConstructor
public class SmsSender {
    private final SmsProperties config;
    private final ObjectMapper json;

    public boolean ready() { return config.ready(); }

    public void send(String phone, String code) {
        if (!ready()) throw new BusinessException("短信服务尚未开通，请使用账号登录或联系门店");
        try {
            var options = new com.aliyun.teaopenapi.models.Config()
                    .setAccessKeyId(config.getAccessKeyId()).setAccessKeySecret(config.getAccessKeySecret())
                    .setEndpoint("dysmsapi.aliyuncs.com").setProtocol("https")
                    .setConnectTimeout(5000).setReadTimeout(10000);
            var client = new com.aliyun.dysmsapi20170525.Client(options);
            var request = new com.aliyun.dysmsapi20170525.models.SendSmsRequest()
                    .setPhoneNumbers(phone).setSignName(config.getSignName())
                    .setTemplateCode(config.getTemplateCode())
                    .setTemplateParam(json.writeValueAsString(Map.of("code", code)));
            var response = client.sendSmsWithOptions(request,
                    new com.aliyun.teautil.models.RuntimeOptions().setAutoretry(false));
            if (response == null || response.getBody() == null || !"OK".equals(response.getBody().getCode()))
                throw new BusinessException("短信发送失败，请稍后重试或联系门店");
        } catch (Exception ignored) {
            throw new BusinessException("短信发送失败，请稍后重试或联系门店");
        }
    }
}
