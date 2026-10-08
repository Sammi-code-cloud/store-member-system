package com.bama.store.service;

import com.bama.store.common.BusinessException;
import com.bama.store.config.BookingNoticeProperties;
import com.bama.store.config.WechatProperties;
import com.bama.store.entity.Reservation;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class BookingNotices {
    private final JdbcTemplate jdbc;
    private final BookingNoticeProperties config;
    private final WechatProperties wechat;
    private final WechatClient client;
    private final ObjectMapper json;
    private final BusinessDictionary businessDictionary;


    public Map<String,Object> settings() {
        boolean ready=businessDictionary.enabled() && config.ready() && wechat.miniReady();
        return Map.of("enabled",ready,"templateIds",ready?List.of(config.getTemplateId()):List.of());
    }
    private boolean linked(Long member,String app,String openid) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM t_wechat_account w JOIN t_member m ON m.id=w.account_id "
                + "WHERE w.identity_key=? AND w.audience='CUSTOMER' AND w.account_id=? AND w.deleted=0 AND m.deleted=0 AND m.status=1",
                Integer.class,"APP:"+WechatFlows.hash(app+":"+openid),member)>0;
    }
    @Transactional
    public void register(Long member,String code) {
        businessDictionary.requireEnabled();
        if (!config.ready() || !wechat.miniReady()) throw new BusinessException("门店尚未配置预约通知");
        if (code==null || code.isBlank() || code.length()>256) throw new BusinessException("请重新微信授权");
        var identity=client.exchange("MINI",code);
        if (!wechat.getMiniAppId().equals(identity.appId()) || !linked(member,identity.appId(),identity.openId()))
            throw new BusinessException("当前微信与会员不一致，请使用绑定的微信登录");
        if (jdbc.queryForList("SELECT id FROM t_member WHERE id=? AND status=1 AND deleted=0 FOR UPDATE",Long.class,member).isEmpty())
            throw new BusinessException("会员不可用");
        if (jdbc.update("UPDATE t_wx_booking_receiver SET open_id=?,update_time=CURRENT_TIMESTAMP WHERE app_id=? AND member_id=?",identity.openId(),identity.appId(),member)==0)
            jdbc.update("INSERT INTO t_wx_booking_receiver(app_id,member_id,open_id) VALUES(?,?,?)",identity.appId(),member,identity.openId());
    }
    /** Only the successful staff confirmation transition calls this method, within its transaction. */
    public void confirmed(Reservation r) {
        if (!config.ready() || !wechat.miniReady()) return;
        if (!TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Booking notice requires confirmation transaction");
        String event="CONFIRMED:"+r.getOrderNo();
        if (jdbc.queryForObject("SELECT COUNT(*) FROM t_wx_booking_notice WHERE event_key=?",Integer.class,event)>0) return;
        String app=wechat.getMiniAppId();
        var receivers=jdbc.queryForList("SELECT open_id FROM t_wx_booking_receiver WHERE app_id=? AND member_id=? AND deleted=0",String.class,app,r.getMemberId());
        String openid=receivers.size()==1?receivers.get(0):null;
        boolean eligible=openid!=null && linked(r.getMemberId(),app,openid);
        var store=jdbc.queryForMap("SELECT name,address,phone FROM t_store WHERE id=?",r.getStoreId());
        String address=Objects.toString(store.get("address"),"");
        if(address.isBlank()) address=Objects.toString(store.get("name"),"门店");
        String phone=Objects.toString(store.get("phone"),"").trim();
        if(!phone.matches("[0-9+\\- ]{3,20}"))phone=Objects.toString(r.getContactPhone(),"");
        var data=Map.of("thing1",Map.of("value",clip(Objects.toString(r.getRoomName(),"茶室预约"),20)),
                "time3",Map.of("value",r.getReserveDate()+" "+r.getStartTime()),
                "thing4",Map.of("value",clip(address,20)),"phrase7",Map.of("value","预约成功"),
                "phone_number5",Map.of("value",phone));
        var payload=Map.of("touser",eligible?openid:"","template_id",config.getTemplateId(),
                "page","pages/customer/profile","miniprogram_state",config.getState(),"lang","zh_CN","data",data);
        try {
            jdbc.update("INSERT INTO t_wx_booking_notice(event_key,member_id,app_id,open_id,payload,status,error_code) VALUES(?,?,?,?,?,?,?)",
                    event,r.getMemberId(),app,openid,json.writeValueAsString(payload),eligible?"PENDING":"SKIPPED",eligible?null:"NO_RECEIVER");
        } catch(com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("Cannot serialize booking notice",e); }
    }
    private String clip(String value,int length) {
        return value.codePointCount(0,value.length())<=length?value:value.substring(0,value.offsetByCodePoints(0,length));
    }
    public void dispatch() {
        if(!config.ready() || !wechat.miniReady()) return;
        jdbc.update("UPDATE t_wx_booking_notice SET status='UNKNOWN',error_code='INTERRUPTED',update_time=CURRENT_TIMESTAMP WHERE status='SENDING' AND update_time<?",LocalDateTime.now().minusMinutes(5));
        for(String event:jdbc.queryForList("SELECT event_key FROM t_wx_booking_notice WHERE status='PENDING' ORDER BY create_time LIMIT 10",String.class)) {
            if(jdbc.update("UPDATE t_wx_booking_notice SET status='SENDING',update_time=CURRENT_TIMESTAMP WHERE event_key=? AND status='PENDING'",event)!=1)continue;
            String status="UNKNOWN",error="TRANSPORT_OR_RESPONSE";
            try {
                var row=jdbc.queryForMap("SELECT * FROM t_wx_booking_notice WHERE event_key=?",event);
                String app=(String)row.get("app_id"),openid=(String)row.get("open_id");
                if(!wechat.getMiniAppId().equals(app) || !linked(((Number)row.get("member_id")).longValue(),app,openid)) {
                    status="SKIPPED";error="BINDING_CHANGED";
                } else {
                    var payload=json.readValue((String)row.get("payload"),new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});
                    int result=client.sendSubscription(payload);
                    status=result==0?"SENT":"REJECTED";error=result==0?null:Integer.toString(result);
                }
            } catch(Exception e) { if(e instanceof InterruptedException)Thread.currentThread().interrupt(); }
            // Ambiguous sends are not retried: a retry could consume another one-time subscription.
            jdbc.update("UPDATE t_wx_booking_notice SET status=?,error_code=?,update_time=CURRENT_TIMESTAMP WHERE event_key=?",status,error,event);
        }
    }
}
