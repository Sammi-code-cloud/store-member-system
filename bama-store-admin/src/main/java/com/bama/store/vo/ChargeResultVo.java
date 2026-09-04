package com.bama.store.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 扣款成功回执
 */
@Data
public class ChargeResultVo {

    private String orderNo;
    private Long memberId;
    private String memberName;
    private BigDecimal originAmount;
    private BigDecimal payAmount;
    private BigDecimal balanceAfter;
    private String staffName;
    private LocalDateTime time;
}
