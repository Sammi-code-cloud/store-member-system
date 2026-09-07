package com.bama.store.service;

import com.bama.store.common.BusinessException;
import com.bama.store.entity.TeaRoom;
import java.math.BigDecimal;
import java.time.*;

public final class BookingRules {
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private BookingRules() {}
    public static LocalDate today() { return LocalDate.now(ZONE); }
    public static int minute(String value) {
        if (value == null || !value.matches("\\d{2}:\\d{2}")) throw new BusinessException("时间格式应为 HH:mm");
        LocalTime time = LocalTime.parse(value);
        return time.getHour() * 60 + time.getMinute();
    }
    public static int duration(BigDecimal hours) {
        if (hours == null || hours.signum() <= 0 || hours.compareTo(BigDecimal.valueOf(24)) > 0
                || hours.multiply(BigDecimal.TEN).stripTrailingZeros().scale() > 0) throw new BusinessException("时长需大于 0、最多 24 小时，精确到 0.1 小时");
        return hours.multiply(BigDecimal.valueOf(60)).intValueExact();
    }
    public static String time(int minute) { return String.format("%02d:%02d", minute / 60, minute % 60); }
    public static boolean overlaps(int start, int end, int otherStart, int otherEnd) {
        return start < otherEnd && otherStart < end;
    }
    public static void validate(TeaRoom room, LocalDate date, String startTime, BigDecimal hours) {
        if (!Integer.valueOf(1).equals(room.getStatus())) throw new BusinessException("房间已暂停预订");
        if (date == null || date.isBefore(today()) || date.isAfter(today().plusDays(room.getAdvanceDays()))) throw new BusinessException("日期超出可预约范围");
        int start = minute(startTime), end = start + duration(hours);
        if (hours.compareTo(room.getMinHours()) < 0) throw new BusinessException("未达到房间起订时长");
        if (start < minute(room.getOpenTime()) || end > minute(room.getCloseTime())) throw new BusinessException("预约超出房间营业时间");
        if (date.equals(today()) && start <= LocalTime.now(ZONE).getHour() * 60 + LocalTime.now(ZONE).getMinute()) throw new BusinessException("不能预约已经过去的时间");
    }
}
