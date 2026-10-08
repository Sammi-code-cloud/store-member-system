package com.bama.store.security;

import com.bama.store.service.StoreAvailability;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Component
@RequiredArgsConstructor
public class ActiveStoreInterceptor implements HandlerInterceptor {
    private final StoreAvailability stores;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginStaff staff)) return true;
        // Match resolved MVC routes, not user-supplied path substrings.
        String route = String.valueOf(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
        String method = request.getMethod();
        if (route.startsWith("/api/customer/") || route.startsWith("/api/wechat/") || route.startsWith("/api/auth/")) return true;
        if (route.equals("/api/store") && (method.equals("GET") || method.equals("POST"))) return true;
        if (route.equals("/api/business-dictionary") && (method.equals("GET") || method.equals("PUT"))) return true;
        if (route.equals("/api/store/{id}/status") && method.equals("PUT")) return true;
        if (route.equals("/api/store/{id}") && method.equals("DELETE")) return true;
        stores.requireActive(staff.getStoreId());
        return true;
    }
}
