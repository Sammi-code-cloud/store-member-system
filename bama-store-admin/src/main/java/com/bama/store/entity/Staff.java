package com.bama.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 员工
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_staff")
public class Staff extends BaseEntity {

    private String staffNo;
    private String name;
    private String phone;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private java.util.List<Long> roleIds;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private java.util.List<String> roleNames;
    private Long storeId;
    /** 1在职 0停用 */
    private Integer status;
}
