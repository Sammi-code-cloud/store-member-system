package com.bama.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 消费单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_consume_order")
public class ConsumeOrder extends BaseEntity {

    private String orderNo;
    private Long memberId;
    private Long staffId;
    private Long storeId;
    private BigDecimal originAmount;
    private BigDecimal payAmount;
    /** MEMBER_CARD */
    private String payType;
    /** PAID/REFUNDED */
    private String status;
    private String remark;
}
