package com.bama.store.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 单号生成工具
 */
public final class OrderNoUtil {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private OrderNoUtil() {
    }

    /** 生成单号：前缀 + 时间戳 + 4位随机 */
    public static String generate(String prefix) {
        int rand = ThreadLocalRandom.current().nextInt(1000, 10000);
        return prefix + LocalDateTime.now().format(FMT) + rand;
    }
}
