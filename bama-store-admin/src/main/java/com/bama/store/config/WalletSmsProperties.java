package com.bama.store.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data @Component @ConfigurationProperties(prefix="bama.sms.wallet-notice")
public class WalletSmsProperties {
    private boolean enabled;
    private String accessKeyId="", accessKeySecret="", signName="";
    private String chargeTemplate="", rechargeTemplate="";
    public boolean ready() {
        return enabled && !accessKeyId.isBlank() && !accessKeySecret.isBlank() && !signName.isBlank()
            && !chargeTemplate.isBlank() && !rechargeTemplate.isBlank();
    }
}
