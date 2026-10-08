package com.bama.store.service;
import com.bama.store.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;
/** Legacy clients must also verify phone ownership before first binding. */
@Service @RequiredArgsConstructor
public class CustomerManualWechatLogin {
    private final WechatAccounts accounts;
    public Map<String,Object> login(WechatClient.Identity identity,String phone,String name) {
        if(phone!=null && !phone.isBlank()) SmsChallenges.validatePhone(phone);
        if(name!=null && name.trim().length()>32) throw new BusinessException("称呼最多32个字");
        return accounts.login(identity,"CUSTOMER");
    }
}
