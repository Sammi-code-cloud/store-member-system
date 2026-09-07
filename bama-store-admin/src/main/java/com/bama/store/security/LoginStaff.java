package com.bama.store.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

/**
 * 登录态员工主体（存入 SecurityContext）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginStaff implements Serializable {

    private Long staffId;
    private String staffNo;
    private String name;
    private String phone;
    private Long storeId;
    /** 权限编码集合 */
    private Set<String> permissions;
    /** Accessible stores, loaded afresh on every authenticated request. */
    private Set<Long> storeIds;
}
