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
    private String password;
    private Long storeId;
    /** 1在职 0停用 */
    private Integer status;
}
