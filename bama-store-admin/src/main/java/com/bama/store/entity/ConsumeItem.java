package com.bama.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 消费明细
 */
@Data
@TableName("t_consume_item")
public class ConsumeItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    /** PRODUCT货品/ROOM茶室 */
    private String itemType;
    private String itemName;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
}
