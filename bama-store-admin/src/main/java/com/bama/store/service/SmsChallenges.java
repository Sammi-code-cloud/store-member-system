package com.bama.store.service;

import com.bama.store.common.BusinessException;
import com.bama.store.config.SmsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;

@Service @RequiredArgsConstructor
public class SmsChallenges {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final SmsProperties config;

    public static void validatePhone(String phone) {
        if (phone == null || !phone.matches("1[3-9][0-9]{9}"))
            throw new BusinessException("请输入正确的中国大陆手机号");
    }

    public String newCode() { return String.format("%06d", new SecureRandom().nextInt(1000000)); }

    // A database lock also enforces limits across multiple application processes.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String reserve(String ticket, String phone, String identity, String ip, String code) {
        validatePhone(phone);
        jdbc.queryForList("SELECT id FROM t_sms_guard WHERE id=1 FOR UPDATE");
        Instant now = Instant.now();
        jdbc.update("DELETE FROM t_sms_challenge WHERE created_at < ?", Timestamp.from(now.minusSeconds(86400)));
        String p = WechatFlows.hash(phone), i = WechatFlows.hash(identity), address = WechatFlows.hash(ip);
        int minute = jdbc.queryForObject("SELECT COUNT(*) FROM t_sms_challenge WHERE created_at>? AND (phone_hash=? OR identity_hash=?)", Integer.class, Timestamp.from(now.minusSeconds(60)), p, i);
        int phoneDay = jdbc.queryForObject("SELECT COUNT(*) FROM t_sms_challenge WHERE phone_hash=?", Integer.class, p);
        int identityDay = jdbc.queryForObject("SELECT COUNT(*) FROM t_sms_challenge WHERE identity_hash=?", Integer.class, i);
        int ipDay = jdbc.queryForObject("SELECT COUNT(*) FROM t_sms_challenge WHERE ip_hash=?", Integer.class, address);
        int total = jdbc.queryForObject("SELECT COUNT(*) FROM t_sms_challenge", Integer.class);
        if (minute > 0) throw new BusinessException("请等待60秒后再获取验证码");
        if (phoneDay >= 5 || identityDay >= 5 || ipDay >= Math.max(1, config.getIpDailyLimit()) || total >= Math.max(1, config.getDailyLimit()))
            throw new BusinessException("验证码发送次数已达上限，请稍后再试或联系门店");
        String token = WechatFlows.random();
        jdbc.update("INSERT INTO t_sms_challenge(token_hash,ticket_hash,phone_hash,identity_hash,ip_hash,code_hash,created_at,expires_at) VALUES(?,?,?,?,?,?,?,?)",
                WechatFlows.hash(token), WechatFlows.hash(ticket), p, i, address, encoder.encode(code), Timestamp.from(now), Timestamp.from(now.plusSeconds(300)));
        return token;
    }

    public void activate(String challenge) {
        jdbc.update("UPDATE t_sms_challenge SET ready=1 WHERE token_hash=?", WechatFlows.hash(challenge));
    }

    // Wrong attempts and consumption must survive a subsequent binding rollback.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean verify(String ticket, String phone, String challenge, String code) {
        if (ticket == null || ticket.length() != 43 || challenge == null || challenge.length() != 43) return false;
        var rows = jdbc.queryForList("SELECT * FROM t_sms_challenge WHERE token_hash=? FOR UPDATE", WechatFlows.hash(challenge));
        if (rows.size() != 1) return false;
        var row = rows.get(0);
        if (((Number) row.get("ready")).intValue() != 1 || ((Number) row.get("consumed")).intValue() != 0
                || ((Number) row.get("attempts")).intValue() >= 5
                || !((Timestamp) row.get("expires_at")).toInstant().isAfter(Instant.now())) return false;
        jdbc.update("UPDATE t_sms_challenge SET attempts=attempts+1 WHERE token_hash=?", WechatFlows.hash(challenge));
        if (phone == null || !WechatFlows.hash(ticket).equals(row.get("ticket_hash"))
                || !WechatFlows.hash(phone).equals(row.get("phone_hash"))
                || code == null || !code.matches("[0-9]{6}") || !encoder.matches(code, row.get("code_hash").toString())) return false;
        jdbc.update("UPDATE t_sms_challenge SET consumed=1 WHERE token_hash=?", WechatFlows.hash(challenge));
        return true;
    }
}
