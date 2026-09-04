package com.bama.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会员（客户）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_member")
public class Member extends BaseEntity {

    private String memberNo;
    private String name;
    private String phone;
    private String openid;
    /** NORMAL/GOLD/BLACK_GOLD */
    private String level;
    /** 折扣百分比，92 表示 92 折 */
    private Integer discount;
    private Integer points;
    /** 1正常 0冻结 */
    private Integer status;
}
