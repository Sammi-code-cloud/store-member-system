package com.bama.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bama.store.common.BusinessException;
import com.bama.store.common.OrderNoUtil;
import com.bama.store.common.ResultCode;
import com.bama.store.dto.ChargeConfirmRequest;
import com.bama.store.dto.ChargeItemDto;
import com.bama.store.dto.RechargeRequest;
import com.bama.store.entity.*;
import com.bama.store.mapper.*;
import com.bama.store.vo.ChargeResultVo;
import com.bama.store.vo.MemberChargeInfoVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 会员账户资金服务：储值、扫码解析、扣款
 * 资金变动均在事务内，配合乐观锁与幂等号，保证一致性。
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final MemberMapper memberMapper;
    private final MemberAccountMapper accountMapper;
    private final WalletTxnMapper walletTxnMapper;
    private final ConsumeOrderMapper consumeOrderMapper;
    private final ConsumeItemMapper consumeItemMapper;
    private final PayCodeService payCodeService;
    private final AuditService audit;
    private final StoreAvailability storeAvailability;
    private final ChargeIdempotency chargeIdempotency;
    private final WalletNotices walletNotices;
    private final BusinessDictionary businessDictionary;

    /** 扫码解析付款码 → 会员信息（付款码一次性失效） */
    public MemberChargeInfoVo resolvePayCode(String payCode) {
        Long memberId = payCodeService.resolve(payCode);
        if (memberId == null) {
            throw new BusinessException(ResultCode.PAYCODE_INVALID);
        }
        return chargeInfo(memberMapper.selectById(memberId));
    }

    public MemberChargeInfoVo resolvePhone(String phone) {
        if (phone == null || !phone.matches("1[3-9]\\d{9}")) throw new BusinessException("请输入正确的11位手机号");
        var matches = memberMapper.selectList(new LambdaQueryWrapper<Member>().eq(Member::getPhone, phone).eq(Member::getStatus, 1));
        if (matches.size() != 1) throw new BusinessException("未找到唯一可用会员，请核对手机号");
        return chargeInfo(matches.get(0));
    }

    private MemberChargeInfoVo chargeInfo(Member member) {
        if (member == null || !Integer.valueOf(1).equals(member.getStatus())) {
            throw new BusinessException(ResultCode.MEMBER_NOT_FOUND);
        }
        MemberAccount account = loadAccount(member.getId());

        MemberChargeInfoVo vo = new MemberChargeInfoVo();
        vo.setMemberId(member.getId());
        vo.setMemberNo(member.getMemberNo());
        vo.setName(member.getName());
        vo.setPhone(member.getPhone());
        vo.setLevel(member.getLevel());
        vo.setDiscount(member.getDiscount());
        vo.setBalance(account.getBalance());
        return vo;
    }

    /** 会员储值 */
    @Transactional(rollbackFor = Exception.class)
    public void recharge(RechargeRequest req, Long staffId, Long storeId) {
        businessDictionary.requireEnabled();
        storeAvailability.lockActive(storeId);
        Member member = memberMapper.selectById(req.getMemberId());
        if (member == null || !Integer.valueOf(1).equals(member.getStatus())) {
            throw new BusinessException(ResultCode.MEMBER_NOT_FOUND);
        }
        MemberAccount account = loadAccount(req.getMemberId());

        BigDecimal amount = req.getAmount();
        BigDecimal gift = req.getGiftAmount() == null ? BigDecimal.ZERO : req.getGiftAmount();
        BigDecimal before = account.getBalance();
        BigDecimal afterPrincipal = before.add(amount);
        BigDecimal after = afterPrincipal.add(gift);

        // 乐观锁更新余额
        account.setBalance(after);
        account.setTotalRecharge(account.getTotalRecharge().add(amount));
        if (accountMapper.updateById(account) == 0) {
            throw new BusinessException(ResultCode.DUPLICATE_SUBMIT);
        }

        // 本金流水
        String rechargeNo = OrderNoUtil.generate("RC");
        saveTxn(member.getId(), rechargeNo, "RECHARGE", amount,
                before, afterPrincipal, null, staffId, storeId, req.getRemark());
        // 赠送流水
        if (gift.compareTo(BigDecimal.ZERO) > 0) {
            saveTxn(member.getId(), OrderNoUtil.generate("GF"), "GIFT", gift,
                    afterPrincipal, after, null, staffId, storeId, "储值赠送");
        }
        audit.record("代客充值", member.getMemberNo(), "本金 ¥" + amount + "，赠送 ¥" + gift + "，余额 " + before + " → " + after);
        walletNotices.enqueue("RECHARGE",rechargeNo,member.getId(),storeId,amount,gift,after);
    }

    /** 扫码扣款（核心） */
    @Transactional(rollbackFor = Exception.class)
    public ChargeResultVo charge(ChargeConfirmRequest req, Long staffId, Long storeId, String staffName) {
        businessDictionary.requireEnabled();
        storeAvailability.lockActive(storeId);
        Member member = memberMapper.selectById(req.getMemberId());
        if (member == null || !Integer.valueOf(1).equals(member.getStatus())) {
            throw new BusinessException(ResultCode.MEMBER_NOT_FOUND);
        }

        if (req.getExpectedPhone() != null && !req.getExpectedPhone().equals(member.getPhone())) {
            throw new BusinessException("会员手机号已变化，请重新查询");
        }
        boolean manual = req.getAmount() != null;
        java.util.List<ChargeItemDto> items = req.getItems();
        if (manual) {
            if (items != null && !items.isEmpty()) throw new BusinessException("手动金额与消费明细不能同时提交");
            checkAmount(req.getAmount());
            if (!StringUtils.hasText(req.getBizNo()) || req.getBizNo().length() > 64) throw new BusinessException("手动扣款缺少有效业务号");
            ChargeItemDto item = new ChargeItemDto();
            item.setItemType("MANUAL"); item.setItemName("手动金额扣款");
            item.setPrice(req.getAmount()); item.setQuantity(1);
            items = java.util.List.of(item);
        } else {
            if (items == null || items.isEmpty()) throw new BusinessException("消费明细不能为空");
            for (ChargeItemDto item : items) {
                if (item == null || !StringUtils.hasText(item.getItemName()) || item.getItemName().length() > 128
                    || !("PRODUCT".equals(item.getItemType()) || "ROOM".equals(item.getItemType()))
                    || (item.getQuantity() != null && item.getQuantity() <= 0)) throw new BusinessException("消费明细无效");
                checkAmount(item.getPrice());
            }
        }

        String bizNo = req.getBizNo();
        ChargeResultVo previous = chargeIdempotency.begin(req, items, staffId, storeId, member.getName(), staffName);
        if (previous != null) return previous;

        // 计算原价与折后应扣
        BigDecimal origin = BigDecimal.ZERO;
        for (ChargeItemDto item : items) {
            int qty = item.getQuantity() == null ? 1 : item.getQuantity();
            origin = origin.add(item.getPrice().multiply(BigDecimal.valueOf(qty)));
        }
        int discount = member.getDiscount() == null ? 100 : member.getDiscount();
        BigDecimal pay = manual ? req.getAmount().setScale(2) : origin.multiply(BigDecimal.valueOf(discount))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        checkAmount(pay);
        MemberAccount account = loadAccount(req.getMemberId());
        if (account.getBalance().compareTo(pay) < 0) {
            throw new BusinessException(ResultCode.BALANCE_NOT_ENOUGH);
        }

        BigDecimal before = account.getBalance();
        BigDecimal after = before.subtract(pay);

        // 乐观锁扣减余额
        account.setBalance(after);
        account.setTotalConsume(account.getTotalConsume().add(pay));
        if (accountMapper.updateById(account) == 0) {
            // 版本冲突，说明并发修改，回滚
            throw new BusinessException(ResultCode.DUPLICATE_SUBMIT);
        }

        // 消费单
        String orderNo = OrderNoUtil.generate("CO");
        ConsumeOrder order = new ConsumeOrder();
        order.setOrderNo(orderNo);
        order.setMemberId(member.getId());
        order.setStaffId(staffId);
        order.setStoreId(storeId);
        order.setOriginAmount(origin);
        order.setPayAmount(pay);
        order.setPayType("MEMBER_CARD");
        order.setStatus("PAID");
        order.setRemark(req.getRemark());
        consumeOrderMapper.insert(order);

        // 明细
        for (ChargeItemDto item : items) {
            int qty = item.getQuantity() == null ? 1 : item.getQuantity();
            ConsumeItem ci = new ConsumeItem();
            ci.setOrderId(order.getId());
            ci.setItemType(item.getItemType());
            ci.setItemName(item.getItemName());
            ci.setPrice(item.getPrice());
            ci.setQuantity(qty);
            ci.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(qty)));
            consumeItemMapper.insert(ci);
        }

        // 资金流水（携带幂等号）
        saveTxn(member.getId(), bizNo, "CONSUME", pay, before, after, orderNo, staffId, storeId, req.getRemark());
        audit.record("消费扣款", orderNo, "顾客 " + member.getMemberNo() + "，¥" + pay + "，余额 " + before + " → " + after);

        ChargeResultVo vo = new ChargeResultVo();
        vo.setOrderNo(orderNo);
        vo.setMemberId(member.getId());
        vo.setMemberName(member.getName());
        vo.setOriginAmount(origin);
        vo.setPayAmount(pay);
        vo.setBalanceAfter(after);
        vo.setStaffName(staffName);
        vo.setTime(LocalDateTime.now());
        chargeIdempotency.complete(bizNo, vo);
        walletNotices.enqueue("CHARGE",orderNo,member.getId(),storeId,pay,BigDecimal.ZERO,after);
        return vo;
    }

    private void checkAmount(BigDecimal value) {
        if (value == null || value.signum() <= 0 || value.stripTrailingZeros().scale() > 2
            || value.compareTo(new BigDecimal("99999999.99")) > 0) throw new BusinessException("请输入有效金额，最多两位小数");
    }

    private MemberAccount loadAccount(Long memberId) {
        MemberAccount account = accountMapper.selectOne(
                new LambdaQueryWrapper<MemberAccount>().eq(MemberAccount::getMemberId, memberId));
        if (account == null) {
            throw new BusinessException(ResultCode.MEMBER_NOT_FOUND);
        }
        return account;
    }

    private void saveTxn(Long memberId, String bizNo, String type, BigDecimal amount,
                         BigDecimal before, BigDecimal after, String refOrderNo,
                         Long staffId, Long storeId, String remark) {
        WalletTxn txn = new WalletTxn();
        txn.setMemberId(memberId);
        txn.setBizNo(bizNo);
        txn.setType(type);
        txn.setAmount(amount);
        txn.setBalanceBefore(before);
        txn.setBalanceAfter(after);
        txn.setRefOrderNo(refOrderNo);
        txn.setStaffId(staffId);
        txn.setStoreId(storeId);
        txn.setRemark(remark);
        walletTxnMapper.insert(txn);
    }
}
