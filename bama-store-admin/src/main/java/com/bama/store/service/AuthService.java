package com.bama.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bama.store.common.BusinessException;
import com.bama.store.common.ResultCode;
import com.bama.store.dto.LoginRequest;
import com.bama.store.dto.LoginResponse;
import com.bama.store.entity.Staff;
import com.bama.store.mapper.StaffMapper;
import com.bama.store.security.JwtUtil;
import com.bama.store.security.LoginStaff;
import com.bama.store.security.LoginStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 登录认证
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final StaffMapper staffMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginStaffService loginStaffService;

    public LoginResponse login(LoginRequest req) {
        Staff staff = staffMapper.selectOne(
                new LambdaQueryWrapper<Staff>().eq(Staff::getPhone, req.getPhone()));
        if (staff == null || !passwordEncoder.matches(req.getPassword(), staff.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }
        if (staff.getStatus() == null || staff.getStatus() != 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        String token = jwtUtil.generate(staff.getId(), staff.getName());
        LoginStaff loginStaff = loginStaffService.load(staff.getId());

        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setStaffId(staff.getId());
        resp.setStaffNo(staff.getStaffNo());
        resp.setName(staff.getName());
        resp.setPhone(staff.getPhone());
        resp.setStoreId(staff.getStoreId());
        resp.setPermissions(loginStaff == null ? java.util.Set.of() : loginStaff.getPermissions());
        return resp;
    }
}
