package com.bama.store.service;

import com.bama.store.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessDictionary {
    public static final String KEY = "business_enabled";
    private final JdbcTemplate jdbc;
    private final AuditService audit;

    public boolean enabled() {
        var values = jdbc.queryForList("SELECT dict_value FROM t_business_dictionary WHERE dict_key=? AND deleted=0", String.class, KEY);
        return values.size() == 1 && "1".equals(values.get(0));
    }

    /** Serialize switch changes with writes; the lock lasts until the business transaction commits. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireEnabled() {
        if (!lockEnabled()) {
            throw new BusinessException("系统正在维护");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean lockEnabled() {
        return "1".equals(lockedValue());
    }

    private String lockedValue() {
        var values = jdbc.queryForList("SELECT dict_value FROM t_business_dictionary WHERE dict_key=? AND deleted=0 FOR UPDATE", String.class, KEY);
        return values.size() == 1 ? values.get(0) : null;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(boolean enabled) {
        String previous = lockedValue();
        if (previous == null) throw new BusinessException("业务字典缺失，请联系管理员修复配置");
        String next = enabled ? "1" : "0";
        if (next.equals(previous)) return;
        jdbc.update("UPDATE t_business_dictionary SET dict_value=?,update_time=CURRENT_TIMESTAMP WHERE dict_key=? AND deleted=0", next, KEY);
        audit.record("修改业务总开关", KEY, ("1".equals(previous) ? "开启" : "关闭") + " → " + (enabled ? "开启" : "关闭") + "；全局控制预约、充值、扣减、注册、微信绑定、通知订阅和业务资料新增、编辑、状态变更和删除");
    }
}
