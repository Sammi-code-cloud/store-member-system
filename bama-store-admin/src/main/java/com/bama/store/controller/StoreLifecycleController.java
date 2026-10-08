package com.bama.store.controller;

import com.bama.store.common.BusinessException;
import com.bama.store.common.Result;
import com.bama.store.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/store") @RequiredArgsConstructor
@PreAuthorize("hasAuthority('store:all')")
public class StoreLifecycleController {
    private final com.bama.store.service.BusinessDictionary businessDictionary;
    private final JdbcTemplate jdbc;
    private final AuditService audit;
    public record StatusRequest(Integer status) {}

    @PutMapping("/{id}/status") @Transactional
    public Result<Void> status(@PathVariable Long id, @RequestBody StatusRequest body) {
        businessDictionary.requireEnabled();
        if (body.status() == null || body.status() != 0 && body.status() != 1)
            throw new BusinessException("营业状态无效");
        var rows = jdbc.queryForList("SELECT name,status FROM t_store WHERE id=? AND deleted=0 FOR UPDATE", id);
        if (rows.isEmpty()) throw new BusinessException("门店不存在或已删除");
        if (((Number) rows.get(0).get("status")).intValue() == body.status()) return Result.success();
        jdbc.update("UPDATE t_store SET status=?,update_time=CURRENT_TIMESTAMP WHERE id=? AND deleted=0", body.status(), id);
        audit.record(body.status() == 1 ? "启用分店" : "停用分店", id, rows.get(0).get("name").toString());
        return Result.success();
    }

    @DeleteMapping("/{id}") @Transactional(isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
    public Result<Void> delete(@PathVariable Long id) {
        businessDictionary.requireEnabled();
        var stores = jdbc.queryForList("SELECT id,name,status FROM t_store WHERE deleted=0 ORDER BY id FOR UPDATE");
        var store = stores.stream().filter(s -> ((Number) s.get("id")).longValue() == id).findFirst()
                .orElseThrow(() -> new BusinessException("门店不存在或已删除"));
        if (((Number) store.get("status")).intValue() != 0) throw new BusinessException("请先停用门店，再执行删除");
        if (stores.size() <= 1) throw new BusinessException("至少保留一家门店，不能删除最后一家门店");
        // Includes historical/soft-deleted rows: never discard the branch identity used by old records.
        for (String table : new String[]{"t_staff", "t_staff_store", "t_tea_room", "t_product", "t_banner",
                "t_reservation", "t_wallet_txn", "t_consume_order", "t_room_closure"}) {
            if (!jdbc.queryForList("SELECT store_id FROM " + table + " WHERE store_id=? LIMIT 1", id).isEmpty())
                throw new BusinessException("门店有关联员工、包间、商品或业务记录，不能删除；请保留停用状态以便查询历史记录");
        }
        jdbc.update("UPDATE t_store SET deleted=1,update_time=CURRENT_TIMESTAMP WHERE id=? AND deleted=0", id);
        audit.record("删除分店", id, store.get("name").toString());
        return Result.success();
    }
}
