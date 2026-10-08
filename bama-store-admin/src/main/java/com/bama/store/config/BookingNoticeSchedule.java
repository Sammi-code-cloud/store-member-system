package com.bama.store.config;
import com.bama.store.service.BookingNotices;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration @RequiredArgsConstructor
public class BookingNoticeSchedule {
    private final BookingNotices notices;
    @Scheduled(fixedDelayString="${bama.wechat.booking-notice.poll-ms:5000}",initialDelay=30000)
    public void send() { notices.dispatch(); }
}
