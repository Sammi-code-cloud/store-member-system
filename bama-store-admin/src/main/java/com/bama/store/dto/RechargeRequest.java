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
    private BigDecimal amount;

    /** 赠送金额 */
    private BigDecimal giftAmount;

    private String remark;
}
