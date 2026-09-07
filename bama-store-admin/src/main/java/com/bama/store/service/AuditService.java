package com.bama.store.service;

import com.bama.store.entity.AuditLog;
import com.bama.store.mapper.AuditLogMapper;
import com.bama.store.security.LoginStaff;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogMapper mapper;

    public void record(String action, Object target, String detail) {
        record(action, target, detail, null);
    }

    public void record(String action, Object target, String detail, Long storeId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        AuditLog log = new AuditLog();
        if (authentication != null && authentication.getPrincipal() instanceof LoginStaff staff) {
            log.setStaffId(staff.getStaffId());
            log.setStoreId(staff.getStoreId());
            log.setActor(staff.getName());
        } else {
            log.setActor("顾客");
            log.setStoreId(storeId);
        }
        log.setAction(action);
        log.setTarget(String.valueOf(target));
        log.setDetail(detail);
        mapper.insert(log);
    }
}
