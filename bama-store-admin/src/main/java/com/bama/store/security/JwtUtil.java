package com.bama.store.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类（基于 jjwt 0.12.x）
 */
@Component
public class JwtUtil {

    @Value("${bama.jwt.secret}")
    private String secret;

    @Value("${bama.jwt.expire}")
    private long expire;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 生成 token，subject 为员工ID */
    public String generate(Long staffId, String name) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(staffId))
                .claim("name", name)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expire))
                .signWith(key())
                .compact();
    }

    /** 解析并校验 token，返回员工ID；非法或过期返回 null */
    public Long parseStaffId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }
}
