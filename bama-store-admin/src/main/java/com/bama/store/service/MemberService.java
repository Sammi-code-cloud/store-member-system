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

    public Page<Member> page(long pageNum, long pageSize, String keyword) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Member::getName, keyword).or().like(Member::getPhone, keyword);
        }
        wrapper.orderByDesc(Member::getId);
        return memberMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
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
}
