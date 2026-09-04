package com.bama.store.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 扫码解析后返回的会员信息
 */
@Data
public class MemberChargeInfoVo {

    private Long memberId;
    private String memberNo;
    private String name;
    private String phone;
    private String level;
    private Integer discount;
    private BigDecimal balance;
}
