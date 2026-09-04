package com.bama.store.config;

import com.bama.store.entity.Staff;
import com.bama.store.entity.StaffRole;
import com.bama.store.mapper.StaffMapper;
import com.bama.store.mapper.StaffRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时初始化管理员账号（密码为 BCrypt，避免在 SQL 中写死密文）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final StaffMapper staffMapper;
    private final StaffRoleMapper staffRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        Long count = staffMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }

        // 店长（超级管理员）
        Staff admin = new Staff();
        admin.setStaffNo("BM-ADMIN");
        admin.setName("王芳");
        admin.setPhone("13800000000");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setStoreId(1L);
        admin.setStatus(1);
        staffMapper.insert(admin);
        staffRoleMapper.insert(new StaffRole(admin.getId(), 1L)); // 店长

        // 收银员（演示扫码收款权限）
        Staff cashier = new Staff();
        cashier.setStaffNo("BM-CASHIER");
        cashier.setName("林思远");
        cashier.setPhone("13800000001");
        cashier.setPassword(passwordEncoder.encode("123456"));
        cashier.setStoreId(1L);
        cashier.setStatus(1);
        staffMapper.insert(cashier);
        staffRoleMapper.insert(new StaffRole(cashier.getId(), 2L)); // 收银员

        log.info("初始化管理员账号完成：店长 13800000000/admin123，收银员 13800000001/123456");
    }
}
