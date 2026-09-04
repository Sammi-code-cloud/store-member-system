package com.bama.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 员工-角色关联
 */
@Data
@TableName("t_staff_role")
public class StaffRole {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long staffId;
    private Long roleId;

    public StaffRole() {
    }

    public StaffRole(Long staffId, Long roleId) {
        this.staffId = staffId;
        this.roleId = roleId;
    }
}
