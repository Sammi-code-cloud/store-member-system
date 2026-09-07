package com.bama.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 茶室
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tea_room")
public class TeaRoom extends BaseEntity {

    private String name;
    private String roomType;
    private String capacity;
    private BigDecimal priceHour;
    private String image;
    private String description;
    private String facilities;
    private String openTime;
    private String closeTime;
    private BigDecimal minHours;
    private Integer advanceDays;
    private Integer sortOrder;
    private Long storeId;
    /** 1可用 0停用 */
    private Integer status;
}
