package com.bama.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 新增员工请求
 */
@Data
public class StaffCreateRequest {

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "初始密码不能为空")
    private String password;

    private Long storeId;
    private List<Long> storeIds;

    /** 角色ID列表 */
    @NotEmpty(message = "至少分配一个角色")
    private List<Long> roleIds;
}
