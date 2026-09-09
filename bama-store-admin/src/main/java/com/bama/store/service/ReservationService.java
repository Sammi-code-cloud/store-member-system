package com.bama.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bama.store.common.BusinessException;
import com.bama.store.common.OrderNoUtil;
import com.bama.store.entity.*;
import com.bama.store.mapper.*;
import com.bama.store.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationMapper reservations;
    private final TeaRoomMapper rooms;
    private final MemberMapper members;
    private final RoomClosureMapper closures;
    private final StoreAvailability storeAvailability;
    private final TeaRoomService roomService;
    private final AuditService audit;

    public Page<Reservation> page(long pageNum, long pageSize, String status) {
        return page(pageNum, pageSize, status, null, null, null, null);
    }
    public Page<Reservation> page(long pageNum, long pageSize, String status, LocalDate date, Long roomId, String keyword, Long memberId) {
        var q = new LambdaQueryWrapper<Reservation>().eq(Reservation::getStoreId, SecurityUtil.storeId());
        q.eq(status != null && !status.isBlank(), Reservation::getStatus, status).eq(date != null, Reservation::getReserveDate, date)
                .eq(roomId != null, Reservation::getRoomId, roomId).eq(memberId != null, Reservation::getMemberId, memberId);
        if (keyword != null && !keyword.isBlank()) {
            var ids = members.selectList(new LambdaQueryWrapper<Member>().and(w -> w.like(Member::getName, keyword).or().like(Member::getPhone, keyword).or().like(Member::getMemberNo, keyword))).stream().map(Member::getId).toList();
            q.and(w -> { w.like(Reservation::getOrderNo, keyword).or().like(Reservation::getContactName, keyword).or().like(Reservation::getContactPhone, keyword); if (!ids.isEmpty()) w.or().in(Reservation::getMemberId, ids); });
        }
        Page<Reservation> page = reservations.selectPage(new Page<>(Math.max(1, pageNum), Math.min(200, Math.max(1, pageSize))), q.orderByDesc(Reservation::getReserveDate).orderByAsc(Reservation::getStartTime).orderByDesc(Reservation::getId));
        page.getRecords().forEach(this::enrich);
        return page;
    }

    private void enrich(Reservation r) {
        Member member = members.selectById(r.getMemberId());
        r.setMemberName(member == null ? "已归档顾客" : member.getName());
        if (r.getRoomName() == null) { TeaRoom room = rooms.selectById(r.getRoomId()); r.setRoomName(room == null ? "已归档房间" : room.getName()); }
    }

    private void checkAvailability(TeaRoom room, LocalDate date, String time, BigDecimal hours, Long excludedId) {
        BookingRules.validate(room, date, time, hours);
        Store store = org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()
                ? storeAvailability.lockActive(room.getStoreId()) : storeAvailability.requireActive(room.getStoreId());
        int start = BookingRules.minute(time), end = start + BookingRules.duration(hours);
        if (start < BookingRules.minute(store.getOpenTime()) || end > BookingRules.minute(store.getCloseTime())) throw new BusinessException("预约超出门店营业时间");
        var taken = reservations.selectList(new LambdaQueryWrapper<Reservation>().eq(Reservation::getRoomId, room.getId()).eq(Reservation::getReserveDate, date).notIn(Reservation::getStatus, "CANCELLED", "REJECTED").ne(excludedId != null, Reservation::getId, excludedId).last("FOR UPDATE"));
        for (Reservation r : taken) {
            int rs = BookingRules.minute(r.getStartTime());
            if (BookingRules.overlaps(start, end, rs, rs + BookingRules.duration(r.getHours()))) throw new BusinessException("该时间段已有预约，请选择其他时段");
        }
        for (RoomClosure c : closures.selectList(new LambdaQueryWrapper<RoomClosure>().eq(RoomClosure::getRoomId, room.getId()).eq(RoomClosure::getClosureDate, date))) {
            if (BookingRules.overlaps(start, end, BookingRules.minute(c.getStartTime()), BookingRules.minute(c.getEndTime()))) throw new BusinessException("该时间段临时关闭：" + c.getReason());
        }
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public String create(Reservation input) {
        TeaRoom room = roomService.lock(input.getRoomId());
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean staff = auth != null && auth.getPrincipal() instanceof LoginStaff;
        if (staff) SecurityUtil.ownStore(room.getStoreId()); else SecurityUtil.ownCustomer(input.getMemberId());
        Member member = input.getMemberId() == null ? null : members.selectById(input.getMemberId());
        if (member == null || !Integer.valueOf(1).equals(member.getStatus())) throw new BusinessException("顾客不存在或已停用");
        String contactName = input.getContactName() == null || input.getContactName().isBlank() ? member.getName() : input.getContactName().trim();
        String contactPhone = input.getContactPhone() == null || input.getContactPhone().isBlank() ? (staff ? "" : member.getPhone()) : input.getContactPhone().trim();
        if (!staff && (contactName == null || contactName.isBlank() || contactPhone == null || !contactPhone.matches("1[3-9]\\d{9}"))) throw new BusinessException("请填写联系人姓名和有效的11位手机号");
        BigDecimal hours = input.getHours() == null ? BigDecimal.ONE : input.getHours();
        checkAvailability(room, input.getReserveDate(), input.getStartTime(), hours, null);
        if (input.getGuests() != null && (input.getGuests() < 1 || input.getGuests() > 100)) throw new BusinessException("人数应为 1–100 人");
        if (contactPhone != null && !contactPhone.isBlank() && !contactPhone.matches("1[3-9]\\d{9}")) throw new BusinessException("联系电话格式不正确");
        if (input.getContactName() != null && input.getContactName().length() > 64 || input.getRemark() != null && input.getRemark().length() > 500) throw new BusinessException("联系人或备注过长");
        Reservation r = new Reservation();
        r.setRoomId(room.getId()); r.setMemberId(member.getId()); r.setReserveDate(input.getReserveDate()); r.setStartTime(input.getStartTime()); r.setHours(hours);
        r.setAmount(room.getPriceHour().multiply(hours).setScale(2, java.math.RoundingMode.HALF_UP)); r.setPriceHour(room.getPriceHour());
        r.setStatus(staff ? "WAITING" : "PENDING"); r.setStoreId(room.getStoreId()); r.setOrderNo(OrderNoUtil.generate("RS")); r.setRoomName(room.getName());
        r.setContactName(contactName);
        r.setContactPhone(contactPhone); r.setGuests(input.getGuests() == null ? 1 : input.getGuests()); r.setRemark(input.getRemark() == null ? null : input.getRemark().trim()); r.setSource(staff ? "STAFF" : "CUSTOMER");
        reservations.insert(r); audit.record("创建预约", r.getOrderNo(), room.getName() + " " + r.getReserveDate() + " " + r.getStartTime(), r.getStoreId());
        return r.getOrderNo();
    }

    private Reservation locked(Long id) {
        Reservation original = reservations.selectById(id);
        if (original == null) throw new BusinessException("预约不存在");
        roomService.lock(original.getRoomId());
        Reservation current = reservations.selectOne(new LambdaQueryWrapper<Reservation>().eq(Reservation::getId, id).last("FOR UPDATE"));
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginStaff) SecurityUtil.ownStore(current.getStoreId());
        else SecurityUtil.ownCustomer(current.getMemberId());
        return current;
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void confirm(Long id) {
        SecurityUtil.current();
        Reservation r = locked(id);
        if (!"PENDING".equals(r.getStatus())) throw new BusinessException("只有待确认申请可以确认");
        checkAvailability(rooms.selectById(r.getRoomId()), r.getReserveDate(), r.getStartTime(), r.getHours(), id);
        r.setStatus("WAITING"); reservations.updateById(r);
        audit.record("确认预约", r.getOrderNo(), "待店员确认 → 预约成功，待到店");
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void reject(Long id, String reason) {
        SecurityUtil.current();
        Reservation r = locked(id);
        if (!"PENDING".equals(r.getStatus())) throw new BusinessException("只有待确认申请可以拒绝");
        if (reason == null || reason.isBlank() || reason.length() > 255) throw new BusinessException("请填写拒绝原因（最多255字）");
        r.setStatus("REJECTED"); r.setCancelReason(reason.trim()); reservations.updateById(r);
        audit.record("拒绝预约", r.getOrderNo(), reason.trim());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void verify(Long id, Long staffId) {
        Reservation r = locked(id);
        if (!"WAITING".equals(r.getStatus())) throw new BusinessException("只有待到店预约可以核销");
        if (!BookingRules.today().equals(r.getReserveDate())) throw new BusinessException("请在预约当天办理到店核销");
        r.setStatus("USING"); r.setVerifyStaffId(staffId); reservations.updateById(r);
        audit.record("到店核销", r.getOrderNo(), "待到店 → 使用中");
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void complete(Long id) {
        Reservation r = locked(id);
        if (!"USING".equals(r.getStatus())) throw new BusinessException("只有使用中的预约可以完成");
        r.setStatus("VERIFIED"); reservations.updateById(r); audit.record("完成预约", r.getOrderNo(), "使用中 → 已完成（不代表已付款）");
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void cancel(Long id, String reason) {
        Reservation r = locked(id);
        if (!Set.of("PENDING", "WAITING").contains(r.getStatus())) throw new BusinessException("只有待确认或待到店预约可以取消");
        if (reason == null || reason.isBlank() || reason.length() > 255) throw new BusinessException("请填写取消原因（最多255字）");
        r.setStatus("CANCELLED"); r.setCancelReason(reason.trim()); reservations.updateById(r); audit.record("取消预约", r.getOrderNo(), reason.trim(), r.getStoreId());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void reschedule(Long id, LocalDate date, String time) {
        Reservation r = locked(id);
        if (!"WAITING".equals(r.getStatus())) throw new BusinessException("只有待到店预约可以改期");
        checkAvailability(rooms.selectById(r.getRoomId()), date, time, r.getHours(), id);
        String before = r.getReserveDate() + " " + r.getStartTime();
        r.setReserveDate(date); r.setStartTime(time); reservations.updateById(r);
        audit.record("预约改期", r.getOrderNo(), before + " → " + date + " " + time + "，保留原成交金额");
    }

    public List<Map<String, Object>> slots(Long roomId, LocalDate date, BigDecimal hours) {
        TeaRoom room = rooms.selectById(roomId);
        if (room == null) throw new BusinessException("房间不存在");
        BookingRules.duration(hours);
        List<Map<String, Object>> result = new ArrayList<>();
        for (int start = BookingRules.minute(room.getOpenTime()); start + BookingRules.duration(hours) <= BookingRules.minute(room.getCloseTime()); start += 30) {
            String time = BookingRules.time(start); boolean available = true;
            try { checkAvailability(room, date, time, hours, null); } catch (BusinessException e) { available = false; }
            result.add(Map.of("time", time, "available", available));
        }
        return result;
    }
}
