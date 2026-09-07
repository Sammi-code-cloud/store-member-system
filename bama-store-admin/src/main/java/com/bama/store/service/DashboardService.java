package com.bama.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bama.store.entity.ConsumeOrder;
import com.bama.store.entity.Member;
import com.bama.store.entity.Product;
import com.bama.store.entity.Reservation;
import com.bama.store.mapper.*;
import com.bama.store.vo.DashboardVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据概览
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MemberMapper memberMapper;
    private final MemberAccountMapper accountMapper;
    private final ProductMapper productMapper;
    private final ReservationMapper reservationMapper;
    private final ConsumeOrderMapper consumeOrderMapper;

    public DashboardVo overview() {
        DashboardVo vo = new DashboardVo();
        vo.setTotalBalance(accountMapper.sumBalance());
        vo.setMemberCount(memberMapper.selectCount(new LambdaQueryWrapper<Member>()));
        vo.setProductCount(productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1).eq(Product::getStoreId, com.bama.store.security.SecurityUtil.storeId())));

        LocalDate today = BookingRules.today();
        vo.setTodayReservations(reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>().eq(Reservation::getReserveDate, today).eq(Reservation::getStoreId, com.bama.store.security.SecurityUtil.storeId())));

        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        List<ConsumeOrder> todayOrders = consumeOrderMapper.selectList(
                new LambdaQueryWrapper<ConsumeOrder>()
                        .ge(ConsumeOrder::getCreateTime, dayStart)
                        .lt(ConsumeOrder::getCreateTime, dayEnd)
                        .eq(ConsumeOrder::getStatus, "PAID").eq(ConsumeOrder::getStoreId, com.bama.store.security.SecurityUtil.storeId()));
        vo.setTodayConsumeCount(todayOrders.size());
        BigDecimal todayAmount = todayOrders.stream()
                .map(ConsumeOrder::getPayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTodayConsumeAmount(todayAmount);
        return vo;
    }
}
