package com.bama.store.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data @Component @ConfigurationProperties(prefix = "bama.sms")
public class SmsProperties {
    private boolean enabled;
    private String accessKeyId = "";
    private String accessKeySecret = "";
    private String signName = "";
    private String templateCode = "";
    private int dailyLimit = 100;
    private int ipDailyLimit = 100;

    public boolean ready() {
        return enabled && !accessKeyId.isBlank() && !accessKeySecret.isBlank()
                && !signName.isBlank() && !templateCode.isBlank();
    }
}
