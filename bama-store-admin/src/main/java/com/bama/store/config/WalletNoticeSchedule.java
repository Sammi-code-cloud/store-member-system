package com.bama.store.config;
import com.bama.store.service.WalletNotices;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
@Configuration @RequiredArgsConstructor
public class WalletNoticeSchedule {
    private final WalletNotices notices;
    @Scheduled(fixedDelayString="${bama.wechat.wallet-notice.poll-ms:5000}",initialDelay=15000)
    public void send() { notices.dispatch(); }
}
