package com.bama.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bama.store.entity.Permission;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PermissionMapper extends BaseMapper<Permission> {

    /** 查询某员工拥有的全部权限编码（去重） */
    @Select("SELECT DISTINCT p.code FROM t_permission p " +
            "JOIN t_role_permission rp ON rp.permission_id = p.id " +
            "JOIN t_staff_role sr ON sr.role_id = rp.role_id " +
            "WHERE sr.staff_id = #{staffId} AND p.deleted = 0")
    List<String> selectPermissionCodesByStaffId(Long staffId);

    /** 查询某角色拥有的权限编码 */
    @Select("SELECT p.code FROM t_permission p " +
            "JOIN t_role_permission rp ON rp.permission_id = p.id " +
            "WHERE rp.role_id = #{roleId} AND p.deleted = 0")
    List<String> selectPermissionCodesByRoleId(Long roleId);
}
