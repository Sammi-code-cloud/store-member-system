package com.bama.store.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Data @Component @ConfigurationProperties(prefix="bama.wechat.wallet-notice")
public class WalletNoticeProperties {
    private boolean enabled;
    private boolean rechargeEnabled=true;
    private String state="formal";
    private Template recharge=new Template(), charge=new Template();
    @Data public static class Template {
        private String id="";
        // Semantic value -> exact keyword from the account's approved WeChat template.
        private Map<String,String> fields=new LinkedHashMap<>();
        public boolean ready() {
            var allowed=Set.of("amount","gift","balance","time","store","order","type","reason");
            return !id.isBlank() && !fields.isEmpty() && allowed.containsAll(fields.keySet())
                && fields.values().stream().allMatch(s->s!=null && s.matches("(thing|amount|time|date|character_string|phrase|number)[0-9]+"))
                && fields.values().stream().distinct().count()==fields.size();
        }
    }
    public boolean ready() { return enabled && Set.of("formal","trial","developer").contains(state) && recharge.ready() && charge.ready(); }
}
