package com.bama.store.controller;

import com.bama.store.common.Result;
import com.bama.store.entity.Store;
import com.bama.store.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 门店设置（后台）
 */
@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreMapper storeMapper;
    private final com.bama.store.service.AuditService audit;

    @GetMapping
    public Result<java.util.List<Store>> list() {
        var user = com.bama.store.security.SecurityUtil.current();
        var q = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Store>();
        if (!user.getPermissions().contains("store:all")) q.eq(Store::getId, user.getStoreId());
        return Result.success(storeMapper.selectList(q.orderByAsc(Store::getId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('store:all')")
    @org.springframework.transaction.annotation.Transactional
    public Result<Long> create(@RequestBody Store body) {
        validate(body);
        Store store = new Store();
        store.setName(body.getName().trim()); store.setAddress(body.getAddress()); store.setPhone(body.getPhone());
        store.setOpenTime(body.getOpenTime()); store.setCloseTime(body.getCloseTime());
        store.setReservationNotice(body.getReservationNotice()); store.setStatus(body.getStatus());
        storeMapper.insert(store);
        audit.record("新增分店", store.getId(), store.getName());
        return Result.success(store.getId());
    }

    private void validate(Store body) {
        if (body.getName() == null || body.getName().isBlank() || body.getName().length() > 64) throw new com.bama.store.common.BusinessException("请填写有效门店名称");
        if (body.getStatus() == null || body.getStatus() != 0 && body.getStatus() != 1) throw new com.bama.store.common.BusinessException("营业状态无效");
        if (com.bama.store.service.BookingRules.minute(body.getCloseTime()) <= com.bama.store.service.BookingRules.minute(body.getOpenTime())) throw new com.bama.store.common.BusinessException("营业结束时间须晚于开始时间");
        if (body.getReservationNotice() != null && body.getReservationNotice().length() > 1000) throw new com.bama.store.common.BusinessException("预约须知最多1000字");
    }

    /** 门店详情 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('store:manage')")
    public Result<Store> detail(@PathVariable Long id) {
        com.bama.store.security.SecurityUtil.ownStore(id);
        return Result.success(storeMapper.selectById(id));
    }

    /** 修改门店名称 / 地址 / 电话（需门店设置权限） */
    @PutMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    @PreAuthorize("hasAuthority('store:manage')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Store body) {
        com.bama.store.security.SecurityUtil.ownStore(id);
        if (body.getName() == null || body.getName().isBlank() || body.getName().length() > 64) throw new com.bama.store.common.BusinessException("请填写有效门店名称");
        if (body.getStatus() == null || body.getStatus() != 0 && body.getStatus() != 1) throw new com.bama.store.common.BusinessException("营业状态无效");
        if (com.bama.store.service.BookingRules.minute(body.getCloseTime()) <= com.bama.store.service.BookingRules.minute(body.getOpenTime())) throw new com.bama.store.common.BusinessException("营业结束时间须晚于开始时间");
        if (body.getReservationNotice() != null && body.getReservationNotice().length() > 1000) throw new com.bama.store.common.BusinessException("预约须知最多1000字");
        Store before = storeMapper.selectById(id);
        if (before == null) throw new com.bama.store.common.BusinessException("门店不存在");
        Store s = new Store();
        s.setId(id);
        s.setName(body.getName());
        s.setAddress(body.getAddress());
        s.setPhone(body.getPhone());
        s.setStatus(body.getStatus());
        s.setOpenTime(body.getOpenTime()); s.setCloseTime(body.getCloseTime()); s.setReservationNotice(body.getReservationNotice());
        storeMapper.updateById(s);
        audit.record("门店设置", id, before.getName() + " / " + before.getOpenTime() + "–" + before.getCloseTime() + " / 状态" + before.getStatus() + " → " + s.getName() + " / " + s.getOpenTime() + "–" + s.getCloseTime() + " / 状态" + s.getStatus());
        return Result.success();
    }
}
