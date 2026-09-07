package com.bama.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bama.store.common.BusinessException;
import com.bama.store.entity.*;
import com.bama.store.mapper.*;
import com.bama.store.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ConsumeOrderMapper orders;
    private final WalletTxnMapper transactions;
    private final MemberMapper members;
    private final MemberAccountMapper accounts;
    private final ReservationMapper reservations;
    private final TeaRoomMapper rooms;
    private final RoomClosureMapper closures;
    private final StaffMapper staff;
    private final StoreMapper stores;

    public Map<String,Object> report(LocalDate start, LocalDate end) {
        if (start == null) start = BookingRules.today().minusDays(6);
        if (end == null) end = BookingRules.today();
        if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) > 365) throw new BusinessException("请选择不超过366天的有效日期范围");
        Long storeId = SecurityUtil.storeId();
        var paid = orders.selectList(new LambdaQueryWrapper<ConsumeOrder>().eq(ConsumeOrder::getStoreId, storeId).eq(ConsumeOrder::getStatus, "PAID").ge(ConsumeOrder::getCreateTime, start.atStartOfDay()).lt(ConsumeOrder::getCreateTime, end.plusDays(1).atStartOfDay()));
        var txns = transactions.selectList(new LambdaQueryWrapper<WalletTxn>().eq(WalletTxn::getStoreId, storeId).ge(WalletTxn::getCreateTime, start.atStartOfDay()).lt(WalletTxn::getCreateTime, end.plusDays(1).atStartOfDay()));
        var newcomers = members.selectList(new LambdaQueryWrapper<Member>().ge(Member::getCreateTime, start.atStartOfDay()).lt(Member::getCreateTime, end.plusDays(1).atStartOfDay()));
        var bookings = reservations.selectList(new LambdaQueryWrapper<Reservation>().eq(Reservation::getStoreId, storeId).ge(Reservation::getReserveDate, start).le(Reservation::getReserveDate, end));
        var roomList = rooms.selectList(new LambdaQueryWrapper<TeaRoom>().eq(TeaRoom::getStoreId, storeId).orderByAsc(TeaRoom::getSortOrder));
        var closed = closures.selectList(new LambdaQueryWrapper<RoomClosure>().eq(RoomClosure::getStoreId, storeId).ge(RoomClosure::getClosureDate, start).le(RoomClosure::getClosureDate, end));
        Store store = stores.selectById(storeId);
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("startDate", start); result.put("endDate", end);
        result.put("consumeAmount", sumOrders(paid)); result.put("consumeCount", paid.size());
        result.put("rechargeAmount", sumTxns(txns, "RECHARGE")); result.put("giftAmount", sumTxns(txns, "GIFT"));
        result.put("newCustomers", newcomers.size()); result.put("customerCount", members.selectCount(null));
        result.put("totalBalance", accounts.sumBalance()); result.put("reservationCount", bookings.size());
        result.put("consumeCustomers", paid.stream().map(ConsumeOrder::getMemberId).distinct().count());
        List<Map<String,Object>> trend = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            final LocalDate d = day;
            trend.add(Map.of("date", day, "consume", sumOrders(paid.stream().filter(o -> o.getCreateTime().toLocalDate().equals(d)).toList()),
                    "recharge", sumTxns(txns.stream().filter(t -> t.getCreateTime().toLocalDate().equals(d)).toList(), "RECHARGE"),
                    "customers", newcomers.stream().filter(m -> m.getCreateTime().toLocalDate().equals(d)).count()));
        }
        result.put("trend", trend);
        Map<String,Long> statuses = new LinkedHashMap<>();
        for (String s : List.of("PENDING", "WAITING", "USING", "VERIFIED", "CANCELLED", "REJECTED")) statuses.put(s, bookings.stream().filter(r -> s.equals(r.getStatus())).count());
        result.put("statuses", statuses);
        List<Map<String,Object>> ranking = new ArrayList<>();
        for (TeaRoom room : roomList) {
            var active = bookings.stream().filter(r -> room.getId().equals(r.getRoomId()) && List.of("WAITING", "USING", "VERIFIED").contains(r.getStatus())).toList();
            BigDecimal hours = active.stream().map(Reservation::getHours).reduce(BigDecimal.ZERO, BigDecimal::add);
            int open = Math.max(BookingRules.minute(room.getOpenTime()), store == null ? 0 : BookingRules.minute(store.getOpenTime()));
            int close = Math.min(BookingRules.minute(room.getCloseTime()), store == null ? 1440 : BookingRules.minute(store.getCloseTime()));
            long availableMinutes = 0;
            for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
                final LocalDate d = day;
                int blocked = 0, last = open;
                var intervals = closed.stream().filter(c -> room.getId().equals(c.getRoomId()) && d.equals(c.getClosureDate())).sorted(Comparator.comparing(RoomClosure::getStartTime)).toList();
                for (RoomClosure c : intervals) {
                    int from = Math.max(last, Math.max(open, BookingRules.minute(c.getStartTime()))), to = Math.min(close, BookingRules.minute(c.getEndTime()));
                    if (to > from) blocked += to - from;
                    last = Math.max(last, to);
                }
                availableMinutes += Math.max(0, close - open - blocked);
            }
            Map<String,Object> row = new LinkedHashMap<>(); row.put("name", room.getName()); row.put("count", active.size()); row.put("hours", hours);
            row.put("bookedAmount", active.stream().map(Reservation::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            row.put("availableHours", BigDecimal.valueOf(availableMinutes).divide(BigDecimal.valueOf(60), 1, java.math.RoundingMode.HALF_UP));
            row.put("bookingRate", availableMinutes == 0 ? null : hours.multiply(BigDecimal.valueOf(6000)).divide(BigDecimal.valueOf(availableMinutes), 1, java.math.RoundingMode.HALF_UP));
            ranking.add(row);
        }
        ranking.sort((a,b) -> ((BigDecimal)b.get("hours")).compareTo((BigDecimal)a.get("hours"))); result.put("rooms", ranking);
        List<Map<String,Object>> staffRows = new ArrayList<>();
        for (Staff employee : staff.selectList(new LambdaQueryWrapper<Staff>().eq(Staff::getStoreId, storeId))) {
            var employeeOrders = paid.stream().filter(o -> employee.getId().equals(o.getStaffId())).toList();
            staffRows.add(Map.of("name", employee.getName(), "consumeCount", employeeOrders.size(), "consumeAmount", sumOrders(employeeOrders), "verified", bookings.stream().filter(r -> employee.getId().equals(r.getVerifyStaffId())).count()));
        }
        result.put("staff", staffRows); return result;
    }
    private BigDecimal sumOrders(List<ConsumeOrder> rows) { return rows.stream().map(ConsumeOrder::getPayAmount).reduce(BigDecimal.ZERO, BigDecimal::add); }
    private BigDecimal sumTxns(List<WalletTxn> rows, String type) { return rows.stream().filter(t -> type.equals(t.getType())).map(WalletTxn::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add); }
}
