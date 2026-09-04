<template>
  <view class="page">
    <view class="header brand-bg">
      <view class="status" :style="{ height: statusBarHeight + 'px' }"></view>
      <view class="nav flex-between">
        <view class="flex">
          <view class="seal">馬</view>
          <text class="bname">员工工作台</text>
        </view>
        <text class="who" @tap="logout">{{ staff.name }} ▾</text>
      </view>

      <view class="today">
        <text class="k">今日收款（本店）</text>
        <text class="amt mono">¥ {{ todayAmount }}</text>
        <text class="sub">消费 {{ todayCount }} 笔 · 预定 {{ todayReservations }} 单</text>
      </view>
    </view>

    <view class="quad">
      <view class="qi" @tap="go('/pages/staff/scan')">
        <view class="ic">📷</view><text>扫码收款</text>
      </view>
      <view class="qi" @tap="go('/pages/staff/recharge')">
        <view class="ic">💳</view><text>会员储值</text>
      </view>
      <view class="qi" @tap="go('/pages/staff/dashboard')">
        <view class="ic">▤</view><text>数据概览</text>
      </view>
      <view class="qi" @tap="loadData">
        <view class="ic">⟳</view><text>刷新</text>
      </view>
    </view>

    <view class="section">
      <view class="row-head flex-between">
        <text class="h">待核销预定</text>
        <text class="more">{{ reservations.length }} 单</text>
      </view>
      <view v-if="reservations.length === 0" class="empty">暂无待核销预定</view>
      <view v-for="r in reservations" :key="r.id" class="li card">
        <view class="tm mono">{{ r.startTime }}</view>
        <view class="txt">
          <text class="t1">茶室 #{{ r.roomId }} · 会员 #{{ r.memberId }}</text>
          <text class="t2">{{ r.reserveDate }} · {{ r.hours }}小时 · ¥{{ r.amount }}</text>
        </view>
        <view class="verify" @tap="verify(r)">核销</view>
      </view>
    </view>
  </view>
</template>

<script>
import api from '@/common/api.js'
import { auth } from '@/common/store.js'

export default {
  data() {
    return {
      statusBarHeight: 20,
      staff: {},
      todayAmount: '0.00',
      todayCount: 0,
      todayReservations: 0,
      reservations: []
    }
  },
  onLoad() {
    this.statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 20
    this.staff = auth.getStaff() || {}
  },
  onShow() {
    if (!auth.isLogin()) {
      uni.reLaunch({ url: '/pages/staff/login' })
      return
    }
    this.loadData()
  },
  methods: {
    go(url) { uni.navigateTo({ url }) },
    async loadData() {
      try {
        const d = await api.dashboard()
        this.todayAmount = (d.todayConsumeAmount || 0).toFixed
          ? Number(d.todayConsumeAmount).toFixed(2)
          : d.todayConsumeAmount
        this.todayCount = d.todayConsumeCount || 0
        this.todayReservations = d.todayReservations || 0
      } catch (e) {}
      try {
        const page = await api.reservationPage({ status: 'WAITING', pageSize: 20 })
        this.reservations = page.records || []
      } catch (e) {}
    },
    async verify(r) {
      uni.showModal({
        title: '核销确认',
        content: `确认核销预定 ${r.orderNo}？`,
        success: async (res) => {
          if (!res.confirm) return
          try {
            await api.reservationVerify(r.id)
            uni.showToast({ title: '核销成功', icon: 'success' })
            this.loadData()
          } catch (e) {}
        }
      })
    },
    logout() {
      uni.showModal({
        title: '退出登录',
        content: '确认退出当前员工账号？',
        success: (res) => {
          if (res.confirm) {
            auth.logout()
            uni.reLaunch({ url: '/pages/staff/login' })
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.page { padding-bottom: 60rpx; }
.header { padding: 0 32rpx 44rpx; border-radius: 0 0 36rpx 36rpx; }
.nav { padding: 12rpx 0 24rpx; }
.seal {
  width: 52rpx; height: 52rpx; border-radius: 14rpx; background: rgba(255,255,255,0.16);
  color: #fff; font-family: serif; display: flex; align-items: center; justify-content: center;
  margin-right: 14rpx; font-size: 30rpx;
}
.bname { color: #fff; font-size: 32rpx; font-weight: 600; }
.who { color: rgba(255,255,255,0.85); font-size: 24rpx; }
.today .k { color: rgba(255,255,255,0.8); font-size: 24rpx; }
.today .amt { display: block; color: #fff; font-size: 60rpx; font-weight: 600; margin-top: 10rpx; }
.today .sub { color: rgba(255,255,255,0.8); font-size: 22rpx; }

.quad {
  display: flex; margin: -30rpx 32rpx 0; background: #fff; border-radius: 24rpx;
  padding: 28rpx 0; box-shadow: 0 16rpx 36rpx -22rpx rgba(60,32,20,0.4);
}
.qi { flex: 1; display: flex; flex-direction: column; align-items: center; }
.qi .ic {
  width: 76rpx; height: 76rpx; border-radius: 22rpx; background: #F6EEDF; color: $brand;
  font-size: 38rpx; display: flex; align-items: center; justify-content: center; margin-bottom: 12rpx;
}
.qi text { font-size: 24rpx; color: $ink; }

.section { padding: 32rpx; }
.row-head { margin-bottom: 20rpx; }
.row-head .h { font-size: 32rpx; font-weight: 600; }
.row-head .more { font-size: 24rpx; color: $muted; }
.empty { text-align: center; color: $muted; font-size: 24rpx; padding: 40rpx 0; }
.li { display: flex; align-items: center; padding: 24rpx; margin-bottom: 18rpx; }
.tm { width: 90rpx; color: $brand; font-weight: 600; font-size: 26rpx; }
.txt { flex: 1; }
.txt .t1 { font-size: 26rpx; }
.txt .t2 { display: block; font-size: 22rpx; color: $muted; margin-top: 4rpx; }
.verify { background: $brand; color: #fff; font-size: 24rpx; padding: 12rpx 28rpx; border-radius: 999rpx; }
</style>
