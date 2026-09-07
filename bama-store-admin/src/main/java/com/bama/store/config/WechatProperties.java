package com.bama.store.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix="bama.wechat")
public class WechatProperties {
    private String miniAppId="", miniSecret="", webAppId="", webSecret="", webRedirectUri="";
    public boolean miniReady() { return !miniAppId.isBlank() && !miniSecret.isBlank(); }
    public boolean webReady() { return !webAppId.isBlank() && !webSecret.isBlank() && webRedirectUri.startsWith("https://"); }
}
