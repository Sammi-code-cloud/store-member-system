package com.bama.store.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bama.store.common.BusinessException;
import com.bama.store.common.Result;
import com.bama.store.common.ResultCode;
import com.bama.store.entity.Member;
import com.bama.store.entity.MemberAccount;
import com.bama.store.entity.Product;
import com.bama.store.entity.Reservation;
import com.bama.store.entity.Store;
import com.bama.store.entity.TeaRoom;
import com.bama.store.entity.WalletTxn;
import com.bama.store.mapper.MemberAccountMapper;
import com.bama.store.mapper.MemberMapper;
import com.bama.store.mapper.ProductMapper;
import com.bama.store.mapper.ReservationMapper;
import com.bama.store.mapper.StoreMapper;
import com.bama.store.mapper.TeaRoomMapper;
import com.bama.store.mapper.WalletTxnMapper;
import com.bama.store.service.PayCodeService;
import com.bama.store.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 顾客端公开接口（演示用，免登录）
 * 说明：正式环境应改为微信 openid 登录后按登录态取自己的数据，
 * 此处为让顾客端 H5 演示可直接展示真实数据而开放。
 */
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final MemberMapper memberMapper;
    private final MemberAccountMapper accountMapper;
    private final WalletTxnMapper walletTxnMapper;
    private final TeaRoomMapper teaRoomMapper;
    private final ReservationMapper reservationMapper;
    private final ReservationService reservationService;
    private final ProductMapper productMapper;
    private final StoreMapper storeMapper;
    private final PayCodeService payCodeService;

    private static final Map<String, String> RESERVE_STATUS = Map.of(
            "WAITING", "待到店", "USING", "进行中", "VERIFIED", "已完成", "CANCELLED", "已取消");

    /** 门店营业时段（可预定的开始时间） */
    private static final List<String> SLOTS = List.of("10:00", "13:00", "15:00", "17:00", "19:00", "20:00");

    private static final Map<String, String> LEVEL_TEXT = Map.of(
            "NORMAL", "普通会员", "GOLD", "金卡会员", "BLACK_GOLD", "黑金会员");

    /** 会员主页信息（会员 + 账户余额） */
    @GetMapping("/{memberId}")
    public Result<Map<String, Object>> home(@PathVariable Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException(ResultCode.MEMBER_NOT_FOUND);
        }
        MemberAccount account = accountMapper.selectOne(
                new LambdaQueryWrapper<MemberAccount>().eq(MemberAccount::getMemberId, memberId));

        Map<String, Object> vo = new HashMap<>();
        vo.put("memberId", member.getId());
        vo.put("name", member.getName());
        vo.put("phone", maskPhone(member.getPhone()));
        vo.put("level", member.getLevel());
        vo.put("levelText", LEVEL_TEXT.getOrDefault(member.getLevel(), "会员"));
        vo.put("discount", member.getDiscount());
        vo.put("points", member.getPoints());
        vo.put("balance", account == null ? 0 : account.getBalance());
        vo.put("totalRecharge", account == null ? 0 : account.getTotalRecharge());
        vo.put("totalConsume", account == null ? 0 : account.getTotalConsume());
        return Result.success(vo);
    }

    /** 最近消费/充值流水 */
    @GetMapping("/{memberId}/records")
    public Result<List<WalletTxn>> records(@PathVariable Long memberId) {
        List<WalletTxn> list = walletTxnMapper.selectList(
                new LambdaQueryWrapper<WalletTxn>()
                        .eq(WalletTxn::getMemberId, memberId)
                        .orderByDesc(WalletTxn::getId)
                        .last("limit 10"));
        return Result.success(list);
    }

    /** 可预定茶室 */
    @GetMapping("/rooms")
    public Result<List<TeaRoom>> rooms() {
        return Result.success(teaRoomMapper.selectList(
                new LambdaQueryWrapper<TeaRoom>().eq(TeaRoom::getStatus, 1).orderByAsc(TeaRoom::getId)));
    }

    /** 当前门店信息（顾客端首页展示，后台可修改） */
    @GetMapping("/store")
    public Result<Map<String, Object>> store() {
        Store s = storeMapper.selectOne(
                new LambdaQueryWrapper<Store>().eq(Store::getStatus, 1).orderByAsc(Store::getId).last("limit 1"));
        Map<String, Object> m = new HashMap<>();
        if (s != null) {
            m.put("id", s.getId());
            m.put("name", s.getName());
            m.put("address", s.getAddress());
            m.put("phone", s.getPhone());
        }
        return Result.success(m);
    }

    /** 活动 / 精选商品（上架货品） */
    @GetMapping("/products")
    public Result<List<Product>> products() {
        return Result.success(productMapper.selectList(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1).orderByAsc(Product::getId)));
    }

    /** 我的预定（含茶室名称与状态） */
    @GetMapping("/{memberId}/reservations")
    public Result<List<Map<String, Object>>> myReservations(@PathVariable Long memberId) {
        List<Reservation> list = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getMemberId, memberId)
                        .orderByDesc(Reservation::getId)
                        .last("limit 20"));
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Reservation r : list) {
            TeaRoom room = teaRoomMapper.selectById(r.getRoomId());
            Map<String, Object> m = new HashMap<>();
            m.put("orderNo", r.getOrderNo());
            m.put("roomName", room == null ? "茶室" : room.getName());
            m.put("roomType", room == null ? "" : room.getRoomType());
            m.put("reserveDate", r.getReserveDate());
            m.put("startTime", r.getStartTime());
            m.put("hours", r.getHours());
            m.put("amount", r.getAmount());
            m.put("status", r.getStatus());
            m.put("statusText", RESERVE_STATUS.getOrDefault(r.getStatus(), r.getStatus()));
            result.add(m);
        }
        return Result.success(result);
    }

    /**
     * 查询某茶室某天的时段占用情况
     * 返回全部营业时段及是否可预定
     */
    @GetMapping("/rooms/{roomId}/slots")
    public Result<List<Map<String, Object>>> slots(@PathVariable Long roomId, @RequestParam String date) {
        LocalDate d = LocalDate.parse(date);
        List<Reservation> taken = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getRoomId, roomId)
                        .eq(Reservation::getReserveDate, d)
                        .ne(Reservation::getStatus, "CANCELLED"));
        List<String> takenTimes = taken.stream().map(Reservation::getStartTime).toList();
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (String s : SLOTS) {
            Map<String, Object> m = new HashMap<>();
            m.put("time", s);
            m.put("available", !takenTimes.contains(s));
            result.add(m);
        }
        return Result.success(result);
    }

    /** 某茶室某天全部已预约时段（开始–结束），用于顾客避开 */
    @GetMapping("/rooms/{roomId}/reserved")
    public Result<List<Map<String, Object>>> reserved(@PathVariable Long roomId, @RequestParam String date) {
        LocalDate d = LocalDate.parse(date);
        List<Reservation> list = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getRoomId, roomId)
                        .eq(Reservation::getReserveDate, d)
                        .ne(Reservation::getStatus, "CANCELLED")
                        .orderByAsc(Reservation::getStartTime));
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Reservation r : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("start", r.getStartTime());
            m.put("end", addHours(r.getStartTime(), r.getHours()));
            m.put("hours", r.getHours());
            result.add(m);
        }
        return Result.success(result);
    }

    /** 开始时间 + 时长 → 结束时间（HH:mm） */
    private String addHours(String start, java.math.BigDecimal hours) {
        if (start == null || hours == null) {
            return start;
        }
        String[] p = start.split(":");
        int mins = Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1])
                + hours.multiply(java.math.BigDecimal.valueOf(60)).intValue();
        return String.format("%02d:%02d", mins / 60, mins % 60);
    }

    /** 顾客按日期预定茶室（复用预定服务，唯一索引防重复占用） */
    @PostMapping("/reserve")
    public Result<String> reserve(@RequestBody Reservation reservation) {
        if (reservation.getMemberId() == null) {
            throw new BusinessException(ResultCode.MEMBER_NOT_FOUND);
        }
        if (reservation.getHours() == null) {
            reservation.setHours(java.math.BigDecimal.valueOf(2));
        }
        return Result.success(reservationService.create(reservation));
    }

    /** 生成本人一次性付款码 */
    @PostMapping("/{memberId}/paycode")
    public Result<Map<String, String>> paycode(@PathVariable Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException(ResultCode.MEMBER_NOT_FOUND);
        }
        return Result.success(Map.of("payCode", payCodeService.generate(memberId)));
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
