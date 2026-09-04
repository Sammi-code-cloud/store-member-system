package com.bama.store.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 消费明细项
 */
@Data
public class ChargeItemDto {

    /** PRODUCT货品 / ROOM茶室 */
    private String itemType;
    private String itemName;
    private BigDecimal price;
    private Integer quantity;
}
