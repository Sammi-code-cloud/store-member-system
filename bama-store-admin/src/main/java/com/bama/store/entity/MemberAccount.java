package com.bama.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 会员账户（余额）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_member_account")
public class MemberAccount extends BaseEntity {

    private Long memberId;
    /** 可用余额 */
    private BigDecimal balance;
    private BigDecimal totalRecharge;
    private BigDecimal totalConsume;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
