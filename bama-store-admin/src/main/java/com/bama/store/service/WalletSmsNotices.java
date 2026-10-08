package com.bama.store.service;

import com.bama.store.config.WalletSmsProperties;
import com.bama.store.entity.Member;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service @RequiredArgsConstructor
public class WalletSmsNotices {
    private final JdbcTemplate jdbc;
    private final WalletSmsProperties config;
    private final WalletSmsSender sender;
    private final ObjectMapper json;

    /** Commit the notification with the balance change; never call a provider inside the money transaction. */
    public void enqueue(String kind,String order,Member member,Long store,BigDecimal amount,BigDecimal balance) {
        if (!config.ready()) return;
        if (!TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("SMS requires a money transaction");
        if (!kind.equals("RECHARGE") && !kind.equals("CHARGE")) throw new IllegalArgumentException("Unknown wallet event");
        String event=kind+":"+order;
        if (jdbc.queryForObject("SELECT COUNT(*) FROM t_sms_wallet_notice WHERE event_key=?",Integer.class,event)>0) return;
        String phone=member.getPhone();
        boolean valid=phone!=null && phone.matches("1[3-9]\\d{9}");
        String name=member.getName()==null || member.getName().isBlank()?"会员":member.getName().trim();
        try {
            String payload=json.writeValueAsString(Map.of("name",name,"amount",amount.setScale(2).toPlainString(),"balance",balance.setScale(2).toPlainString()));
            jdbc.update("INSERT INTO t_sms_wallet_notice(event_key,member_id,store_id,phone,sign_name,template_code,payload,status,error_code) VALUES(?,?,?,?,?,?,?,?,?)",
                event,member.getId(),store,phone,config.getSignName(),kind.equals("RECHARGE")?config.getRechargeTemplate():config.getChargeTemplate(),payload,valid?"PENDING":"SKIPPED",valid?null:"INVALID_PHONE");
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {throw new IllegalStateException("Cannot serialize wallet SMS",e);}
    }
    public void dispatch() {
        if (!config.ready()) return;
        // SendSms has no provider idempotency. An interrupted or timed-out send is not automatically repeated.
        jdbc.update("UPDATE t_sms_wallet_notice SET status='UNKNOWN',error_code='INTERRUPTED',update_time=CURRENT_TIMESTAMP WHERE status='SENDING' AND update_time<?",LocalDateTime.now().minusMinutes(5));
        for (String event:jdbc.queryForList("SELECT event_key FROM t_sms_wallet_notice WHERE status='PENDING' ORDER BY create_time LIMIT 5",String.class)) {
            if (jdbc.update("UPDATE t_sms_wallet_notice SET status='SENDING',update_time=CURRENT_TIMESTAMP WHERE event_key=? AND status='PENDING'",event)!=1) continue;
            String status="UNKNOWN",error="TRANSPORT_OR_RESPONSE",provider=null;
            try {
                var row=jdbc.queryForMap("SELECT * FROM t_sms_wallet_notice WHERE event_key=?",event);
                var phones=jdbc.queryForList("SELECT phone FROM t_member WHERE id=? AND status=1 AND deleted=0",String.class,row.get("member_id"));
                if (phones.size()!=1 || !phones.get(0).equals(row.get("phone"))) {status="SKIPPED";error="RECIPIENT_CHANGED";}
                else {
                    var result=sender.send((String)row.get("phone"),(String)row.get("sign_name"),(String)row.get("template_code"),(String)row.get("payload"),event);
                    if (result!=null && result.code()!=null) {
                        status="OK".equals(result.code())?"ACCEPTED":"REJECTED";
                        error="ACCEPTED".equals(status)?null:safeCode(result.code());
                        provider=result.bizId()==null?null:result.bizId().substring(0,Math.min(128,result.bizId().length()));
                    }
                }
            } catch (Exception ignored) { /* Never log phone, financial payload, credentials or provider exception details. */ }
            jdbc.update("UPDATE t_sms_wallet_notice SET status=?,error_code=?,provider_id=?,update_time=CURRENT_TIMESTAMP WHERE event_key=? AND status='SENDING'",status,error,provider,event);
        }
    }
    private String safeCode(String code) {return code.matches("[A-Za-z0-9_.-]{1,64}")?code:"PROVIDER_REJECTED";}
}
