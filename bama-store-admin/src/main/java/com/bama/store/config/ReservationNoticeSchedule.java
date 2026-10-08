package com.bama.store.config;

import com.bama.store.service.ReservationNotices;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration @RequiredArgsConstructor
public class ReservationNoticeSchedule {
    private final ReservationNotices notices;
    @Scheduled(fixedDelayString = "${bama.wxpusher.poll-ms:5000}", initialDelay = 30000)
    public void send() { notices.dispatch(); }
}
