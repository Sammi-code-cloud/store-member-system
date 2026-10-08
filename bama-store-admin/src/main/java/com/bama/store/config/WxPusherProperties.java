package com.bama.store.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bama.wxpusher")
public class WxPusherProperties {
    private boolean enabled;
    private String appToken = "";
}
