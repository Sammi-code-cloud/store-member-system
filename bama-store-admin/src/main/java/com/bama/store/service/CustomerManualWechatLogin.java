package com.bama.store.service;

import com.bama.store.common.BusinessException;
import com.bama.store.common.OrderNoUtil;
import com.bama.store.entity.Member;
import com.bama.store.entity.MemberAccount;
import com.bama.store.mapper.MemberMapper;
import com.bama.store.mapper.MemberAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

/** 微信身份用于认证，手填手机号仅作为联系资料，不能凭号码认领现有会员。 */
@Service @RequiredArgsConstructor
public class CustomerManualWechatLogin {
    private final JdbcTemplate jdbc;
    private final MemberMapper members;
    private final MemberAccountMapper wallets;
    private final CustomerAuthService customers;

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> login(WechatClient.Identity identity,String phone,String name) {
        if(phone!=null && !phone.isBlank() && !phone.matches("1[3-9]\\d{9}"))throw new BusinessException("请输入正确的11位手机号");
        name=name==null?"":name.trim();
        if(name.length()>32)throw new BusinessException("称呼最多32个字");
        var keys=new ArrayList<String>();
        keys.add("APP:"+WechatFlows.hash(identity.appId()+":"+identity.openId()));
        if(identity.unionId()!=null&&!identity.unionId().isBlank())keys.add("UNION:"+WechatFlows.hash(identity.unionId()));
        Collections.sort(keys);
        var ids=new HashSet<Long>();
        for(String key:keys)ids.addAll(jdbc.queryForList("SELECT account_id FROM t_wechat_account WHERE identity_key=? AND audience='CUSTOMER' FOR UPDATE",Long.class,key));
        if(ids.size()>1)throw new BusinessException("微信会员关联冲突，请联系门店核实");
        Long id=ids.isEmpty()?null:ids.iterator().next();
        // Returning members authenticate through the verified WeChat identity, never a typed phone.
        if(phone==null || phone.isBlank()) {
            if(id!=null) {
                jdbc.queryForList("SELECT id FROM t_member WHERE id=? FOR UPDATE",id);
                Member existing=members.selectById(id);
                if(existing==null || !Integer.valueOf(1).equals(existing.getStatus()))throw new BusinessException("会员已停用，请联系门店");
                if(existing.getPhone()!=null && existing.getPhone().matches("1[3-9]\\d{9}"))
                    return Map.of("audience","CUSTOMER","bindRequired",false,"account",customers.wechatLogin(id));
            }
            return Map.of("audience","CUSTOMER","manualPhoneRequired",true);
        }

        var phoneOwners=jdbc.queryForList("SELECT id FROM t_member WHERE phone=? AND deleted=0 FOR UPDATE",Long.class,phone);
        if(phoneOwners.stream().anyMatch(owner->!owner.equals(id)))throw new BusinessException("该手机号已有会员档案，请联系门店核实微信绑定，原余额不变");
        Member member;
        if(id!=null){
            jdbc.queryForList("SELECT id FROM t_member WHERE id=? FOR UPDATE",id);
            member=members.selectById(id);
            if(member==null || !Integer.valueOf(1).equals(member.getStatus()))throw new BusinessException("会员已停用，请联系门店");
            if(member.getPhone()!=null&&!member.getPhone().isBlank()&&!phone.equals(member.getPhone()))throw new BusinessException("手机号与已绑定会员不一致，请核对或联系门店修改");
            jdbc.update("UPDATE t_member SET phone=? WHERE id=?",phone,id);
            if(!name.isBlank()&&(member.getName()==null||member.getName().isBlank()||"微信顾客".equals(member.getName())))
                jdbc.update("UPDATE t_member SET name=? WHERE id=?",name,id);
        }else{
            member=new Member();member.setMemberNo(OrderNoUtil.generate("M"));member.setName(name.isBlank()?"微信顾客":name);member.setPhone(phone);
            member.setLevel("NORMAL");member.setDiscount(100);member.setPoints(0);member.setStatus(1);members.insert(member);
            MemberAccount wallet=new MemberAccount();wallet.setMemberId(member.getId());wallet.setBalance(BigDecimal.ZERO);wallet.setTotalRecharge(BigDecimal.ZERO);wallet.setTotalConsume(BigDecimal.ZERO);wallet.setVersion(0);wallets.insert(wallet);
        }
        for(String key:keys){
            var linked=jdbc.queryForList("SELECT account_id FROM t_wechat_account WHERE identity_key=? AND audience='CUSTOMER'",Long.class,key);
            if(linked.isEmpty())jdbc.update("INSERT INTO t_wechat_account(identity_key,audience,account_id) VALUES(?,'CUSTOMER',?)",key,member.getId());
            else if(!linked.get(0).equals(member.getId()))throw new BusinessException("微信会员关联冲突，请重新登录");
        }
        return Map.of("audience","CUSTOMER","bindRequired",false,"account",customers.wechatLogin(member.getId()));
    }
}
