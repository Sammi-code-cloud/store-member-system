package com.bama.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** 独立放行微信手填手机号登录，现有员工和顾客接口鉴权保持原配置。 */
@Configuration
public class CustomerManualWechatSecurity {
    @Bean @Order(0)
    public SecurityFilterChain customerManualWechatChain(HttpSecurity http, SecurityConfig existing) throws Exception {
        return http.securityMatcher("/api/wechat/customer/manual-login","/api/wechat/customer/member-bind")
                .csrf(c -> c.disable())
                .cors(c -> c.configurationSource(existing.corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.POST,"/api/wechat/customer/manual-login","/api/wechat/customer/member-bind").permitAll().anyRequest().denyAll())
                .build();
    }
}
