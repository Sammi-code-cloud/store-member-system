package com.bama.store.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：解析 token → 装配登录态与权限
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final LoginStaffService loginStaffService;
    private final com.bama.store.mapper.MemberMapper memberMapper;
    private final com.bama.store.mapper.StoreMapper storeMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (StringUtils.hasText(header) && header.startsWith(PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = header.substring(PREFIX.length());
            Long staffId = jwtUtil.parseStaffId(token);
            if (staffId != null) {
                LoginStaff loginStaff = loginStaffService.load(staffId);
                if (loginStaff != null) {
                    String selected = request.getHeader("X-Store-Id");
                    if (StringUtils.hasText(selected)) {
                        Long storeId = null;
                        try { storeId = Long.valueOf(selected); } catch (NumberFormatException ignored) { }
                        if (storeId == null || (!storeId.equals(loginStaff.getStoreId())
                                && !loginStaff.getPermissions().contains("store:all")) || storeMapper.selectById(storeId) == null) {
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":403,\"message\":\"无权访问该分店\",\"data\":null}");
                            return;
                        }
                        loginStaff.setStoreId(storeId);
                    }
                    List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>(loginStaff.getPermissions().stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList());
                    authorities.add(new SimpleGrantedAuthority("staff:session"));
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(loginStaff, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else {
                Long memberId = jwtUtil.parseCustomerId(token);
                var member = memberId == null ? null : memberMapper.selectById(memberId);
                if (member != null && Integer.valueOf(1).equals(member.getStatus())) {
                    var authentication = new UsernamePasswordAuthenticationToken(new LoginCustomer(memberId), null,
                            List.of(new SimpleGrantedAuthority("customer:self")));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        chain.doFilter(request, response);
    }
}
