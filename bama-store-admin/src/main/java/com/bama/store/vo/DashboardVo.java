package com.bama.store.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 数据概览
 */
@Data
public class DashboardVo {

    /** 会员卡余额总额 */
    private BigDecimal totalBalance;
    /** 会员总数 */
    private long memberCount;
    /** 货品总数 */
    private long productCount;
    /** 今日预定数 */
    private long todayReservations;
    /** 今日消费笔数 */
    private long todayConsumeCount;
    /** 今日消费金额 */
    private BigDecimal todayConsumeAmount;
}
