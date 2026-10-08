package com.bama.store.service;

import com.bama.store.common.BusinessException;
import com.bama.store.config.WalletNoticeProperties;
import com.bama.store.config.WechatProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service @RequiredArgsConstructor
public class WalletNotices {
    private final JdbcTemplate jdbc;
    private final WalletNoticeProperties config;
    private final WechatProperties wechat;
    private final WechatClient client;
    private final ObjectMapper json;
    private final BusinessDictionary businessDictionary;


    public Map<String,Object> settings() {
        boolean ready=businessDictionary.enabled() && config.ready() && wechat.miniReady();
        return Map.of("enabled",ready,"templateIds",ready ? List.of(config.getRecharge().getId(),config.getCharge().getId()).stream().distinct().toList() : List.of());
    }
    private boolean linked(Long member,String app,String openid) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM t_wechat_account WHERE identity_key=? AND audience='CUSTOMER' AND account_id=? AND deleted=0",Integer.class,"APP:"+WechatFlows.hash(app+":"+openid),member)>0;
    }
    @Transactional
    public void register(Long member,String code) {
        businessDictionary.requireEnabled();
        if (!config.ready()) throw new BusinessException("门店尚未配置微信交易通知");
        if (code==null || code.isBlank() || code.length()>256) throw new BusinessException("请重新微信授权");
        var identity=client.exchange("MINI",code);
        if (!wechat.getMiniAppId().equals(identity.appId()) || !linked(member,identity.appId(),identity.openId()))
            throw new BusinessException("当前微信与会员不一致，请使用绑定的微信登录");
        // Lock the membership row to serialize repeat registrations. Never trust a client-supplied OpenID.
        if (jdbc.queryForList("SELECT id FROM t_member WHERE id=? AND status=1 AND deleted=0 FOR UPDATE",Long.class,member).isEmpty())
            throw new BusinessException("会员不可用");
        if (jdbc.update("UPDATE t_wx_wallet_receiver SET open_id=?,update_time=CURRENT_TIMESTAMP WHERE app_id=? AND member_id=?",identity.openId(),identity.appId(),member)==0)
            jdbc.update("INSERT INTO t_wx_wallet_receiver(app_id,member_id,open_id) VALUES(?,?,?)",identity.appId(),member,identity.openId());
    }
    /** The outbox entry commits or rolls back together with the money transaction. No network IO here. */
    public void enqueue(String kind,String order,Long member,Long store,BigDecimal amount,BigDecimal gift,BigDecimal balance) {
        if (!config.ready() || (kind.equals("RECHARGE") && !config.isRechargeEnabled())) return;
        if (!TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Wallet notice requires a money transaction");
        String event=kind+":"+order;
        if (jdbc.queryForObject("SELECT COUNT(*) FROM t_wx_wallet_notice WHERE event_key=?",Integer.class,event)>0) return;
        var template=kind.equals("RECHARGE")?config.getRecharge():config.getCharge();
        String app=wechat.getMiniAppId();
        var receivers=jdbc.queryForList("SELECT open_id FROM t_wx_wallet_receiver WHERE app_id=? AND member_id=? AND deleted=0",String.class,app,member);
        String openid=receivers.size()==1?receivers.get(0):null;
        boolean eligible=openid!=null && linked(member,app,openid);
        var names=jdbc.queryForList("SELECT name FROM t_store WHERE id=?",String.class,store);
        boolean recharge=kind.equals("RECHARGE");
        BigDecimal bonus=gift==null?BigDecimal.ZERO:gift;
        BigDecimal change=recharge?amount.add(bonus):amount.negate();
        var values=Map.of("amount",money(change),"gift",money(gift),"balance",money(balance),
            "time",LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            "store",clip(names.isEmpty()?"门店":names.get(0),20),"order",order,"type",recharge?"充值":"扣款",
            "reason",recharge?(bonus.signum()>0?"会员充值（含赠送）":"会员充值到账"):"会员余额消费");
        Map<String,Object> data=new LinkedHashMap<>();
        template.getFields().forEach((semantic,keyword)->data.put(keyword,Map.of("value",values.get(semantic))));
        Map<String,Object> payload=new LinkedHashMap<>();
        payload.put("touser",eligible?openid:"");payload.put("template_id",template.getId());
        payload.put("page","pages/customer/profile");payload.put("miniprogram_state",config.getState());payload.put("lang","zh_CN");payload.put("data",data);
        try {
            jdbc.update("INSERT INTO t_wx_wallet_notice(event_key,member_id,app_id,open_id,template_id,payload,status,error_code) VALUES(?,?,?,?,?,?,?,?)",
                event,member,app,openid,template.getId(),json.writeValueAsString(payload),eligible?"PENDING":"SKIPPED",eligible?null:"NO_RECEIVER");
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("Cannot serialize wallet notification",e); }
    }
    private String money(BigDecimal v) { return (v==null?BigDecimal.ZERO:v).setScale(2).toPlainString(); }
    private String clip(String s,int n) { return s.codePointCount(0,s.length())<=n?s:s.substring(0,s.offsetByCodePoints(0,n)); }

    public void dispatch() {
        if (!config.ready()) return;
        // A crashed/ambiguous request must not be sent again automatically: WeChat offers no idempotency key.
        jdbc.update("UPDATE t_wx_wallet_notice SET status='UNKNOWN',error_code='INTERRUPTED',update_time=CURRENT_TIMESTAMP WHERE status='SENDING' AND update_time<?",LocalDateTime.now().minusMinutes(5));
        for (String event:jdbc.queryForList("SELECT event_key FROM t_wx_wallet_notice WHERE status='PENDING' ORDER BY create_time LIMIT 10",String.class)) {
            if (jdbc.update("UPDATE t_wx_wallet_notice SET status='SENDING',update_time=CURRENT_TIMESTAMP WHERE event_key=? AND status='PENDING'",event)!=1) continue;
            String status="UNKNOWN",error="TRANSPORT_OR_RESPONSE";
            try {
                var row=jdbc.queryForMap("SELECT * FROM t_wx_wallet_notice WHERE event_key=?",event);
                String app=(String)row.get("app_id"),openid=(String)row.get("open_id");
                if (!wechat.getMiniAppId().equals(app) || !linked(((Number)row.get("member_id")).longValue(),app,openid)) {
                    status="SKIPPED";error="BINDING_CHANGED";
                } else {
                    var payload=json.readValue((String)row.get("payload"),new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});
                    int result=client.sendSubscription(payload);
                    status=result==0?"SENT":"REJECTED";error=result==0?null:Integer.toString(result);
                }
            } catch (Exception ignored) { /* Never log credentials, OpenIDs, payloads or tokens. */ }
            jdbc.update("UPDATE t_wx_wallet_notice SET status=?,error_code=?,update_time=CURRENT_TIMESTAMP WHERE event_key=?",status,error,event);
        }
    }
}
