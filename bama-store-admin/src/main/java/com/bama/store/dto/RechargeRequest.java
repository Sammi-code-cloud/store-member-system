package com.bama.store.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 会员储值请求
 */
@Data
public class RechargeRequest {

    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于0")
    @jakarta.validation.constraints.Digits(integer = 8, fraction = 2, message = "充值金额最多8位整数、2位小数")
    private BigDecimal amount;

    /** 赠送金额 */
    @DecimalMin(value = "0", message = "赠送金额不能为负数")
    @jakarta.validation.constraints.Digits(integer = 8, fraction = 2, message = "赠送金额最多8位整数、2位小数")
    private BigDecimal giftAmount;

    @jakarta.validation.constraints.Size(max = 128, message = "备注最多128字")
    private String remark;
}
