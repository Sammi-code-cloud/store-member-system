<template>
  <view class="page">
    <view class="kpis">
      <view class="kpi card">
        <text class="l">会员卡余额总额</text>
        <text class="v mono">¥{{ fmt(data.totalBalance) }}</text>
      </view>
      <view class="kpi card">
        <text class="l">今日消费金额</text>
        <text class="v mono">¥{{ fmt(data.todayConsumeAmount) }}</text>
        <text class="c">{{ data.todayConsumeCount || 0 }} 笔</text>
      </view>
      <view class="kpi card">
        <text class="l">会员总数</text>
        <text class="v mono">{{ data.memberCount || 0 }}</text>
      </view>
      <view class="kpi card">
        <text class="l">今日预定</text>
        <text class="v mono">{{ data.todayReservations || 0 }}</text>
      </view>
      <view class="kpi card">
        <text class="l">货品总数</text>
        <text class="v mono">{{ data.productCount || 0 }}</text>
      </view>
    </view>
    <view class="btn-primary reload" @tap="load">刷新数据</view>
  </view>
</template>

<script>
import api from '@/common/api.js'
export default {
  data() {
    return { data: {} }
  },
  onShow() { this.load() },
  methods: {
    fmt(n) { return Number(n || 0).toFixed(2) },
    async load() {
      try {
        this.data = await api.dashboard()
      } catch (e) {}
    }
  }
}
</script>

<style lang="scss" scoped>
.page { padding: 32rpx; }
.kpis { display: flex; flex-wrap: wrap; gap: 20rpx; }
.kpi {
  width: calc(50% - 10rpx); padding: 30rpx 28rpx; display: flex; flex-direction: column;
}
.kpi:first-child { width: 100%; }
.kpi .l { font-size: 24rpx; color: $muted; }
.kpi .v { font-size: 48rpx; font-weight: 600; margin-top: 14rpx; color: $ink; }
.kpi .c { font-size: 22rpx; color: $green; margin-top: 6rpx; }
.reload { margin-top: 40rpx; padding: 26rpx; font-size: 30rpx; font-weight: 600; }
</style>
