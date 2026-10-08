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
import java.util.HashSet;

/** Only an existing WeChat identity link may select an existing member; never match by phone alone. */
@Service @RequiredArgsConstructor
public class StaffWechatMembership {
    private final JdbcTemplate jdbc;
    private final MemberMapper members;
    private final MemberAccountMapper wallets;
    private final BusinessDictionary businessDictionary;


    @Transactional
    public Long synchronize(Long staffId) {
        businessDictionary.requireEnabled();
        var staffRows=jdbc.queryForList("SELECT name,phone FROM t_staff WHERE id=? AND status=1 AND deleted=0 FOR UPDATE",staffId);
        if(staffRows.size()!=1)throw new BusinessException("员工不存在或已停用");
        var keys=jdbc.queryForList("SELECT identity_key FROM t_wechat_account WHERE audience='STAFF' AND account_id=? ORDER BY identity_key",String.class,staffId);
        if(keys.isEmpty())throw new BusinessException("请先绑定员工微信");
        var linked=new HashSet<Long>();
        for(var key:keys)linked.addAll(jdbc.queryForList("SELECT account_id FROM t_wechat_account WHERE audience='CUSTOMER' AND identity_key=?",Long.class,key));
        if(linked.size()>1)throw new BusinessException("微信关联了不同会员档案，请联系管理员核实");
        String phone=(String)staffRows.get(0).get("phone"),name=(String)staffRows.get(0).get("name");
        Long id=linked.isEmpty()?null:linked.iterator().next();
        var phoneOwners=jdbc.queryForList("SELECT id FROM t_member WHERE phone=? AND deleted=0",Long.class,phone);
        if(phoneOwners.stream().anyMatch(owner->!owner.equals(id)))throw new BusinessException("该手机号已有其他会员档案，请联系管理员核实绑定，原余额不变");
        Member member;
        if(id==null) {
            member=new Member();member.setMemberNo(OrderNoUtil.generate("M"));member.setName(name);member.setPhone(phone);
            member.setLevel("NORMAL");member.setDiscount(100);member.setPoints(0);member.setStatus(1);members.insert(member);
            var wallet=new MemberAccount();wallet.setMemberId(member.getId());wallet.setBalance(BigDecimal.ZERO);
            wallet.setTotalRecharge(BigDecimal.ZERO);wallet.setTotalConsume(BigDecimal.ZERO);wallet.setVersion(0);wallets.insert(wallet);
        } else {
            jdbc.queryForList("SELECT id FROM t_member WHERE id=? FOR UPDATE",id);
            member=members.selectById(id);
            if(member==null || !Integer.valueOf(1).equals(member.getStatus()))throw new BusinessException("原会员已停用，请联系管理员核实");
            if(member.getPhone()!=null && !member.getPhone().isBlank() && !member.getPhone().equals(phone))
                throw new BusinessException("员工手机号与原会员资料不一致，请联系管理员核实");
            // Update profile columns only; balances, membership level, points and orders remain untouched.
            jdbc.update("UPDATE t_member SET name=CASE WHEN name IS NULL OR name='' OR name='微信顾客' THEN ? ELSE name END, phone=? WHERE id=?",name,phone,id);
        }
        for(var key:keys) {
            var existing=jdbc.queryForList("SELECT account_id FROM t_wechat_account WHERE audience='CUSTOMER' AND identity_key=?",Long.class,key);
            if(existing.isEmpty())jdbc.update("INSERT INTO t_wechat_account(identity_key,audience,account_id) VALUES(?,'CUSTOMER',?)",key,member.getId());
            else if(!existing.get(0).equals(member.getId()))throw new BusinessException("微信会员绑定冲突，请重新登录");
        }
        return member.getId();
    }
}
