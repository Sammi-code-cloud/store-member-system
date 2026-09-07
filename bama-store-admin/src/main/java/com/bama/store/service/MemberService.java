package com.bama.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bama.store.common.BusinessException;
import com.bama.store.common.ResultCode;
import com.bama.store.entity.Member;
import com.bama.store.entity.MemberAccount;
import com.bama.store.mapper.MemberAccountMapper;
import com.bama.store.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 会员管理
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;
    private final MemberAccountMapper memberAccountMapper;
    private final AuditService audit;

    public Page<Member> page(long pageNum, long pageSize, String keyword) {
        return page(pageNum, pageSize, keyword, null, null, null, null);
    }

    public Page<Member> page(long pageNum, long pageSize, String keyword, String level, Integer status, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Member::getName, keyword).or().like(Member::getPhone, keyword).or().like(Member::getMemberNo, keyword).or().like(Member::getUsername, keyword));
        }
        wrapper.eq(StringUtils.hasText(level), Member::getLevel, level).eq(status != null, Member::getStatus, status);
        if (startDate != null) wrapper.ge(Member::getCreateTime, startDate.atStartOfDay());
        if (endDate != null) wrapper.lt(Member::getCreateTime, endDate.plusDays(1).atStartOfDay());
        wrapper.orderByDesc(Member::getId);
        Page<Member> page = memberMapper.selectPage(new Page<>(Math.max(1, pageNum), Math.min(200, Math.max(1, pageSize))), wrapper);
        var ids = page.getRecords().stream().map(Member::getId).toList();
        if (!ids.isEmpty()) {
            var accounts = memberAccountMapper.selectList(new LambdaQueryWrapper<MemberAccount>().in(MemberAccount::getMemberId, ids)).stream().collect(java.util.stream.Collectors.toMap(MemberAccount::getMemberId, a -> a));
            for (Member m : page.getRecords()) {
                MemberAccount account = accounts.get(m.getId());
                if (account != null) { m.setBalance(account.getBalance()); m.setTotalConsume(account.getTotalConsume()); }
            }
        }
        return page;
    }

    public Member getById(Long id) {
        Member member = memberMapper.selectById(id);
        if (member == null) {
            throw new BusinessException(ResultCode.MEMBER_NOT_FOUND);
        }
        return member;
    }

    /** 获取会员账户，不存在则抛异常 */
    public MemberAccount getAccount(Long memberId) {
        MemberAccount account = memberAccountMapper.selectOne(
                new LambdaQueryWrapper<MemberAccount>().eq(MemberAccount::getMemberId, memberId));
        if (account == null) {
            throw new BusinessException(ResultCode.MEMBER_NOT_FOUND);
        }
        return account;
    }

    @org.springframework.transaction.annotation.Transactional
    public void update(Long id, String name, String remark, Integer status) {
        Member member = getById(id);
        if (name == null || name.isBlank() || name.length() > 32) throw new BusinessException("姓名需为 1–32 字");
        if (remark != null && remark.length() > 500) throw new BusinessException("备注最多500字");
        if (status == null || status != 0 && status != 1) throw new BusinessException("状态无效");
        String before = member.getName() + " / 状态" + member.getStatus() + " / " + member.getRemark();
        Member update = new Member(); update.setId(id); update.setName(name.trim()); update.setRemark(remark == null ? "" : remark); update.setStatus(status);
        memberMapper.updateById(update);
        audit.record("编辑顾客", member.getMemberNo(), before + " → " + name.trim() + " / 状态" + status + " / " + update.getRemark());
    }
}
