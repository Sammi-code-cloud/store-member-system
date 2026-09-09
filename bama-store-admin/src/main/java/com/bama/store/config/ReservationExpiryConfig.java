package com.bama.store.config;
import com.bama.store.service.ReservationExpiry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.*;

@Configuration @EnableScheduling @RequiredArgsConstructor
public class ReservationExpiryConfig implements WebMvcConfigurer {
 private final ReservationExpiry expiry;
 @Scheduled(fixedDelay=3600000,initialDelay=3600000) public void sweep(){expiry.expire();}
 @Override public void addInterceptors(InterceptorRegistry registry){
  registry.addInterceptor(new HandlerInterceptor(){
   @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler){expiry.expire();return true;}
  }).addPathPatterns("/api/reservations","/api/reservations/**","/api/dashboard","/api/customer/*/reservations","/api/customer/rooms/*/reserved","/api/customer/rooms/*/slots");
 }
}
