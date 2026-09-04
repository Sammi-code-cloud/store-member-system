package com.bama.store.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应返回体
 */
@Data
public class Result<T> implements Serializable {

    /** 业务状态码，200 成功 */
    private int code;
    /** 提示信息 */
    private String message;
    /** 数据 */
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMessage(ResultCode.SUCCESS.getMessage());
        r.setData(data);
        return r;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return fail(resultCode.getCode(), resultCode.getMessage());
    }

    public static <T> Result<T> fail(String message) {
        return fail(ResultCode.BUSINESS_ERROR.getCode(), message);
    }
}
