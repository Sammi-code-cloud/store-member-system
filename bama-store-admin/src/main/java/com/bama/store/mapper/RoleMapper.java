package com.bama.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bama.store.entity.Role;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RoleMapper extends BaseMapper<Role> {

    /** 查询某员工拥有的角色 */
    @Select("SELECT r.* FROM t_role r " +
            "JOIN t_staff_role sr ON sr.role_id = r.id " +
            "WHERE sr.staff_id = #{staffId} AND r.deleted = 0")
    List<Role> selectRolesByStaffId(Long staffId);
}
