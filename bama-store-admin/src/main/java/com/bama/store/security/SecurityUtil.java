package com.bama.store.security;

import com.bama.store.common.BusinessException;
import com.bama.store.common.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 获取当前登录员工的帮助类
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static LoginStaff current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginStaff staff) {
            return staff;
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }

    public static Long staffId() {
        return current().getStaffId();
    }

    public static Long storeId() {
        return current().getStoreId();
    }
}
