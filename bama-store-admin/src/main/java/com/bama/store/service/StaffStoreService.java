package com.bama.store.service;

import com.bama.store.common.BusinessException;
import com.bama.store.common.ResultCode;
import com.bama.store.entity.Staff;
import com.bama.store.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

/** The home store is always accessible; additional stores are explicitly granted. */
@Service
@RequiredArgsConstructor
public class StaffStoreService {
    private final JdbcTemplate jdbc;

    public List<Long> assigned(Staff staff) {
        var ids = new TreeSet<Long>();
        if (staff.getStoreId() != null) ids.add(staff.getStoreId());
        ids.addAll(jdbc.queryForList("SELECT store_id FROM t_staff_store WHERE staff_id=?", Long.class, staff.getId()));
        return List.copyOf(ids);
    }

    public Set<Long> accessible(Staff staff, Set<String> permissions) {
        if (permissions.contains("store:all"))
            return new HashSet<>(jdbc.queryForList("SELECT id FROM t_store WHERE deleted=0", Long.class));
        var ids = new HashSet<>(assigned(staff));
        ids.retainAll(jdbc.queryForList("SELECT id FROM t_store WHERE deleted=0", Long.class));
        return ids;
    }

    public List<Long> staffInStore(Long storeId) {
        return jdbc.queryForList("SELECT staff_id FROM t_staff_store WHERE store_id=?", Long.class, storeId);
    }

    public void requireManageable(Staff staff) {
        if (!SecurityUtil.current().getStoreIds().containsAll(assigned(staff)))
            throw new BusinessException(ResultCode.FORBIDDEN);
    }

    public List<Long> validate(List<Long> requested, Long homeStore) {
        if (requested == null || requested.isEmpty() || requested.stream().anyMatch(Objects::isNull))
            throw new BusinessException("请至少选择一个可访问分店");
        var ids = new TreeSet<>(requested);
        if (!ids.contains(homeStore)) throw new BusinessException("可访问分店必须包含员工所属分店");
        if (!SecurityUtil.current().getStoreIds().containsAll(ids))
            throw new BusinessException("不能分配超出本人范围的分店权限");
        return List.copyOf(ids);
    }

    // Called inside StaffService's transaction so profile, roles and store grants change together.
    public void assign(Long staffId, List<Long> storeIds) {
        jdbc.update("DELETE FROM t_staff_store WHERE staff_id=?", staffId);
        for (Long storeId : storeIds)
            jdbc.update("INSERT INTO t_staff_store(staff_id,store_id) VALUES (?,?)", staffId, storeId);
    }
}
