package com.bama.store.common;

import lombok.Getter;

/**
 * 业务状态码枚举
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BUSINESS_ERROR(400, "业务处理失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误"),

    // 业务相关
    LOGIN_FAILED(1001, "手机号或密码错误"),
    ACCOUNT_DISABLED(1002, "账号已停用"),
    BALANCE_NOT_ENOUGH(1003, "会员卡余额不足"),
    PAYCODE_INVALID(1004, "付款码无效或已过期"),
    MEMBER_NOT_FOUND(1005, "会员不存在"),
    ROOM_SLOT_TAKEN(1006, "该时段已被预定"),
    DUPLICATE_SUBMIT(1007, "请勿重复提交");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
