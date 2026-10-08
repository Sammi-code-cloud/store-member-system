package com.bama.store.service;

import com.bama.store.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service @RequiredArgsConstructor
public class CustomerWechatBinding {
    private final WechatFlows flows;
    private final SmsSender sender;
    private final SmsChallenges challenges;
    private final WechatAccounts accounts;
    private final BusinessDictionary businessDictionary;


    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> send(String ticket, String phone, String ip) {
        businessDictionary.requireEnabled();
        SmsChallenges.validatePhone(phone);
        var identity = flows.peek(ticket, "CUSTOMER_BIND");
        if(identity.containsKey("phone") && !phone.equals(identity.get("phone"))) throw new BusinessException("请使用扫码时核对的会员手机号");
        if (!sender.ready()) throw new BusinessException("短信服务尚未开通，请使用账号登录或联系门店");
        String code = challenges.newCode();
        String challenge = challenges.reserve(ticket, phone, identity.get("app") + ":" + identity.get("openid"), ip, code);
        sender.send(phone, code);
        challenges.activate(challenge);
        return Map.of("challenge", challenge, "retryAfter", 60, "expiresIn", 300);
    }

    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> bind(String ticket, String phone, String challenge, String code) {
        businessDictionary.requireEnabled();
        SmsChallenges.validatePhone(phone);
        var identity = flows.peek(ticket, "CUSTOMER_BIND");
        if(identity.containsKey("phone") && !phone.equals(identity.get("phone"))) throw new BusinessException("请使用扫码时核对的会员手机号");
        if (!challenges.verify(ticket, phone, challenge, code))
            throw new BusinessException("验证码错误、已过期或已使用，请重新获取后验证");
        return accounts.bindCustomer(ticket, phone);
    }
}
