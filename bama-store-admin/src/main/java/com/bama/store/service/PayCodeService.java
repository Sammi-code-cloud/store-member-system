package com.bama.store.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会员付款码服务（一次性、短时效）
 * 注意：此处为内存实现，仅用于演示/单机；生产环境请替换为 Redis，
 * 以支持分布式与自动过期。
 */
@Service
public class PayCodeService {

    @Value("${bama.paycode.expire-seconds:60}")
    private long expireSeconds;

    /** code -> 记录 */
    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    private record Entry(Long memberId, long expireAt) {
    }

    /** 为会员生成一次性付款码 */
    public String generate(Long memberId) {
        String code = "BM" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        store.put(code, new Entry(memberId, System.currentTimeMillis() + expireSeconds * 1000));
        return code;
    }

    /**
     * 解析付款码，返回会员ID。
     * 校验通过后立即失效（一次性），防止重放。
     * 无效或过期返回 null。
     */
    public Long resolve(String code) {
        if (code == null) {
            return null;
        }
        Entry entry = store.remove(code);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expireAt()) {
            return null;
        }
        return entry.memberId();
    }
}
