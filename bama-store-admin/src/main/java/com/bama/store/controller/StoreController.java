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

    /** 门店详情 */
    @GetMapping("/{id}")
    public Result<Store> detail(@PathVariable Long id) {
        return Result.success(storeMapper.selectById(id));
    }

    /** 修改门店名称 / 地址 / 电话（需门店设置权限） */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('store:manage')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Store body) {
        Store s = new Store();
        s.setId(id);
        s.setName(body.getName());
        s.setAddress(body.getAddress());
        s.setPhone(body.getPhone());
        storeMapper.updateById(s);
        return Result.success();
    }
}
