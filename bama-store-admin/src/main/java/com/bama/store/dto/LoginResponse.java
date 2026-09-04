package com.bama.store.dto;

import lombok.Data;

import java.util.Set;

@Data
public class LoginResponse {

    private String token;
    private Long staffId;
    private String staffNo;
    private String name;
    private String phone;
    private Long storeId;
    /** 前端据此渲染菜单与按钮 */
    private Set<String> permissions;
}
