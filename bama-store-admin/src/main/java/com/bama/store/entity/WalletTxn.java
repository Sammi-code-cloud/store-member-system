package com.bama.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 资金流水（只增不改）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_wallet_txn")
public class WalletTxn extends BaseEntity {

    private Long memberId;
    /** 业务幂等号 */
    private String bizNo;
    /** RECHARGE/CONSUME/REFUND/GIFT */
    private String type;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String refOrderNo;
    private Long staffId;
    private Long storeId;
    private String remark;
}
