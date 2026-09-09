package com.bama.store.service;

import com.bama.store.common.*;
import com.bama.store.entity.*;
import com.bama.store.mapper.*;
import com.bama.store.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.sql.Timestamp;
import java.security.SecureRandom;
import java.util.*;

@Service @RequiredArgsConstructor
public class MemberEnrollment {
    private final JdbcTemplate jdbc;
    private final MemberMapper members;
    private final MemberAccountMapper wallets;
    private final CustomerAuthService customers;
    private final WechatClient wechat;
    private final AuditService audit;

    @Transactional
    public Map<String,Object> create(String name,String phone,String remark){
        if(name==null||name.trim().isEmpty()||name.trim().length()>32)throw new BusinessException("请填写1–32字会员姓名");
        if(phone==null||!phone.matches("1[3-9]\\d{9}"))throw new BusinessException("请输入正确的11位手机号");
        if(remark!=null&&remark.length()>500)throw new BusinessException("备注最多500字");
        if(!jdbc.queryForList("SELECT id FROM t_member WHERE phone=? AND deleted=0",Long.class,phone).isEmpty())throw new BusinessException("该手机号已有会员，请直接使用已有档案");
        Member member=new Member();member.setMemberNo(OrderNoUtil.generate("M"));member.setName(name.trim());member.setPhone(phone);member.setRemark(remark);
        member.setStatus(1);member.setLevel("NORMAL");member.setDiscount(100);member.setPoints(0);members.insert(member);
        MemberAccount wallet=new MemberAccount();wallet.setMemberId(member.getId());wallet.setBalance(BigDecimal.ZERO);wallet.setTotalRecharge(BigDecimal.ZERO);wallet.setTotalConsume(BigDecimal.ZERO);wallet.setVersion(0);wallets.insert(wallet);
        audit.record("新增会员",member.getMemberNo(),"后台登记会员，初始余额0");
        return Map.of("id",member.getId(),"name",member.getName(),"phone",phone);
    }
    private Member lockMember(Long id){
        jdbc.queryForList("SELECT id FROM t_member WHERE id=? FOR UPDATE",id);
        Member member=members.selectById(id);
        if(member==null||!Integer.valueOf(1).equals(member.getStatus()))throw new BusinessException("会员不存在或已停用");
        return member;
    }
    @Transactional
    public Map<String,Object> code(Long id){
        Member member=lockMember(id);
        if(member.getPhone()==null||!member.getPhone().matches("1[3-9]\\d{9}"))throw new BusinessException("请先登记有效会员手机号");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM t_wechat_account WHERE audience='CUSTOMER' AND account_id=?",Integer.class,id)>0)
            return Map.of("bound",true,"memberName",member.getName(),"phone",member.getPhone());
        jdbc.update("DELETE FROM t_wechat_flow WHERE kind='MEMBER_QR' AND (payload LIKE ? OR expires_at<?)",id+"|%",Timestamp.from(Instant.now()));
        byte[] bytes=new byte[16];new SecureRandom().nextBytes(bytes);String ticket=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String payload=id+"|"+SecurityUtil.storeId()+"|"+SecurityUtil.staffId();
        jdbc.update("INSERT INTO t_wechat_flow(token_hash,kind,payload,expires_at) VALUES(?,'MEMBER_QR',?,?)",WechatFlows.hash(ticket),payload,Timestamp.from(Instant.now().plusSeconds(600)));
        String image=wechat.memberBindCode(ticket);
        audit.record("生成会员微信绑定码",member.getMemberNo(),"有效期10分钟，重新生成使旧码失效");
        return Map.of("bound",false,"image",image,"memberName",member.getName(),"phone",member.getPhone(),"expiresIn",600);
    }
    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> bind(String ticket,String phone,String code){
        if(ticket==null||!ticket.matches("[A-Za-z0-9_-]{22}"))throw new BusinessException("会员绑定码无效，请向门店重新获取");
        if(phone==null||!phone.matches("1[3-9]\\d{9}"))throw new BusinessException("请输入登记的11位会员手机号");
        var rows=jdbc.queryForList("SELECT payload FROM t_wechat_flow WHERE token_hash=? AND kind='MEMBER_QR' AND expires_at>?",String.class,WechatFlows.hash(ticket),Timestamp.from(Instant.now()));
        if(rows.size()!=1)throw new BusinessException("绑定码已过期或已使用，请门店重新生成");
        String[] payload=rows.get(0).split("\\|");Long id=Long.valueOf(payload[0]);
        Member member=lockMember(id);
        if(jdbc.queryForList("SELECT payload FROM t_wechat_flow WHERE token_hash=? AND kind='MEMBER_QR' AND expires_at>? FOR UPDATE",String.class,WechatFlows.hash(ticket),Timestamp.from(Instant.now())).isEmpty())throw new BusinessException("绑定码已过期或已使用，请门店重新生成");
        if(!phone.equals(member.getPhone()))throw new BusinessException("手机号与二维码对应会员不一致，请填写门店登记的手机号");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM t_wechat_account WHERE audience='CUSTOMER' AND account_id=?",Integer.class,id)>0)throw new BusinessException("该会员已绑定微信，不能重复绑定");
        var identity=wechat.exchange("MINI",code);
        var keys=new ArrayList<String>();keys.add("APP:"+WechatFlows.hash(identity.appId()+":"+identity.openId()));
        if(identity.unionId()!=null&&!identity.unionId().isBlank())keys.add("UNION:"+WechatFlows.hash(identity.unionId()));
        Collections.sort(keys);
        for(String key:keys){
            if(!jdbc.queryForList("SELECT account_id FROM t_wechat_account WHERE identity_key=? AND audience='CUSTOMER' FOR UPDATE",Long.class,key).isEmpty())throw new BusinessException("当前微信已有会员档案，请联系门店核实，原余额保留");
        }
        for(String key:keys)jdbc.update("INSERT INTO t_wechat_account(identity_key,audience,account_id) VALUES(?,'CUSTOMER',?)",key,id);
        jdbc.update("DELETE FROM t_wechat_flow WHERE token_hash=? AND kind='MEMBER_QR'",WechatFlows.hash(ticket));
        audit.record("会员绑定微信",member.getMemberNo(),"使用门店发放的绑定码完成绑定",Long.valueOf(payload[1]));
        return Map.of("audience","CUSTOMER","bindRequired",false,"account",customers.wechatLogin(id));
    }
}
