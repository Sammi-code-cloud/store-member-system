package com.bama.store.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bama.store.common.*;
import com.bama.store.entity.*;
import com.bama.store.mapper.*;
import com.bama.store.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecordsController {
    private final WalletTxnMapper transactions;
    private final MemberMapper members;
    private final StaffMapper staff;
    private final AuditLogMapper logs;
    private final StoreMapper stores;

    /** 普通员工仅查看自己经办的记录，资金流水权限允许查看当前门店全部。 */
    @GetMapping("/staff/transactions")
    @PreAuthorize("hasAnyAuthority('charge:scan','account:recharge','account:view')")
    public Result<PageResult<WalletTxn>> staffTransactions(@RequestParam(defaultValue="1") long pageNum,
            @RequestParam(defaultValue="20") long pageSize, @RequestParam(required=false) String type,
            @RequestParam(required=false) String keyword, @RequestParam(required=false) LocalDate startDate,
            @RequestParam(required=false) LocalDate endDate, @RequestParam(defaultValue="mine") String scope) {
        if (!java.util.Set.of("mine","store").contains(scope)) throw new BusinessException("无效的记录范围");
        if (type != null && !type.isBlank() && !java.util.Set.of("RECHARGE","GIFT","CONSUME","REFUND").contains(type)) throw new BusinessException("无效的流水类型");
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) throw new BusinessException("开始日期不能晚于结束日期");
        if ("store".equals(scope) && org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().noneMatch(a -> "account:view".equals(a.getAuthority()))) throw new BusinessException(ResultCode.FORBIDDEN);
        return Result.success(PageResult.of(query(pageNum,pageSize,type,keyword,startDate,endDate,null,
                "mine".equals(scope) ? SecurityUtil.staffId() : null)));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAuthority('account:view')")
    public Result<PageResult<WalletTxn>> transactions(@RequestParam(defaultValue="1") long pageNum, @RequestParam(defaultValue="20") long pageSize,
            @RequestParam(required=false) String type, @RequestParam(required=false) String keyword,
            @RequestParam(required=false) LocalDate startDate, @RequestParam(required=false) LocalDate endDate,
            @RequestParam(required=false) Long memberId) {
        return Result.success(PageResult.of(query(pageNum, pageSize, type, keyword, startDate, endDate, memberId, null)));
    }

    @GetMapping("/members/{id}/transactions")
    @PreAuthorize("hasAuthority('member:view')")
    public Result<PageResult<WalletTxn>> memberTransactions(@PathVariable Long id, @RequestParam(defaultValue="1") long pageNum, @RequestParam(defaultValue="10") long pageSize) {
        if (members.selectById(id) == null) throw new BusinessException("顾客不存在");
        return Result.success(PageResult.of(query(pageNum, pageSize, null, null, null, null, id, null)));
    }

    private Page<WalletTxn> query(long pageNum, long pageSize, String type, String keyword, LocalDate startDate, LocalDate endDate, Long memberId, Long operatorId) {
        var q = new LambdaQueryWrapper<WalletTxn>().eq(WalletTxn::getStoreId, SecurityUtil.storeId()).eq(memberId != null, WalletTxn::getMemberId, memberId)
                .eq(type != null && !type.isBlank(), WalletTxn::getType, type).eq(operatorId != null, WalletTxn::getStaffId, operatorId);
        if (startDate != null) q.ge(WalletTxn::getCreateTime, startDate.atStartOfDay());
        if (endDate != null) q.lt(WalletTxn::getCreateTime, endDate.plusDays(1).atStartOfDay());
        if (keyword != null && !keyword.isBlank()) {
            var ids = members.selectList(new LambdaQueryWrapper<Member>().and(w -> w.like(Member::getName, keyword).or().like(Member::getPhone, keyword).or().like(Member::getMemberNo, keyword))).stream().map(Member::getId).toList();
            q.and(w -> { w.like(WalletTxn::getBizNo, keyword).or().like(WalletTxn::getRefOrderNo, keyword); if (!ids.isEmpty()) w.or().in(WalletTxn::getMemberId, ids); });
        }
        var page = transactions.selectPage(new Page<WalletTxn>(Math.max(1, pageNum), Math.min(200, Math.max(1, pageSize))), q.orderByDesc(WalletTxn::getId));
        Store store = stores.selectById(SecurityUtil.storeId());
        for (WalletTxn row : page.getRecords()) {
            Member member = members.selectById(row.getMemberId()); Staff operator = row.getStaffId() == null ? null : staff.selectById(row.getStaffId());
            row.setMemberPhone(member == null ? null : member.getPhone()); row.setStoreName(store == null ? "已归档门店" : store.getName());
            row.setMemberName(member == null ? "已归档顾客" : member.getName()); row.setStaffName(operator == null ? "—" : operator.getName());
        }
        return page;
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAuthority('audit:view')")
    public Result<PageResult<AuditLog>> logs(@RequestParam(defaultValue="1") long pageNum, @RequestParam(defaultValue="20") long pageSize,
            @RequestParam(required=false) String keyword, @RequestParam(required=false) LocalDate startDate, @RequestParam(required=false) LocalDate endDate) {
        var q = new LambdaQueryWrapper<AuditLog>().eq(AuditLog::getStoreId, SecurityUtil.storeId());
        if (keyword != null && !keyword.isBlank()) q.and(w -> w.like(AuditLog::getActor, keyword).or().like(AuditLog::getAction, keyword).or().like(AuditLog::getTarget, keyword));
        if (startDate != null) q.ge(AuditLog::getCreateTime, startDate.atStartOfDay());
        if (endDate != null) q.lt(AuditLog::getCreateTime, endDate.plusDays(1).atStartOfDay());
        return Result.success(PageResult.of(logs.selectPage(new Page<>(Math.max(1, pageNum), Math.min(200, Math.max(1, pageSize))), q.orderByDesc(AuditLog::getId))));
    }
}
