package com.bama.store.config;
import com.bama.store.service.WalletSmsNotices;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration @RequiredArgsConstructor
public class WalletSmsSchedule {
    private final WalletSmsNotices notices;
    @Scheduled(fixedDelayString="${bama.sms.wallet-notice.poll-ms:5000}",initialDelay=15000)
    public void send() { notices.dispatch(); }
}
