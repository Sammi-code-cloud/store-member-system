package com.bama.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 茶室预定
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_reservation")
public class Reservation extends BaseEntity {

    private String orderNo;
    private Long memberId;
    private Long roomId;
    private LocalDate reserveDate;
    private String startTime;
    private BigDecimal hours;
    private BigDecimal amount;
    /** WAITING/USING/VERIFIED/CANCELLED */
    private String status;
    private Long verifyStaffId;
    private Long storeId;
}
