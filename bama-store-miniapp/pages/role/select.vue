<template>
  <view class="page brand-bg">
    <view class="logo-area">
      <view class="seal">馬</view>
      <text class="title">八马门店</text>
      <text class="sub">BAMA TEA · 一茶一室，尽在掌中</text>
    </view>

    <view class="cards">
      <view class="entry" @tap="go('/pages/customer/home')">
        <text class="ic">🍵</text>
        <view class="info">
          <text class="n">我是顾客</text>
          <text class="d">预定茶室 · 会员储值 · 出示付款码</text>
        </view>
        <text class="arrow">›</text>
      </view>

      <view class="entry" @tap="toStaff">
        <text class="ic">🧑‍🍳</text>
        <view class="info">
          <text class="n">我是员工</text>
          <text class="d">扫码收款 · 核销预定 · 会员储值</text>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>

    <text class="foot">八马茶业 · 门店服务</text>
  </view>
</template>

<script>
import { auth } from '@/common/store.js'
export default {
  methods: {
    go(url) {
      uni.navigateTo({ url })
    },
    toStaff() {
      // 已登录直接进工作台
      if (auth.isLogin()) {
        uni.navigateTo({ url: '/pages/staff/workbench' })
      } else {
        uni.navigateTo({ url: '/pages/staff/login' })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding: 160rpx 48rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.logo-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 90rpx;
}
.seal {
  width: 120rpx;
  height: 120rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.14);
  border: 2rpx solid rgba(255, 255, 255, 0.4);
  color: #fff;
  font-size: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: serif;
}
.title {
  color: #fff;
  font-size: 52rpx;
  font-weight: 600;
  margin-top: 28rpx;
  letter-spacing: 4rpx;
}
.sub {
  color: rgba(255, 255, 255, 0.7);
  font-size: 24rpx;
  margin-top: 14rpx;
}
.cards {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 28rpx;
}
.entry {
  background: #fff;
  border-radius: 28rpx;
  padding: 36rpx 32rpx;
  display: flex;
  align-items: center;
  box-shadow: 0 20rpx 40rpx -20rpx rgba(0, 0, 0, 0.35);
}
.entry .ic {
  font-size: 56rpx;
  margin-right: 26rpx;
}
.entry .info {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.entry .n {
  font-size: 34rpx;
  font-weight: 600;
  color: $ink;
}
.entry .d {
  font-size: 24rpx;
  color: $muted;
  margin-top: 8rpx;
}
.entry .arrow {
  font-size: 44rpx;
  color: $muted;
}
.foot {
  color: rgba(255, 255, 255, 0.6);
  font-size: 22rpx;
  margin-top: auto;
  margin-bottom: 60rpx;
}
</style>
