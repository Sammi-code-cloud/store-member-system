package com.bama.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 货品
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_product")
public class Product extends BaseEntity {

    private String name;
    private String barcode;
    private String category;
    private String spec;
    private BigDecimal retailPrice;
    private BigDecimal memberPrice;
    private Integer stock;
    private Integer warnStock;
    private String channel;
    private String image;
    /** 1上架 0下架 */
    private Integer status;
    private Long storeId;
}
