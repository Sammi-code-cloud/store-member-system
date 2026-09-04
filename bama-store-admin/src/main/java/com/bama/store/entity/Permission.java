package com.bama.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_permission")
public class Permission extends BaseEntity {

    private String code;
    private String name;
    private String module;
}
