package com.bama.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bama.store.common.BusinessException;
import com.bama.store.entity.*;
import com.bama.store.mapper.*;
import com.bama.store.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeaRoomService {
    private final TeaRoomMapper teaRoomMapper;
    private final BusinessDictionary businessDictionary;
    private final ReservationMapper reservations;
    private final RoomClosureMapper closures;
    private final AuditService audit;

    public List<TeaRoom> list() {
        return teaRoomMapper.selectList(new LambdaQueryWrapper<TeaRoom>()
                .eq(TeaRoom::getStoreId, SecurityUtil.storeId()).orderByAsc(TeaRoom::getSortOrder).orderByDesc(TeaRoom::getId));
    }

    public TeaRoom lock(Long id) {
        if (id == null) throw new BusinessException("请选择房间");
        TeaRoom room = teaRoomMapper.selectOne(new LambdaQueryWrapper<TeaRoom>().eq(TeaRoom::getId, id).last("FOR UPDATE"));
        if (room == null) throw new BusinessException("房间不存在");
        return room;
    }

    @Transactional
    public Long save(TeaRoom input) {
        businessDictionary.requireEnabled();
        TeaRoom room = input.getId() == null ? new TeaRoom() : lock(input.getId());
        if (room.getId() != null) SecurityUtil.ownStore(room.getStoreId());
        if (input.getName() == null || input.getName().isBlank() || input.getName().trim().length() > 64) throw new BusinessException("请填写 1–64 字的房间名称");
        if (input.getPriceHour() == null || input.getPriceHour().signum() < 0 || input.getPriceHour().compareTo(new BigDecimal("99999999.99")) > 0 || input.getPriceHour().scale() > 2) throw new BusinessException("价格应为有效的非负金额，最多两位小数");
        String open = input.getOpenTime() == null ? "10:00" : input.getOpenTime();
        String close = input.getCloseTime() == null ? "22:00" : input.getCloseTime();
        int minutes = BookingRules.minute(close) - BookingRules.minute(open);
        BigDecimal min = input.getMinHours() == null ? BigDecimal.ONE : input.getMinHours();
        if (minutes <= 0 || BookingRules.duration(min) > minutes) throw new BusinessException("营业结束时间须晚于开始时间，且能容纳起订时长");
        int advance = input.getAdvanceDays() == null ? 30 : input.getAdvanceDays();
        if (advance < 0 || advance > 365) throw new BusinessException("可提前预订天数应为 0–365 天");
        int status = input.getStatus() == null ? 1 : input.getStatus();
        if (status != 0 && status != 1) throw new BusinessException("房间状态无效");
        String before = room.getId() == null ? "新建" : room.getName() + " / ¥" + room.getPriceHour() + " / 状态" + room.getStatus();
        room.setName(input.getName().trim()); room.setPriceHour(input.getPriceHour()); room.setStatus(status);
        room.setRoomType(input.getRoomType()); room.setCapacity(input.getCapacity()); room.setImage(input.getImage());
        room.setDescription(input.getDescription()); room.setFacilities(input.getFacilities());
        room.setOpenTime(open); room.setCloseTime(close); room.setMinHours(min); room.setAdvanceDays(advance);
        room.setSortOrder(input.getSortOrder() == null ? 0 : input.getSortOrder()); room.setStoreId(SecurityUtil.storeId());
        if (room.getId() == null) teaRoomMapper.insert(room); else teaRoomMapper.updateById(room);
        audit.record("保存房间", room.getId(), before + " → " + room.getName() + " / ¥" + room.getPriceHour() + " / 状态" + room.getStatus());
        return room.getId();
    }

    @Transactional
    public void delete(Long id) {
        businessDictionary.requireEnabled();
        TeaRoom room = lock(id); SecurityUtil.ownStore(room.getStoreId());
        if (reservations.selectCount(new LambdaQueryWrapper<Reservation>().eq(Reservation::getRoomId, id)) > 0) throw new BusinessException("房间有关联预约，请改为停用以保留记录");
        teaRoomMapper.deleteById(id); audit.record("删除房间", id, room.getName());
    }

    public List<RoomClosure> closures(Long roomId, LocalDate date) {
        TeaRoom room = teaRoomMapper.selectById(roomId);
        if (room == null) throw new BusinessException("房间不存在");
        SecurityUtil.ownStore(room.getStoreId());
        return closures.selectList(new LambdaQueryWrapper<RoomClosure>().eq(RoomClosure::getRoomId, roomId)
                .ge(RoomClosure::getClosureDate, date == null ? BookingRules.today() : date).orderByAsc(RoomClosure::getClosureDate).orderByAsc(RoomClosure::getStartTime));
    }

    @Transactional
    public void close(Long roomId, RoomClosure input) {
        businessDictionary.requireEnabled();
        TeaRoom room = lock(roomId); SecurityUtil.ownStore(room.getStoreId());
        if (input.getClosureDate() == null || input.getClosureDate().isBefore(BookingRules.today())) throw new BusinessException("请选择今天或之后的日期");
        int start = BookingRules.minute(input.getStartTime()), end = BookingRules.minute(input.getEndTime());
        if (start >= end) throw new BusinessException("结束时间须晚于开始时间");
        if (input.getReason() == null || input.getReason().isBlank() || input.getReason().length() > 255) throw new BusinessException("请填写关闭原因（最多255字）");
        var taken = reservations.selectList(new LambdaQueryWrapper<Reservation>().eq(Reservation::getRoomId, roomId).eq(Reservation::getReserveDate, input.getClosureDate()).notIn(Reservation::getStatus, "CANCELLED", "REJECTED"));
        for (Reservation r : taken) {
            int rs = BookingRules.minute(r.getStartTime());
            if (BookingRules.overlaps(start, end, rs, rs + BookingRules.duration(r.getHours()))) throw new BusinessException("关闭时段已有预约，请先处理预约");
        }
        RoomClosure closure = new RoomClosure(); closure.setRoomId(roomId); closure.setStoreId(room.getStoreId());
        closure.setClosureDate(input.getClosureDate()); closure.setStartTime(input.getStartTime()); closure.setEndTime(input.getEndTime()); closure.setReason(input.getReason().trim());
        closures.insert(closure); audit.record("关闭房间时段", roomId, closure.getClosureDate() + " " + closure.getStartTime() + "–" + closure.getEndTime() + " " + closure.getReason());
    }

    @Transactional
    public void reopen(Long roomId, Long closureId) {
        businessDictionary.requireEnabled();
        TeaRoom room = lock(roomId); SecurityUtil.ownStore(room.getStoreId());
        RoomClosure closure = closures.selectById(closureId);
        if (closure == null || !roomId.equals(closure.getRoomId())) throw new BusinessException("关闭记录不存在");
        closures.deleteById(closureId); audit.record("恢复房间时段", roomId, closure.getClosureDate() + " " + closure.getStartTime() + "–" + closure.getEndTime());
    }
}
