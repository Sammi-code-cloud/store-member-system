package com.bama.store.config;

import com.bama.store.security.ActiveStoreInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class StoreAccessConfig implements WebMvcConfigurer {
    private final ActiveStoreInterceptor activeStore;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(activeStore).addPathPatterns("/api/**");
    }
}
