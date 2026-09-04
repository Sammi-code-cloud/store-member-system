package com.bama.store.dto;

import jakarta.validation.constraints.NotEmpty;
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

    @NotEmpty(message = "消费明细不能为空")
    private List<ChargeItemDto> items;

    private String remark;

    /** 业务幂等号，前端可传；不传则由后端生成 */
    private String bizNo;
}
