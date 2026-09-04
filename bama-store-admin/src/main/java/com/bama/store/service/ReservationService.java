package com.bama.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bama.store.common.BusinessException;
import com.bama.store.common.OrderNoUtil;
import com.bama.store.common.ResultCode;
import com.bama.store.entity.Reservation;
import com.bama.store.entity.TeaRoom;
import com.bama.store.mapper.ReservationMapper;
import com.bama.store.mapper.TeaRoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 茶室预定
 */
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationMapper reservationMapper;
    private final TeaRoomMapper teaRoomMapper;

    public Page<Reservation> page(long pageNum, long pageSize, String status) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Reservation::getStatus, status);
        }
        wrapper.orderByDesc(Reservation::getId);
        return reservationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /** 创建预定，依赖唯一索引防止时段重复占用 */
    @Transactional(rollbackFor = Exception.class)
    public String create(Reservation reservation) {
        TeaRoom room = teaRoomMapper.selectById(reservation.getRoomId());
        if (room == null) {
            throw new BusinessException("茶室不存在");
        }
        BigDecimal hours = reservation.getHours() == null ? BigDecimal.ONE : reservation.getHours();
        reservation.setHours(hours);
        reservation.setAmount(room.getPriceHour().multiply(hours));
        reservation.setStatus("WAITING");
        reservation.setStoreId(room.getStoreId());
        reservation.setOrderNo(OrderNoUtil.generate("RS"));
        try {
            reservationMapper.insert(reservation);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.ROOM_SLOT_TAKEN);
        }
        return reservation.getOrderNo();
    }

    /** 核销预定 */
    public void verify(Long reservationId, Long staffId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException("预定单不存在");
        }
        if ("VERIFIED".equals(reservation.getStatus())) {
            throw new BusinessException("该预定已核销");
        }
        Reservation update = new Reservation();
        update.setId(reservationId);
        update.setStatus("VERIFIED");
        update.setVerifyStaffId(staffId);
        reservationMapper.updateById(update);
    }
}
