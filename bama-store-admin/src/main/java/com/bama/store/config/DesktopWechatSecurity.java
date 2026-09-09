package com.bama.store.config;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class DesktopWechatSecurity {
 @Bean @Order(-1)
 public SecurityFilterChain desktopWechatChain(HttpSecurity http,SecurityConfig existing) throws Exception {
  return http.securityMatcher("/api/wechat/desktop/**").csrf(c->c.disable()).cors(c->c.configurationSource(existing.corsConfigurationSource()))
   .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a->a.requestMatchers(HttpMethod.POST,"/api/wechat/desktop/create","/api/wechat/desktop/poll","/api/wechat/desktop/cancel","/api/wechat/desktop/confirm").permitAll().anyRequest().denyAll()).build();
 }
}
