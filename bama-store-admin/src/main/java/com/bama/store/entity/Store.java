package com.bama.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 门店
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_store")
public class Store extends BaseEntity {

    private String name;
    private String address;
    private String phone;
    /** 1营业 0停业 */
    private Integer status;
}
