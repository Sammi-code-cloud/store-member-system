package com.bama.store.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.Set;

@Data @Component @ConfigurationProperties(prefix="bama.wechat.booking-notice")
public class BookingNoticeProperties {
    private boolean enabled;
    private String templateId="";
    private String state="formal";
    public boolean ready() { return enabled && !templateId.isBlank() && Set.of("formal","trial","developer").contains(state); }
}
