package com.bama.store.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 扫码扣款确认请求
 */
@Data
public class ChargeConfirmRequest {

    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    private List<ChargeItemDto> items;

    /** 手动扣款的实扣金额；与消费明细互斥，不再计算会员折扣。 */
    private BigDecimal amount;

    /** 手机号查询后的再次核对。 */
    private String expectedPhone;

    private String remark;

    /** 必填业务幂等号；同一笔扣款重试必须使用原号。 */
    private String bizNo;
}
