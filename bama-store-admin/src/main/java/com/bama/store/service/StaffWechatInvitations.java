package com.bama.store.service;

import com.bama.store.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StaffWechatInvitations {
    private final JdbcTemplate jdbc;
    private final WechatAccounts accounts;
    private final WechatClient wechat;
    private final StaffWechatMembership membership;

    @Transactional
    public String create(Long staffId) {
        requireStaff(staffId);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM t_wechat_account WHERE audience='STAFF' AND account_id=?", Integer.class, staffId) > 0)
            throw new BusinessException("该员工已绑定微信，可直接微信登录；如需换绑请先核实原绑定");
        jdbc.update("DELETE FROM t_wechat_flow WHERE kind='STAFF_QR' AND (payload=? OR expires_at<?)",staffId.toString(),Timestamp.from(Instant.now()));
        byte[] bytes=new byte[16]; new SecureRandom().nextBytes(bytes);
        String ticket=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("INSERT INTO t_wechat_flow(token_hash,kind,payload,expires_at) VALUES(?,'STAFF_QR',?,?)",
                WechatFlows.hash(ticket),staffId.toString(),Timestamp.from(Instant.now().plusSeconds(600)));
        return ticket;
    }

    @Transactional
    public Map<String,Object> bind(String ticket,String phone,String code) {
        if(ticket==null || !ticket.matches("[A-Za-z0-9_-]{22}"))throw new BusinessException("员工绑定码无效，请重新扫码");
        if(phone==null || !phone.matches("1[3-9]\\d{9}"))throw new BusinessException("请填写员工手机号");
        var rows=jdbc.queryForList("SELECT payload FROM t_wechat_flow WHERE token_hash=? AND kind='STAFF_QR' AND expires_at>?",WechatFlows.hash(ticket),Timestamp.from(Instant.now()));
        if(rows.size()!=1)throw new BusinessException("绑定码已过期或已使用，请管理员重新生成");
        Long staffId=Long.valueOf(rows.get(0).get("payload").toString());
        var staff=requireStaff(staffId);
        // Issuing and consuming both lock staff before the ticket, including concurrent refreshes.
        if(jdbc.queryForList("SELECT payload FROM t_wechat_flow WHERE token_hash=? AND kind='STAFF_QR' AND expires_at>? FOR UPDATE",WechatFlows.hash(ticket),Timestamp.from(Instant.now())).isEmpty())
            throw new BusinessException("绑定码已过期或已使用，请管理员重新生成");
        if(!phone.equals(staff.get("phone")))throw new BusinessException("手机号与此二维码对应的员工不一致，请输入管理员登记的手机号");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM t_wechat_account WHERE audience='STAFF' AND account_id=?",Integer.class,staffId)>0)
            throw new BusinessException("该员工已绑定微信，不能重复绑定");
        var result=accounts.bind(wechat.exchange("MINI",code),staffId);
        membership.synchronize(staffId);
        jdbc.update("DELETE FROM t_wechat_flow WHERE token_hash=? AND kind='STAFF_QR'",WechatFlows.hash(ticket));
        return result;
    }

    private Map<String,Object> requireStaff(Long staffId) {
        var rows=jdbc.queryForList("SELECT phone,status FROM t_staff WHERE id=? AND deleted=0 FOR UPDATE",staffId);
        if(rows.isEmpty() || ((Number)rows.get(0).get("status")).intValue()!=1)throw new BusinessException("员工不存在或已停用");
        return rows.get(0);
    }
}
