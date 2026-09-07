package com.bama.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bama.store.common.BusinessException;
import com.bama.store.common.OrderNoUtil;
import com.bama.store.entity.Member;
import com.bama.store.entity.MemberAccount;
import com.bama.store.mapper.MemberMapper;
import com.bama.store.mapper.MemberAccountMapper;
import com.bama.store.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerAuthService {
    private final MemberMapper members;
    private final MemberAccountMapper accounts;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;

    @Transactional
    public Map<String, Object> register(String username, String password, String name) {
        username = normalize(username);
        if (password == null || password.length() < 8 || password.length() > 64) throw new BusinessException("密码需为 8–64 位");
        if (name == null || name.isBlank() || name.trim().length() > 32) throw new BusinessException("请填写 1–32 字的称呼");
        if (members.selectCount(new LambdaQueryWrapper<Member>().eq(Member::getUsername, username)) > 0) throw new BusinessException("账号已存在，请登录");
        Member member = new Member();
        member.setMemberNo(OrderNoUtil.generate("M")); member.setUsername(username);
        member.setPassword(encoder.encode(password)); member.setName(name.trim());
        member.setLevel("NORMAL"); member.setDiscount(100); member.setPoints(0); member.setStatus(1);
        member.setLastLoginTime(LocalDateTime.now(com.bama.store.service.BookingRules.ZONE));
        members.insert(member);
        MemberAccount account = new MemberAccount();
        account.setMemberId(member.getId()); account.setBalance(BigDecimal.ZERO);
        account.setTotalRecharge(BigDecimal.ZERO); account.setTotalConsume(BigDecimal.ZERO); account.setVersion(0);
        accounts.insert(account);
        return response(member);
    }

    @Transactional
    public Map<String, Object> login(String username, String password) {
        Member member = members.selectOne(new LambdaQueryWrapper<Member>().eq(Member::getUsername, normalize(username)));
        if (member == null || member.getPassword() == null || password == null || !encoder.matches(password, member.getPassword())) throw new BusinessException("账号或密码错误");
        if (!Integer.valueOf(1).equals(member.getStatus())) throw new BusinessException("账号已停用，请联系门店");
        Member update = new Member(); update.setId(member.getId()); update.setLastLoginTime(LocalDateTime.now(com.bama.store.service.BookingRules.ZONE));
        members.updateById(update);
        return response(member);
    }

    private String normalize(String username) {
        if (username == null || !username.trim().matches("[a-zA-Z0-9_]{4,32}")) throw new BusinessException("账号需为 4–32 位字母、数字或下划线");
        return username.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public Map<String,Object> wechatLogin(Long id) {
        if(id==null) throw new BusinessException("请先验证手机号并绑定微信");
        Member member=requireActive(id);
        member.setLastLoginTime(LocalDateTime.now(BookingRules.ZONE)); members.updateById(member);return response(member);
    }

    public Member requireActive(Long id) {
        Member member=members.selectById(id);
        if(member==null || !Integer.valueOf(1).equals(member.getStatus()))throw new BusinessException("账号已停用，请联系门店");
        return member;
    }

    @Transactional
    public Long createWechatMember(String phone) {
            Member member=new Member(); member.setMemberNo(OrderNoUtil.generate("M")); member.setName("微信顾客");
            member.setPhone(phone);
            member.setLevel("NORMAL"); member.setDiscount(100); member.setPoints(0); member.setStatus(1);
            member.setLastLoginTime(LocalDateTime.now(BookingRules.ZONE)); members.insert(member);
            MemberAccount account=new MemberAccount(); account.setMemberId(member.getId()); account.setBalance(BigDecimal.ZERO);
            account.setTotalRecharge(BigDecimal.ZERO); account.setTotalConsume(BigDecimal.ZERO); account.setVersion(0); accounts.insert(account);
            return member.getId();
    }

    private Map<String, Object> response(Member member) {
        return Map.of("token", jwt.generateCustomer(member.getId(), member.getName()), "memberId", member.getId(), "name", member.getName());
    }
}
