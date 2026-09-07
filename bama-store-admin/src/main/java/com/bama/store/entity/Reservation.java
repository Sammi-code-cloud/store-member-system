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
    /** PENDING/WAITING/USING/VERIFIED/CANCELLED/REJECTED */
    private String status;
    private Long verifyStaffId;
    private Long storeId;
    private String roomName;
    private String contactName;
    private String contactPhone;
    private Integer guests;
    private String remark;
    private String cancelReason;
    private String source;
    private BigDecimal priceHour;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String memberName;
}
