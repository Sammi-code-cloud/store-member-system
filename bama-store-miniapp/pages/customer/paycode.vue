<template>
  <view class="page brand-bg">
    <text class="title">付款码</text>
    <text class="tip">向店员出示，扫码即扣会员卡</text>

    <view class="card">
      <view v-if="loading" class="loading">生成中...</view>
      <block v-else>
        <!-- 二维码占位（真实项目可用 uqrcode 生成图形） -->
        <view class="qr">
          <view class="qr-seal">八</view>
        </view>
        <!-- 条码占位 -->
        <view class="barcode"></view>
        <text class="code mono">{{ payCode }}</text>
        <text class="countdown">{{ countdown }}s 后自动刷新</text>
      </block>
    </view>

    <view class="balance-bar">
      <text>会员卡可用余额</text>
      <text class="b mono">¥{{ member.balance }}</text>
    </view>

    <view class="refresh" @tap="refresh">手动刷新付款码</view>
  </view>
</template>

<script>
import api from '@/common/api.js'
import { demoMember } from '@/common/mock.js'

export default {
  data() {
    return {
      member: demoMember,
      payCode: '',
      loading: true,
      countdown: 60,
      timer: null
    }
  },
  onLoad() {
    this.refresh()
  },
  onUnload() {
    if (this.timer) clearInterval(this.timer)
  },
  methods: {
    async refresh() {
      this.loading = true
      try {
        // 真实接口：为当前会员生成一次性付款码（演示写死 memberId=1）
        const res = await api.paycodeGenerate(this.member.memberId)
        this.payCode = res.payCode
        this.startCountdown()
      } catch (e) {
        // 后端未启动时回退演示码
        this.payCode = 'BM' + Date.now().toString().slice(-16)
      } finally {
        this.loading = false
      }
    },
    startCountdown() {
      if (this.timer) clearInterval(this.timer)
      this.countdown = 60
      this.timer = setInterval(() => {
        this.countdown--
        if (this.countdown <= 0) {
          this.refresh()
        }
      }, 1000)
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding: 60rpx 48rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.title { color: #fff; font-size: 38rpx; font-weight: 600; }
.tip { color: rgba(255,255,255,0.8); font-size: 24rpx; margin-top: 12rpx; }
.card {
  background: #fff; border-radius: 28rpx; padding: 44rpx; width: 100%;
  margin-top: 44rpx; display: flex; flex-direction: column; align-items: center;
}
.loading { color: $muted; padding: 80rpx 0; }
.qr {
  width: 320rpx; height: 320rpx; border-radius: 16rpx; position: relative;
  background-image:
    repeating-linear-gradient(90deg, #241C19 0 12rpx, #fff 12rpx 24rpx),
    repeating-linear-gradient(0deg, #241C19 0 12rpx, transparent 12rpx 24rpx);
  background-blend-mode: multiply;
  border: 10rpx solid #fff; box-shadow: 0 0 0 2rpx $line;
}
.qr-seal {
  position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%);
  width: 72rpx; height: 72rpx; background: $brand; color: #fff; font-family: serif;
  display: flex; align-items: center; justify-content: center; border-radius: 12rpx;
  font-size: 40rpx; border: 6rpx solid #fff;
}
.barcode {
  height: 90rpx; width: 100%; margin-top: 30rpx;
  background: repeating-linear-gradient(90deg, #241C19 0 4rpx, #fff 4rpx 10rpx, #241C19 10rpx 12rpx, #fff 12rpx 20rpx);
}
.code { margin-top: 18rpx; color: $muted; font-size: 26rpx; letter-spacing: 6rpx; }
.countdown { margin-top: 12rpx; color: $brand; font-size: 22rpx; }
.balance-bar {
  margin-top: 40rpx; width: 100%; background: rgba(255,255,255,0.14);
  border: 2rpx solid rgba(255,255,255,0.25); border-radius: 20rpx;
  padding: 24rpx 30rpx; display: flex; align-items: center; justify-content: space-between;
  color: #fff; font-size: 26rpx;
}
.balance-bar .b { font-size: 34rpx; font-weight: 600; }
.refresh {
  margin-top: 50rpx; color: #fff; font-size: 26rpx;
  border: 2rpx solid rgba(255,255,255,0.5); border-radius: 999rpx; padding: 18rpx 50rpx;
}
</style>
